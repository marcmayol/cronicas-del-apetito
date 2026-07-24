package com.marcm.cronicasapetito.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marcm.actualizador.EstadoActualizacion

/**
 * Banner no bloqueante en la parte alta de la pantalla principal. Solo se muestra
 * para los estados accionables o en progreso; los errores y el "estás al día" se
 * reservan para la pantalla de Ajustes (comprobación manual).
 */
@Composable
fun BannerActualizacion(
    estado: EstadoActualizacion,
    onActualizar: () -> Unit,
) {
    when (estado) {
        is EstadoActualizacion.Disponible -> Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(end = 10.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Nueva versión ${estado.info.versionName}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    if (estado.info.notas.isNotBlank()) {
                        Text(
                            text = estado.info.notas,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Button(onClick = onActualizar, modifier = Modifier.padding(start = 8.dp)) {
                    Text("Actualizar")
                }
            }
        }

        is EstadoActualizacion.Descargando -> BannerProgreso(
            texto = "Descargando actualización… ${estado.porcentaje}%",
            progreso = estado.porcentaje / 100f,
        )

        EstadoActualizacion.Verificando -> BannerProgreso("Verificando la descarga…", null)
        EstadoActualizacion.Instalando -> BannerProgreso("Instalando…", null)

        else -> Unit // Inactivo, Comprobando, AlDia, PidiendoPermiso, Error: sin banner
    }
}

@Composable
private fun BannerProgreso(texto: String, progreso: Float?) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = texto,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            if (progreso != null) {
                LinearProgressIndicator(
                    progress = { progreso },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
