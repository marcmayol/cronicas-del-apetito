package com.marcm.cronicasapetito.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.marcm.cronicasapetito.CronicasApp
import com.marcm.cronicasapetito.R
import com.marcm.cronicasapetito.data.MealRepository
import com.marcm.cronicasapetito.notifications.MealNotifier
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EntryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MealNotifier.dismiss(this)

        val repo = MealRepository((application as CronicasApp).database.mealDao())

        setContent {
            CronicasTheme {
                EntryScreen(
                    onSave = { foodText, moodText, timestamp ->
                        lifecycleScope.launch {
                            repo.addFood(foodText, timestamp)
                            if (moodText.isNotBlank()) {
                                repo.addMood(moodText.trim(), timestamp)
                            }
                            finish()
                        }
                    },
                    onCancel = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryScreen(
    onSave: (foodText: String, moodText: String, timestamp: Long) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var foodText by remember { mutableStateOf("") }
    var moodText by remember { mutableStateOf("") }
    var selectedTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val scrollState = rememberScrollState()

    val dateTimeFormat = remember {
        SimpleDateFormat("EEEE d 'de' MMMM, HH:mm", Locale("es"))
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.entry_title)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Schedule,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Fecha y hora",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = dateTimeFormat.format(Date(selectedTime))
                            .replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                OutlinedButton(
                    onClick = {
                        pickDateTime(context, selectedTime) { chosen ->
                            selectedTime = chosen
                        }
                    }
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Cambiar")
                }
            }

            Text(
                text = "¿Qué has comido?",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = foodText,
                onValueChange = { foodText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.input_hint)) },
                minLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
            )

            Text(
                text = "¿Cómo te has sentido? (opcional)",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = moodText,
                onValueChange = { moodText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Puedes dejarlo en blanco si no quieres anotarlo") },
                minLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) { Text("Cancelar") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (foodText.isNotBlank()) onSave(foodText.trim(), moodText, selectedTime)
                    },
                    enabled = foodText.isNotBlank()
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

/**
 * Abre el selector de fecha y, al confirmar, el de hora. Devuelve en [onPicked] el
 * timestamp resultante (con segundos a 0). [initial] preselecciona ambos diálogos.
 */
private fun pickDateTime(context: Context, initial: Long, onPicked: (Long) -> Unit) {
    val cal = Calendar.getInstance().apply { timeInMillis = initial }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    val result = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onPicked(result.timeInMillis)
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).apply {
        // No permitir fechas futuras: una comida olvidada siempre es de hoy o antes.
        datePicker.maxDate = System.currentTimeMillis()
    }.show()
}
