package com.salar.focuslock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.salar.focuslock.ui.theme.SalarFocusLockTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Placeholder entry point only — establishes the Compose + Hilt wiring so the project
 * is a buildable, launchable app. The real Dashboard/Rules/Lock-screen UI is out of
 * scope for Batch 1 per the "no unnecessary UI" instruction and is added in a later batch.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SalarFocusLockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PlaceholderScreen()
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen() {
    Text(text = "Salar Focus Lock — foundation build (Batch 1)")
}
