package com.salar.focuslock.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.salar.focuslock.data.local.room.entity.BlockedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedAppDao {

    @Query("SELECT * FROM blocked_apps WHERE ruleId = :ruleId")
    fun observeForRule(ruleId: Long): Flow<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps WHERE ruleId = :ruleId")
    suspend fun getForRuleSync(ruleId: Long): List<BlockedAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: BlockedAppEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<BlockedAppEntity>)

    @Delete
    suspend fun delete(app: BlockedAppEntity)

    @Query("DELETE FROM blocked_apps WHERE ruleId = :ruleId")
    suspend fun deleteAllForRule(ruleId: Long)
}
