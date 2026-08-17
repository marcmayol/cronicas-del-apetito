package com.marcm.cronicasapetito.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class FormatoCompartir { IMAGEN, PDF }

/**
 * Hoja de compartir. Hereda vista + periodo + filtro y lo dice dos veces —
 * arriba y al pie — porque quien recibe el documento tiene que saber qué le ha
 * llegado sin preguntar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompartirSheet(
    contexto: String,
    formatoSugerido: FormatoCompartir,
    onCerrar: () -> Unit,
    onCompartir: (FormatoCompartir) -> Unit,
) {
    var formato by remember { mutableStateOf(formatoSugerido) }

    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Compartir", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = contexto,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(14.dp))
            OpcionFormato(
                icono = Icons.Filled.Image,
                titulo = "Imagen",
                descripcion = "Se ve directamente en el chat. Recomendada para Semana y Mes.",
                elegida = formato == FormatoCompartir.IMAGEN,
                onClick = { formato = FormatoCompartir.IMAGEN },
            )
            Spacer(Modifier.height(9.dp))
            OpcionFormato(
                icono = Icons.Filled.Description,
                titulo = "PDF",
                descripcion = "Para imprimir o llevar a consulta. Incluye las fotos.",
                elegida = formato == FormatoCompartir.PDF,
                onClick = { formato = FormatoCompartir.PDF },
            )

            Spacer(Modifier.height(12.dp))
            Text(
                text = "Se comparte exactamente lo que estás viendo: $contexto.",
                style = MaterialTheme.typography.bodySmall,
                color = colorsCronicas.tenue,
            )

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCerrar) { Text("Cancelar") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onCompartir(formato) },
                    shape = RoundedCornerShape(999.dp),
                ) { Text("Compartir") }
            }
        }
    }
}

@Composable
private fun OpcionFormato(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    descripcion: String,
    elegida: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (elegida) 1.4.dp else 1.dp,
            if (elegida) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .background(
                        if (elegida) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icono,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                    tint = if (elegida) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (elegida) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
