package com.sliide.app.features.users.data.repository

import com.sliide.app.core.common.DomainResult
import com.sliide.app.core.common.toDomainResult
import com.sliide.app.features.users.data.local.PendingDeleteDao
import com.sliide.app.features.users.data.local.PendingDeleteEntity
import com.sliide.app.features.users.data.local.UserDao
import com.sliide.app.features.users.data.mapper.UserMapper
import com.sliide.app.features.users.data.remote.UserRemoteDataSource
import com.sliide.app.features.users.domain.model.CreateUserRequest
import com.sliide.app.features.users.domain.model.User
import com.sliide.app.features.users.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class UserRepositoryImpl(
    private val remoteDataSource: UserRemoteDataSource,
    private val dao: UserDao,
    private val pendingDeleteDao: PendingDeleteDao,
    private val mapper: UserMapper,
) : UserRepository {

    private var totalPages: Int = 1
    private var currentPage: Int? = null
    private var nextSortOrder: Long = 0

    override fun observeUsers(): Flow<List<User>> =
        dao.observeAll().map { entities -> entities.map(mapper::entityToDomain) }

    override suspend fun refresh(): DomainResult<Boolean> {
        when (val firstResult = remoteDataSource.fetchUsers(page = 1)) {
            is DomainResult.Failure -> return firstResult
            is DomainResult.Success -> { totalPages = firstResult.data.second }
        }

        val allDtos = mutableListOf<com.sliide.app.features.users.data.remote.UserDto>()
        var page = totalPages
        while (page >= 1 && allDtos.size < 20) {
            when (val result = remoteDataSource.fetchUsers(page = page)) {
                is DomainResult.Failure -> return result
                is DomainResult.Success -> {
                    allDtos.addAll(result.data.first)
                    page--
                }
            }
        }

        currentPage = page + 1
        nextSortOrder = 0
        val pendingIds = pendingDeleteDao.getAll().map { it.userId }.toSet()
        val entities = allDtos
            .filter { it.id !in pendingIds }
            .map { dto -> mapper.dtoToEntity(dto).copy(sortOrder = nextSortOrder++) }
        dao.replaceAll(entities)
        val hasMore = (currentPage ?: 1) > 1
        return DomainResult.Success(hasMore)
    }

    override suspend fun loadNextPage(): Boolean {
        val nextPage = (currentPage ?: return false) - 1
        if (nextPage < 1) return false
        return when (val result = remoteDataSource.fetchUsers(page = nextPage)) {
            is DomainResult.Failure -> false
            is DomainResult.Success -> {
                currentPage = nextPage
                val pendingIds = pendingDeleteDao.getAll().map { it.userId }.toSet()
                val entities = result.data.first
                    .filter { it.id !in pendingIds }
                    .map { dto -> mapper.dtoToEntity(dto).copy(sortOrder = nextSortOrder++) }
                dao.upsert(entities)
                nextPage > 1
            }
        }
    }

    override suspend fun addUser(request: CreateUserRequest): DomainResult<User> {
        val body = mapper.createRequestToBody(request)
        return when (val result = remoteDataSource.createUser(body)) {
            is DomainResult.Failure -> result
            is DomainResult.Success -> {
                val entity = mapper.dtoToEntity(result.data).copy(sortOrder = -1)
                dao.insert(entity)
                DomainResult.Success(mapper.entityToDomain(entity))
            }
        }
    }

    override suspend fun deleteUser(userId: Long): DomainResult<Unit> = runCatching {
        dao.deleteById(userId)
        pendingDeleteDao.insert(PendingDeleteEntity(userId))
    }.toDomainResult()

    override suspend fun restoreUser(user: User): DomainResult<Unit> = runCatching {
        pendingDeleteDao.deleteByUserId(user.id)
        dao.insert(mapper.domainToEntity(user))
    }.toDomainResult()
}
