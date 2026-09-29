package com.salar.focuslock.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.salar.focuslock.ui.editor.RuleEditorScreen
import com.salar.focuslock.ui.editor.RuleEditorViewModel
import com.salar.focuslock.ui.rules.RuleListScreen

/** Top-level screen switch: rule list <-> rule editor. (Two screens, so no nav library needed.) */
@Composable
fun AppRoot(
    mainViewModel: MainViewModel,
    editorViewModel: RuleEditorViewModel,
    onOpenAccessibilitySettings: () -> Unit
) {
    val screen by mainViewModel.screen.collectAsState()

    when (val current = screen) {
        Screen.RuleList -> RuleListScreen(
            viewModel = mainViewModel,
            onOpenAccessibilitySettings = onOpenAccessibilitySettings
        )

        is Screen.RuleEditor -> {
            val close = {
                editorViewModel.clear()
                mainViewModel.closeEditor()
            }
            BackHandler(onBack = close)
            RuleEditorScreen(
                viewModel = editorViewModel,
                ruleId = current.ruleId,
                onClose = close
            )
        }
    }
}
