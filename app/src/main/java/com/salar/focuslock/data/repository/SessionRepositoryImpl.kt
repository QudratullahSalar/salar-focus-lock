package com.salar.focuslock.data.repository

import com.salar.focuslock.data.local.room.dao.FocusSessionDao
import com.salar.focuslock.data.mapper.toDomain
import com.salar.focuslock.data.mapper.toEntity
import com.salar.focuslock.domain.model.FocusSession
import com.salar.focuslock.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val focusSessionDao: FocusSessionDao
) : SessionRepository {

    override fun observeSessions(): Flow<List<FocusSession>> =
        focusSessionDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getActiveSession(): FocusSession? =
        focusSessionDao.getActiveSession()?.toDomain()

    override suspend fun getSessionsBetween(from: Instant, to: Instant): List<FocusSession> =
        focusSessionDao.getSessionsBetween(from.toEpochMilli(), to.toEpochMilli()).map { it.toDomain() }

    override suspend fun startSession(session: FocusSession): Long =
        focusSessionDao.insert(session.toEntity())

    override suspend fun updateSession(session: FocusSession) {
        focusSessionDao.update(session.toEntity())
    }
}
