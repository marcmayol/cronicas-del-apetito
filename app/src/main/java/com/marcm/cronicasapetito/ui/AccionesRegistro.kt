package com.marcm.cronicasapetito.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marcm.cronicasapetito.R
import com.marcm.cronicasapetito.data.MealEntry

/**
 * Qué se puede hacer con un registro ya anotado: corregirlo o quitarlo.
 *
 * Hasta la v2.2.2 no se podía ninguna de las dos cosas. Con un aviso cada hora
 * uno anota deprisa y se equivoca —la comida de ayer puesta en hoy, un dedazo,
 * el mismo plato dos veces— y eso acababa impreso en la consulta sin remedio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccionesRegistroSheet(
    entry: MealEntry,
    onCerrar: () -> Unit,
    onEditar: () -> Unit,
    onBorrar: () -> Unit,
) {
    val context = LocalContext.current
    val visual = visualDe(entry.kind)

    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            // Encabezado: que se vea cuál es el registro sobre el que se actúa,
            // porque borrar el de al lado es justo lo que no puede pasar.
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(visual.contenedor, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        iconoDe(entry.kind),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = visual.color,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = Fechas.hora(entry.timestampMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = visual.color,
                    )
                    Text(
                        text = contenidoLegible(context, entry),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            FilaAccionRegistro(
                icono = Icons.Filled.Edit,
                texto = stringResource(R.string.entry_edit),
                onClick = onEditar,
            )
            FilaAccionRegistro(
                icono = Icons.Filled.DeleteOutline,
                texto = stringResource(R.string.entry_delete),
                // Rojo tierra, el mismo del sistema: avisa sin gritar.
                tinte = MaterialTheme.colorScheme.error,
                onClick = onBorrar,
            )
        }
    }
}

@Composable
private fun FilaAccionRegistro(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    texto: String,
    onClick: () -> Unit,
    tinte: Color? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(
            icono,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = tinte ?: MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyLarge,
            color = tinte ?: MaterialTheme.colorScheme.onSurface,
        )
    }
}
