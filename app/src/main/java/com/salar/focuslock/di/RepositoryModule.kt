package com.salar.focuslock.di

import com.salar.focuslock.data.appdiscovery.AppDiscoveryRepositoryImpl
import com.salar.focuslock.data.appdiscovery.AppIconProvider
import com.salar.focuslock.data.repository.RuleCache
import com.salar.focuslock.data.repository.RuleRepositoryImpl
import com.salar.focuslock.data.repository.SessionRepositoryImpl
import com.salar.focuslock.domain.repository.AppDiscoveryRepository
import com.salar.focuslock.domain.repository.RuleRepository
import com.salar.focuslock.domain.repository.RuleSnapshotProvider
import com.salar.focuslock.domain.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindRuleRepository(impl: RuleRepositoryImpl): RuleRepository

    @Binds
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    abstract fun bindAppDiscoveryRepository(impl: AppDiscoveryRepositoryImpl): AppDiscoveryRepository

    @Binds
    abstract fun bindAppIconProvider(impl: AppDiscoveryRepositoryImpl): AppIconProvider

    @Binds
    abstract fun bindRuleSnapshotProvider(impl: RuleCache): RuleSnapshotProvider
}
