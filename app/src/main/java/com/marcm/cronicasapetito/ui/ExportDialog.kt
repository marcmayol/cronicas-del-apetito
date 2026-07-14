package com.marcm.cronicasapetito.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.marcm.cronicasapetito.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onExport: (from: Long?, to: Long?) -> Unit
) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(ExportMode.ALL) }
    var fromMillis by remember { mutableStateOf<Long?>(null) }
    var toMillis by remember { mutableStateOf<Long?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.export_pdf)) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = mode == ExportMode.ALL,
                        onClick = { mode = ExportMode.ALL }
                    )
                    Text(stringResource(R.string.export_all))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = mode == ExportMode.RANGE,
                        onClick = { mode = ExportMode.RANGE }
                    )
                    Text(stringResource(R.string.export_range))
                }
                if (mode == ExportMode.RANGE) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                pickDate(context) { millis ->
                                    fromMillis = startOfDay(millis)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                fromMillis?.let { dateFormat.format(Date(it)) }
                                    ?: stringResource(R.string.from_date)
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                pickDate(context) { millis ->
                                    toMillis = endOfDay(millis)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                toMillis?.let { dateFormat.format(Date(it)) }
                                    ?: stringResource(R.string.to_date)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (mode) {
                        ExportMode.ALL -> onExport(null, null)
                        ExportMode.RANGE -> {
                            val f = fromMillis
                            val t = toMillis
                            if (f != null && t != null && f <= t) onExport(f, t)
                        }
                    }
                }
            ) { Text(stringResource(R.string.export_pdf)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private enum class ExportMode { ALL, RANGE }

private fun pickDate(context: android.content.Context, onPicked: (Long) -> Unit) {
    val cal = Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, day ->
            val c = Calendar.getInstance().apply {
                set(year, month, day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onPicked(c.timeInMillis)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun startOfDay(millis: Long): Long {
    val c = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return c.timeInMillis
}

private fun endOfDay(millis: Long): Long {
    val c = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
    }
    return c.timeInMillis
}
