package com.salar.focuslock.di

import android.content.Context
import androidx.room.Room
import com.salar.focuslock.data.local.room.FocusLockDatabase
import com.salar.focuslock.data.local.room.dao.BlockedAppDao
import com.salar.focuslock.data.local.room.dao.FocusRuleDao
import com.salar.focuslock.data.local.room.dao.FocusSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideFocusLockDatabase(@ApplicationContext context: Context): FocusLockDatabase =
        Room.databaseBuilder(context, FocusLockDatabase::class.java, FocusLockDatabase.DATABASE_NAME)
            .build()

    @Provides
    fun provideFocusRuleDao(database: FocusLockDatabase): FocusRuleDao = database.focusRuleDao()

    @Provides
    fun provideBlockedAppDao(database: FocusLockDatabase): BlockedAppDao = database.blockedAppDao()

    @Provides
    fun provideFocusSessionDao(database: FocusLockDatabase): FocusSessionDao = database.focusSessionDao()
}
