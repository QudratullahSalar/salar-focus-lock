@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.salar.focuslock.ui.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.salar.focuslock.domain.model.InstalledApp
import com.salar.focuslock.domain.rules.AppPickerFilter
import com.salar.focuslock.domain.rules.RuleDraftError
import com.salar.focuslock.ui.format.formatMinuteOfDay
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private enum class TimeField { START, END }

@Composable
fun RuleEditorScreen(
    viewModel: RuleEditorViewModel,
    ruleId: Long?,
    onClose: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(ruleId) { viewModel.start(ruleId) }
    LaunchedEffect(state.isSaved) { if (state.isSaved) onClose() }

    var pickingTimeFor by remember { mutableStateOf<TimeField?>(null) }

    val draft = state.draft
    val editable = !state.isLoading && !state.isLockedForEditing
    val visibleApps = remember(
        state.installedApps, state.searchQuery, state.showSystemApps, draft.selectedApps.keys
    ) {
        AppPickerFilter.filter(
            apps = state.installedApps,
            query = state.searchQuery,
            showSystemApps = state.showSystemApps,
            selectedPackages = draft.selectedApps.keys
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (ruleId == null) "New rule" else "Edit rule") },
                navigationIcon = { TextButton(onClick = onClose) { Text("Cancel") } },
                actions = { TextButton(onClick = { viewModel.save() }, enabled = editable) { Text("Save") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isLockedForEditing) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            "This rule is active right now, so it can't be changed until it ends.",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            state.validationError?.let { error ->
                item { Text(error.message(), color = MaterialTheme.colorScheme.error) }
            }
            if (state.saveFailed) {
                item { Text("Couldn't save. Please try again.", color = MaterialTheme.colorScheme.error) }
            }

            item {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { viewModel.setName(it) },
                    label = { Text("Rule name") },
                    singleLine = true,
                    enabled = editable,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { pickingTimeFor = TimeField.START },
                            enabled = editable,
                            modifier = Modifier.weight(1f)
                        ) { Text("Starts ${formatMinuteOfDay(draft.startMinuteOfDay)}") }
                        OutlinedButton(
                            onClick = { pickingTimeFor = TimeField.END },
                            enabled = editable,
                            modifier = Modifier.weight(1f)
                        ) { Text("Ends ${formatMinuteOfDay(draft.endMinuteOfDay)}") }
                    }
                    if (draft.isOvernight) {
                        Spacer(Modifier.height(6.dp))
                        Text("Overnight rule: ends the next day.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item {
                Column {
                    Text("Repeat on", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DayOfWeek.values().forEach { day ->
                            FilterChip(
                                selected = day in draft.repeatDays,
                                onClick = { viewModel.toggleDay(day) },
                                enabled = editable,
                                label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) }
                            )
                        }
                    }
                }
            }

            item {
                Column {
                    Text(
                        "Apps to lock (${draft.selectedApps.size} selected)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        label = { Text("Search apps") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Show system apps", modifier = Modifier.weight(1f))
                        Switch(
                            checked = state.showSystemApps,
                            onCheckedChange = { viewModel.setShowSystemApps(it) }
                        )
                    }
                }
            }

            when {
                state.isLoading -> item { Text("Loading apps…") }
                visibleApps.isEmpty() -> item { Text("No apps found.") }
                else -> items(visibleApps, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        checked = app.packageName in draft.selectedApps,
                        enabled = editable,
                        loadIcon = { packageName -> viewModel.loadIcon(packageName) },
                        onToggle = { viewModel.toggleApp(app) }
                    )
                }
            }
        }
    }

    pickingTimeFor?.let { field ->
        TimePickerDialog(
            initialMinuteOfDay = if (field == TimeField.START) draft.startMinuteOfDay else draft.endMinuteOfDay,
            onConfirm = { minuteOfDay ->
                if (field == TimeField.START) viewModel.setStart(minuteOfDay) else viewModel.setEnd(minuteOfDay)
                pickingTimeFor = null
            },
            onDismiss = { pickingTimeFor = null }
        )
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    checked: Boolean,
    enabled: Boolean,
    loadIcon: suspend (String) -> ImageBitmap?,
    onToggle: () -> Unit
) {
    val icon by produceState<ImageBitmap?>(initialValue = null, key1 = app.packageName) {
        value = loadIcon(app.packageName)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onToggle() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bitmap = icon
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(40.dp))
        } else {
            Spacer(Modifier.size(40.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(app.appName, style = MaterialTheme.typography.bodyLarge)
            Text(app.packageName, style = MaterialTheme.typography.bodySmall)
        }
        Checkbox(checked = checked, onCheckedChange = { onToggle() }, enabled = enabled)
    }
}

@Composable
private fun TimePickerDialog(
    initialMinuteOfDay: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val pickerState = rememberTimePickerState(
        initialHour = initialMinuteOfDay / 60,
        initialMinute = initialMinuteOfDay % 60,
        is24Hour = false
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(pickerState.hour * 60 + pickerState.minute) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(state = pickerState) }
    )
}

private fun RuleDraftError.message(): String = when (this) {
    RuleDraftError.NAME_EMPTY -> "Give the rule a name."
    RuleDraftError.NAME_TOO_LONG -> "The name is too long (max 40 characters)."
    RuleDraftError.INVALID_TIME -> "Choose a valid start and end time."
    RuleDraftError.START_EQUALS_END -> "Start and end time can't be the same."
    RuleDraftError.NO_DAYS -> "Choose at least one day."
    RuleDraftError.NO_APPS -> "Choose at least one app to lock."
}
