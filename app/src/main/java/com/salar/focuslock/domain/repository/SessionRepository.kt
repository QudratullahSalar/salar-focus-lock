package com.salar.focuslock.domain.repository

import com.salar.focuslock.domain.model.FocusSession
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface SessionRepository {
    fun observeSessions(): Flow<List<FocusSession>>
    suspend fun getActiveSession(): FocusSession?
    suspend fun getSessionsBetween(from: Instant, to: Instant): List<FocusSession>

    /** Returns the new session's row id. */
    suspend fun startSession(session: FocusSession): Long
    suspend fun updateSession(session: FocusSession)
}
