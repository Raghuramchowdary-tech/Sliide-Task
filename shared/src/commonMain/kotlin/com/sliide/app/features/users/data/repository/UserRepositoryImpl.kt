package com.sliide.app.features.users.data.repository

import com.sliide.app.core.common.DomainResult
import com.sliide.app.core.common.toDomainResult
import com.sliide.app.features.users.data.local.PendingDeleteDao
import com.sliide.app.features.users.data.local.PendingDeleteEntity
import com.sliide.app.features.users.data.local.UserDao
import com.sliide.app.features.users.data.local.UserEntity
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

    override fun observeUsers(): Flow<List<User>> =
        dao.observeAll().map { entities -> entities.map(mapper::entityToDomain) }

    override suspend fun refresh(): DomainResult<Unit> {
        // Fetch page 1 to get totalPages
        when (val firstResult = remoteDataSource.fetchUsers(page = 1)) {
            is DomainResult.Failure -> return firstResult
            is DomainResult.Success -> { totalPages = firstResult.data.second }
        }
        // Load backwards from last page until we have enough for a full screen
        val allEntities = mutableListOf<UserEntity>()
        var page = totalPages
        while (page >= 1 && allEntities.size < 20) {
            when (val result = remoteDataSource.fetchUsers(page = page)) {
                is DomainResult.Failure -> return result
                is DomainResult.Success -> {
                    allEntities.addAll(result.data.first.map(mapper::dtoToEntity))
                    page--
                }
            }
        }
        currentPage = page + 1
        val pendingIds = pendingDeleteDao.getAll().map { it.userId }.toSet()
        val filtered = allEntities.filter { it.id !in pendingIds }
        dao.upsert(filtered)
        return DomainResult.Success(Unit)
    }

    override suspend fun loadNextPage(): Boolean {
        val nextPage = (currentPage ?: return false) - 1
        if (nextPage < 1) return false
        return when (val result = remoteDataSource.fetchUsers(page = nextPage)) {
            is DomainResult.Failure -> false
            is DomainResult.Success -> {
                currentPage = nextPage
                val pendingIds = pendingDeleteDao.getAll().map { it.userId }.toSet()
                val entities = result.data.first.map(mapper::dtoToEntity)
                    .filter { it.id !in pendingIds }
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
                val entity = mapper.dtoToEntity(result.data)
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
