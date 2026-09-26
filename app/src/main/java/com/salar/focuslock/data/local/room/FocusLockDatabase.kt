package com.salar.focuslock.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.salar.focuslock.data.local.room.dao.BlockedAppDao
import com.salar.focuslock.data.local.room.dao.FocusRuleDao
import com.salar.focuslock.data.local.room.dao.FocusSessionDao
import com.salar.focuslock.data.local.room.entity.BlockedAppEntity
import com.salar.focuslock.data.local.room.entity.FocusRuleEntity
import com.salar.focuslock.data.local.room.entity.FocusSessionEntity

@Database(
    entities = [FocusRuleEntity::class, BlockedAppEntity::class, FocusSessionEntity::class],
    version = 1,
    // exportSchema left false until a real build environment wires up the KSP
    // room.schemaLocation argument (Batch 1 has no build environment to verify that with).
    exportSchema = false
)
abstract class FocusLockDatabase : RoomDatabase() {

    abstract fun focusRuleDao(): FocusRuleDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        const val DATABASE_NAME = "salar_focus_lock.db"
    }
}
