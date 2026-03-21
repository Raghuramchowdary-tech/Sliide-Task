package com.sliide.app.features.users.data.repository

import com.sliide.app.core.common.DomainError
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

    override fun observeUsers(): Flow<List<User>> =
        dao.observeAll().map { entities -> entities.map(mapper::entityToDomain) }

    override suspend fun refreshFromLastPage(): DomainResult<Unit> {
        syncPendingDeletes()
        return when (val result = remoteDataSource.fetchLastPageUsers()) {
            is DomainResult.Failure -> result
            is DomainResult.Success -> {
                val pendingIds = pendingDeleteDao.getAll().map { it.userId }.toSet()
                val entities = result.data.map(mapper::dtoToEntity)
                    .filter { it.id !in pendingIds }
                dao.upsert(entities)
                DomainResult.Success(Unit)
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

    private suspend fun syncPendingDeletes() {
        val pending = pendingDeleteDao.getAll()
        for (entry in pending) {
            val result = remoteDataSource.deleteUser(entry.userId)
            val succeeded = result is DomainResult.Success
            val alreadyGone = result is DomainResult.Failure &&
                result.error == DomainError.NotFound
            if (succeeded || alreadyGone) {
                pendingDeleteDao.deleteByUserId(entry.userId)
            }
        }
    }
}
