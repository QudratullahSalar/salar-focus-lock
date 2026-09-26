package com.salar.focuslock

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. Hilt-annotated so the DI graph (Room database, DataStore,
 * repositories — wired in di/DatabaseModule.kt and di/RepositoryModule.kt) is available
 * from app startup. Deliberately does no blocking-engine or service work in this batch.
 */
@HiltAndroidApp
class FocusLockApp : Application()
