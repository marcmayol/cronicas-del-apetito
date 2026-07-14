package com.marcm.cronicasapetito.ui

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.marcm.cronicasapetito.CronicasApp
import com.marcm.cronicasapetito.data.MealRepository
import com.marcm.cronicasapetito.notifications.MealNotifier
import kotlinx.coroutines.launch

private enum class Step { WALK_QUESTION, WALK_MINUTES, MOOD }

class WalkMoodActivity : ComponentActivity() {

    companion object {
        const val EXTRA_START_AT_MINUTES = "start_at_minutes"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MealNotifier.dismiss(this)

        val repo = MealRepository((application as CronicasApp).database.mealDao())
        val startAtMinutes = intent.getBooleanExtra(EXTRA_START_AT_MINUTES, false)

        setContent {
            CronicasTheme {
                WalkMoodFlow(
                    initialStep = if (startAtMinutes) Step.WALK_MINUTES else Step.WALK_QUESTION,
                    onSave = { walkMinutes, moodText ->
                        lifecycleScope.launch {
                            if (walkMinutes != null && walkMinutes > 0) {
                                repo.addWalk(walkMinutes)
                            }
                            if (moodText.isNotBlank()) {
                                repo.addMood(moodText.trim())
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
private fun WalkMoodFlow(
    initialStep: Step = Step.WALK_QUESTION,
    onSave: (walkMinutes: Int?, moodText: String) -> Unit,
    onCancel: () -> Unit
) {
    var step by remember { mutableStateOf(initialStep) }
    var walkMinutes by remember { mutableStateOf<Int?>(null) }
    var minutesText by remember { mutableStateOf("") }
    var moodText by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Seguimiento") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            when (step) {
                Step.WALK_QUESTION -> {
                    Text(
                        text = "¿Has ido a caminar?",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { step = Step.WALK_MINUTES },
                            modifier = Modifier.weight(1f)
                        ) { Text("Sí") }
                        OutlinedButton(
                            onClick = {
                                walkMinutes = null
                                onSave(null, "")
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("No") }
                    }
                }

                Step.WALK_MINUTES -> {
                    Text(
                        text = "¿Cuánto tiempo has caminado?",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { input ->
                            minutesText = input.filter { it.isDigit() }.take(4)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Minutos") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (initialStep == Step.WALK_QUESTION) {
                            TextButton(onClick = { step = Step.WALK_QUESTION }) { Text("Atrás") }
                        } else {
                            TextButton(onClick = onCancel) { Text("Cancelar") }
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                walkMinutes = minutesText.toIntOrNull()
                                step = Step.MOOD
                            },
                            enabled = (minutesText.toIntOrNull() ?: 0) > 0
                        ) { Text("Continuar") }
                    }
                }

                Step.MOOD -> {
                    Text(
                        text = "¿Cómo te has sentido?",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Si quieres, anota cómo te has sentido. Es opcional.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = moodText,
                        onValueChange = { moodText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Describe cómo te sientes ahora mismo") },
                        minLines = 4
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onCancel) { Text("Cancelar") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { onSave(walkMinutes, moodText) }
                        ) { Text(if (moodText.isBlank()) "Omitir" else "Guardar") }
                    }
                }
            }
        }
    }
}
