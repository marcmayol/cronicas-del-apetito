package com.marcm.cronicasapetito.variante

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marcm.actualizador.Actualizador
import com.marcm.actualizador.ActualizadorConfig
import com.marcm.actualizador.EstadoActualizacion
import com.marcm.actualizador.Modo
import com.marcm.actualizador.TipoError
import com.marcm.cronicasapetito.BuildConfig
import com.marcm.cronicasapetito.R
import com.marcm.cronicasapetito.ui.Bloque
import com.marcm.cronicasapetito.ui.PuenteActualizador
import com.marcm.cronicasapetito.ui.Separador
import com.marcm.cronicasapetito.ui.TituloSeccion
import com.marcm.cronicasapetito.ui.colorsCronicas
import com.marcm.cronicasapetito.ui.visualDe
import com.marcm.cronicasapetito.data.EntryKind
import kotlinx.coroutines.launch

/**
 * Variante de fuera de Play (DracApps y marcmayol.com): la app se actualiza
 * sola. Todo lo que toca el módulo :actualizador vive aquí, para que la
 * variante de Play ni siquiera lo enlace.
 */
fun crearPuenteActualizador(app: Application): PuenteActualizador =
    ActualizadorPropio(
        Actualizador(
            app = app,
            config = ActualizadorConfig(
                manifiestoUrl = "https://marcmayol.com/cronicas-del-apetito/updates.json",
                versionCodeActual = BuildConfig.VERSION_CODE,
                checkHorasPorDefecto = 24,
            ),
        )
    )

private class ActualizadorPropio(private val actualizador: Actualizador) : PuenteActualizador {

    override val hayCanalPropio = true

    init {
        actualizador.programarPeriodica()
    }

    @Composable
    override fun Banner() {
        val estado by actualizador.estado.collectAsState()
        BannerActualizacion(
            estado = estado,
            onActualizar = { actualizador.actualizarAhora() },
        )
    }

    @Composable
    override fun SeccionAjustes() {
        val scope = rememberCoroutineScope()
        val estado by actualizador.estado.collectAsState()
        var buscarAuto by remember { mutableStateOf(actualizador.buscarAutomaticamente) }

        TituloSeccion(stringResource(R.string.settings_updates))
        Bloque {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_auto_check),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = stringResource(R.string.settings_auto_check_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = buscarAuto,
                    onCheckedChange = {
                        buscarAuto = it
                        actualizador.buscarAutomaticamente = it
                    },
                )
            }
            Separador()
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_check_now),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(2.dp))
                    LineaEstado(estado)
                }
                OutlinedButton(
                    onClick = { scope.launch { actualizador.comprobar(Modo.MANUAL) } },
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Text(stringResource(R.string.settings_check), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (estado is EstadoActualizacion.Disponible) {
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { actualizador.actualizarAhora() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) { Text(stringResource(R.string.settings_download_install)) }
        }
    }

    override suspend fun comprobarAlAbrir() {
        actualizador.comprobar(Modo.AUTOMATICO)
    }

    override fun alVolverAlFrente() {
        actualizador.onPermisoQuizaConcedido()
    }
}

/** El estado vive bajo «Buscar ahora»; el error en rojo tierra, nunca alarma. */
@Composable
private fun LineaEstado(estado: EstadoActualizacion) {
    when (estado) {
        EstadoActualizacion.Comprobando -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            TextoEstado(stringResource(R.string.update_checking))
        }

        EstadoActualizacion.AlDia -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = visualDe(EntryKind.WALK).color,
            )
            Spacer(Modifier.width(6.dp))
            TextoEstado(
                stringResource(
                    R.string.update_up_to_date,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE,
                ),
                color = visualDe(EntryKind.WALK).color,
            )
        }

        is EstadoActualizacion.Disponible ->
            TextoEstado(stringResource(R.string.update_available, estado.info.versionName))

        is EstadoActualizacion.Descargando ->
            TextoEstado(stringResource(R.string.update_downloading, estado.porcentaje))

        EstadoActualizacion.Verificando -> TextoEstado(stringResource(R.string.update_verifying))
        EstadoActualizacion.Instalando -> TextoEstado(stringResource(R.string.update_installing))
        is EstadoActualizacion.Error -> TextoEstado(
            mensajeError(estado),
            color = MaterialTheme.colorScheme.error,
        )

        else -> TextoEstado(
            stringResource(R.string.update_last_known, BuildConfig.VERSION_NAME),
            color = colorsCronicas.tenue,
        )
    }
}

@Composable
private fun TextoEstado(
    texto: String,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodySmall,
        color = if (color == androidx.compose.ui.graphics.Color.Unspecified)
            MaterialTheme.colorScheme.onSurfaceVariant else color,
    )
}

@Composable
private fun mensajeError(e: EstadoActualizacion.Error): String = when (e.tipo) {
    TipoError.SIN_RED -> stringResource(R.string.update_error_network)
    TipoError.HTTP -> stringResource(R.string.update_error_http)
    TipoError.MANIFIESTO -> stringResource(R.string.update_error_manifest)
    TipoError.DESCARGA -> stringResource(R.string.update_error_download)
    TipoError.HASH -> stringResource(R.string.update_error_hash)
    TipoError.INSTALACION ->
        stringResource(R.string.update_error_install) + (e.mensaje?.let { ": $it" } ?: ".")
}
