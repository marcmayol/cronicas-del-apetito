package com.marcm.cronicasapetito.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddEntryPickerDialog(
    onDismiss: () -> Unit,
    onPickFood: () -> Unit,
    onPickWalk: () -> Unit,
    onPickGym: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Qué quieres anotar?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPickFood,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Restaurant, contentDescription = null)
                    Text(
                        text = "Comida",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                OutlinedButton(
                    onClick = onPickWalk,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.DirectionsWalk, contentDescription = null)
                    Text(
                        text = "Caminata",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                OutlinedButton(
                    onClick = onPickGym,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.FitnessCenter, contentDescription = null)
                    Text(
                        text = "Gimnasio",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun GymQuestionDialog(
    onDismiss: () -> Unit,
    onAnswer: (went: Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Has ido al gimnasio hoy?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onAnswer(true) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.FitnessCenter, contentDescription = null)
                    Text(text = "Sí", modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedButton(
                    onClick = { onAnswer(false) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "No")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
