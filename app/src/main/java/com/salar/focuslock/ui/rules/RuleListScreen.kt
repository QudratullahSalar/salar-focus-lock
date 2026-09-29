@file:OptIn(ExperimentalMaterial3Api::class)

package com.salar.focuslock.ui.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.salar.focuslock.domain.model.FocusRule
import com.salar.focuslock.ui.format.formatMinuteOfDay
import com.salar.focuslock.ui.format.formatTimeRange
import com.salar.focuslock.ui.format.summarizeApps
import com.salar.focuslock.ui.format.summarizeDays
import com.salar.focuslock.ui.main.MainViewModel
import com.salar.focuslock.ui.main.RuleListItem

@Composable
fun RuleListScreen(
    viewModel: MainViewModel,
    onOpenAccessibilitySettings: () -> Unit
) {
    val rules by viewModel.rules.collectAsState()
    val accessibilityEnabled by viewModel.accessibilityEnabled.collectAsState()
    var ruleToDelete by remember { mutableStateOf<FocusRule?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Salar Focus Lock") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { viewModel.openEditor(null) }) {
                Text("Add rule")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!accessibilityEnabled) {
                item { AccessibilityBanner(onOpenAccessibilitySettings) }
            }

            val current = rules
            when {
                current == null -> item { Text("Loading…") }
                current.isEmpty() -> item {
                    Text("No focus rules yet. Tap “Add rule” to lock apps during a schedule.")
                }
                else -> items(current, key = { it.rule.id }) { entry ->
                    RuleCard(
                        entry = entry,
                        onToggle = { enabled -> viewModel.setRuleEnabled(entry.rule.id, enabled) },
                        onEdit = { viewModel.openEditor(entry.rule.id) },
                        onDelete = { ruleToDelete = entry.rule }
                    )
                }
            }
        }
    }

    ruleToDelete?.let { rule ->
        AlertDialog(
            onDismissRequest = { ruleToDelete = null },
            title = { Text("Delete rule?") },
            text = { Text("“${rule.name}” will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRule(rule.id)
                    ruleToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { ruleToDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AccessibilityBanner(onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Blocking is OFF",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Turn on “Salar Focus Lock” in Accessibility settings so the app can detect which " +
                    "app is open. It only reads the app's name — never your screen, messages or passwords.",
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "If the switch is greyed out: Settings → Apps → Salar Focus Lock → ⋮ menu → " +
                    "Allow restricted settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onOpenSettings) { Text("Open Accessibility settings") }
        }
    }
}

@Composable
private fun RuleCard(
    entry: RuleListItem,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val rule = entry.rule
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = rule.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggle,
                    enabled = !entry.isLockedNow
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(formatTimeRange(rule.startMinuteOfDay, rule.endMinuteOfDay))
            Text(summarizeDays(rule.repeatDays), style = MaterialTheme.typography.bodyMedium)
            Text("Apps: ${summarizeApps(entry.appLabels)}", style = MaterialTheme.typography.bodyMedium)

            if (entry.isLockedNow) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Locked now — can't be changed until ${formatMinuteOfDay(rule.endMinuteOfDay)}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            } else if (!rule.isEnabled) {
                Spacer(Modifier.height(6.dp))
                Text("Off", style = MaterialTheme.typography.bodySmall)
            }

            Row {
                TextButton(onClick = onEdit, enabled = !entry.isLockedNow) { Text("Edit") }
                TextButton(onClick = onDelete, enabled = !entry.isLockedNow) { Text("Delete") }
            }
        }
    }
}
