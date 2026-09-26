package com.salar.focuslock.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.salar.focuslock.data.local.room.entity.FocusRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusRuleDao {

    @Query("SELECT * FROM focus_rules ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FocusRuleEntity>>

    @Query("SELECT * FROM focus_rules WHERE id = :id")
    suspend fun getById(id: Long): FocusRuleEntity?

    @Query("SELECT * FROM focus_rules WHERE isEnabled = 1")
    suspend fun getEnabledRules(): List<FocusRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: FocusRuleEntity): Long

    @Update
    suspend fun update(rule: FocusRuleEntity)

    @Delete
    suspend fun delete(rule: FocusRuleEntity)
}
