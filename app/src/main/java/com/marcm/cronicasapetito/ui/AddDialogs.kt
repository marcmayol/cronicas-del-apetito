package com.marcm.cronicasapetito.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marcm.cronicasapetito.data.EntryKind

/**
 * P2 — «¿Qué quieres anotar?». Misma estructura que siempre (comida primero y
 * sólida) para no alargar el camino rápido; lo nuevo es que cada opción lleva el
 * icono en el color de su tipo.
 */
@Composable
fun AddEntryPickerDialog(
    onDismiss: () -> Unit,
    onPickFood: () -> Unit,
    onPickWalk: () -> Unit,
    onPickGym: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Qué quieres anotar?") },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.background,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                BotonTipoPrincipal(
                    kind = EntryKind.FOOD,
                    texto = "Comida",
                    onClick = onPickFood,
                )
                BotonTipoSecundario(
                    kind = EntryKind.WALK,
                    texto = "Caminata",
                    onClick = onPickWalk,
                )
                BotonTipoSecundario(
                    kind = EntryKind.GYM,
                    texto = "Gimnasio",
                    onClick = onPickGym,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

/** P5 — el gimnasio se responde y se guarda, sin pasos intermedios. */
@Composable
fun GymQuestionDialog(
    onDismiss: () -> Unit,
    onAnswer: (went: Boolean) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Has ido al gimnasio hoy?") },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.background,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                BotonTipoPrincipal(
                    kind = EntryKind.GYM,
                    texto = "Sí",
                    onClick = { onAnswer(true) },
                )
                OutlinedButton(
                    onClick = { onAnswer(false) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.4.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Text(
                        text = "No",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun BotonTipoPrincipal(kind: String, texto: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        FilaOpcion(icono = iconoDe(kind), texto = texto)
    }
}

@Composable
private fun BotonTipoSecundario(kind: String, texto: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.4.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        FilaOpcion(icono = iconoDe(kind), texto = texto, tinte = visualDe(kind).color)
    }
}

@Composable
private fun FilaOpcion(
    icono: ImageVector,
    texto: String,
    tinte: androidx.compose.ui.graphics.Color? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icono,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = tinte ?: androidx.compose.ui.graphics.Color.Unspecified,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
    }
}
