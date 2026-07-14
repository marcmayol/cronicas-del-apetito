package com.marcm.cronicasapetito.ui

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marcm.cronicasapetito.R
import com.marcm.cronicasapetito.data.EntryKind
import com.marcm.cronicasapetito.data.MealEntry
import com.marcm.cronicasapetito.data.MealRepository
import com.marcm.cronicasapetito.export.PdfExporter
import com.marcm.cronicasapetito.export.shareFile
import com.marcm.cronicasapetito.notifications.MealAlarmScheduler
import com.marcm.cronicasapetito.notifications.SleepPrefs
import android.widget.Toast
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.graphics.Color

private fun kindLabel(kind: String): String = when (kind) {
    EntryKind.FOOD -> "Comida"
    EntryKind.WALK -> "Caminata"
    EntryKind.MOOD -> "Estado de ánimo"
    EntryKind.GYM -> "Gimnasio"
    else -> kind
}

private fun kindColor(kind: String): Color = when (kind) {
    EntryKind.FOOD -> Color(0xFF7A4E2D)
    EntryKind.WALK -> Color(0xFF2E7D32)
    EntryKind.MOOD -> Color(0xFF6A1B9A)
    EntryKind.GYM -> Color(0xFF1565C0)
    else -> Color.DarkGray
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: MealRepository,
    onRequestExactAlarmPermission: () -> Unit
) {
    val context = LocalContext.current
    val entries by repository.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showExportDialog by remember { mutableStateOf(false) }
    var showAddPicker by remember { mutableStateOf(false) }
    var showGymDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var sleeping by remember { mutableStateOf(SleepPrefs.isSleeping(context)) }

    // Aviso si Android 12+ no concedió alarmas exactas
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) onRequestExactAlarmPermission()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showSleepDialog = true }) {
                        Icon(
                            Icons.Filled.Bedtime,
                            contentDescription = stringResource(R.string.sleep_button)
                        )
                    }
                    OutlinedButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                        Text(
                            text = " " + stringResource(R.string.export_pdf),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddPicker = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Anotar") }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (sleeping) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Bedtime,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = stringResource(R.string.sleep_banner),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                MealAlarmScheduler.wakeUp(context)
                                sleeping = false
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.wake_toast),
                                    Toast.LENGTH_LONG
                                ).show()
                            }) {
                                Text(stringResource(R.string.wake_button))
                            }
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    if (entries.isEmpty()) {
                        Text(
                            text = stringResource(R.string.empty_history),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        HistoryList(entries)
                    }
                }
            }
        }
    }

    if (showSleepDialog) {
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            icon = { Icon(Icons.Filled.Bedtime, contentDescription = null) },
            title = { Text(stringResource(R.string.sleep_dialog_title)) },
            text = { Text(stringResource(R.string.sleep_dialog_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showSleepDialog = false
                    MealAlarmScheduler.goToSleep(context)
                    sleeping = true
                    Toast.makeText(
                        context,
                        context.getString(R.string.sleep_toast),
                        Toast.LENGTH_LONG
                    ).show()
                }) {
                    Text(stringResource(R.string.sleep_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSleepDialog = false }) {
                    Text(stringResource(R.string.sleep_cancel))
                }
            }
        )
    }

    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExport = { from, to ->
                showExportDialog = false
                scope.launch {
                    val list = if (from == null || to == null) repository.getAll()
                    else repository.getInRange(from, to)
                    val file = PdfExporter.export(context, list, from, to)
                    shareFile(context, file)
                }
            }
        )
    }

    if (showAddPicker) {
        AddEntryPickerDialog(
            onDismiss = { showAddPicker = false },
            onPickFood = {
                showAddPicker = false
                context.startActivity(Intent(context, EntryActivity::class.java))
            },
            onPickWalk = {
                showAddPicker = false
                val walkIntent = Intent(context, WalkMoodActivity::class.java).apply {
                    putExtra(WalkMoodActivity.EXTRA_START_AT_MINUTES, true)
                }
                context.startActivity(walkIntent)
            },
            onPickGym = {
                showAddPicker = false
                showGymDialog = true
            }
        )
    }

    if (showGymDialog) {
        GymQuestionDialog(
            onDismiss = { showGymDialog = false },
            onAnswer = { went ->
                showGymDialog = false
                scope.launch { repository.addGym(went) }
            }
        )
    }
}

@Composable
private fun HistoryList(entries: List<MealEntry>) {
    val grouped = remember(entries) {
        val dayFormat = SimpleDateFormat("EEEE d 'de' MMMM yyyy", Locale("es"))
        entries.groupBy { dayFormat.format(Date(it.timestampMillis)) }
            .toList() // mantiene orden de inserción (DESC por timestamp)
    }
    val hourFormat = remember { SimpleDateFormat("HH:mm", Locale("es")) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grouped.forEach { (day, dayEntries) ->
            item(key = "header-$day") {
                Text(
                    text = day.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(dayEntries, key = { it.id }) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = hourFormat.format(Date(entry.timestampMillis)),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = kindLabel(entry.kind),
                                style = MaterialTheme.typography.labelMedium,
                                color = kindColor(entry.kind),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = entry.content,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
