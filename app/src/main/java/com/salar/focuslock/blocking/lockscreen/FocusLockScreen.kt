package com.salar.focuslock.blocking.lockscreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.salar.focuslock.domain.model.FocusLockScreenState
import com.salar.focuslock.ui.format.toClockString
import com.salar.focuslock.ui.format.toUnlockTimeString

/**
 * Stateless — pure rendering of [state]. No navigation, no data fetching, no unlock button by
 * design (requirement #1). All orchestration/recomputation lives in FocusLockRoute.
 */
@Composable
fun FocusLockScreen(state: FocusLockScreenState, appIcon: ImageBitmap?) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "\uD83D\uDD12", style = MaterialTheme.typography.displayMedium) // 🔒
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Focus Mode Active",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = "${state.appName} is locked.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = state.ruleName,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            Text(text = "Unlocks at", style = MaterialTheme.typography.labelMedium)
            Text(text = state.unlockAt.toUnlockTimeString(), style = MaterialTheme.typography.headlineSmall)

            Spacer(Modifier.height(20.dp))

            Text(text = "Remaining", style = MaterialTheme.typography.labelMedium)
            Text(text = state.remainingDuration.toClockString(), style = MaterialTheme.typography.headlineMedium)

            Spacer(Modifier.height(36.dp))

            Text(
                text = "Taking a break helps you stay focused on what matters. " +
                    "This app unlocks automatically when your focus period ends.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}
