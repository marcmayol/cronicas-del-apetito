package com.marcm.cronicasapetito.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.marcm.actualizador.EstadoActualizacion
import com.marcm.actualizador.Modo
import com.marcm.actualizador.TipoError
import com.marcm.cronicasapetito.BuildConfig
import com.marcm.cronicasapetito.CronicasApp
import com.marcm.cronicasapetito.R
import kotlinx.coroutines.launch

/** Ajustes y «Acerca de»: versión, autobúsqueda y comprobación manual. */
class AjustesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actualizador = (application as CronicasApp).actualizador
        setContent {
            CronicasTheme {
                AjustesScreen(actualizador = actualizador, onBack = { finish() })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as CronicasApp).actualizador.onPermisoQuizaConcedido()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AjustesScreen(actualizador: Actualizador, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val estado by actualizador.estado.collectAsState()
    var buscarAuto by remember { mutableStateOf(actualizador.buscarAutomaticamente) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // Lo primero, porque es lo que cada persona necesita ajustar a su vida.
            SeccionRecordatorios()

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
                    ) { Text(stringResource(R.string.settings_check), fontWeight = FontWeight.SemiBold) }
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

            TituloSeccion(stringResource(R.string.settings_about))
            Bloque {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "C",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primaryContainer,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(
                                R.string.settings_version,
                                BuildConfig.VERSION_NAME,
                                BuildConfig.VERSION_CODE,
                                stringResource(R.string.settings_local_only),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
internal fun TituloSeccion(texto: String) {
    Text(
        text = texto.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = colorsCronicas.tenue,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp, start = 2.dp),
    )
}

@Composable
internal fun Bloque(contenido: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column { contenido() }
    }
}

@Composable
internal fun Separador() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

/** El estado vive bajo «Buscar ahora»; el error en rojo tierra, nunca alarma. */
@Composable
private fun LineaEstado(estado: EstadoActualizacion) {
    when (estado) {
        EstadoActualizacion.Comprobando -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Texto(stringResource(R.string.update_checking))
        }

        EstadoActualizacion.AlDia -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = visualDe(com.marcm.cronicasapetito.data.EntryKind.WALK).color,
            )
            Spacer(Modifier.width(6.dp))
            Texto(
                stringResource(
                    R.string.update_up_to_date,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE,
                ),
                color = visualDe(com.marcm.cronicasapetito.data.EntryKind.WALK).color,
            )
        }

        is EstadoActualizacion.Disponible ->
            Texto(stringResource(R.string.update_available, estado.info.versionName))

        is EstadoActualizacion.Descargando ->
            Texto(stringResource(R.string.update_downloading, estado.porcentaje))
        EstadoActualizacion.Verificando -> Texto(stringResource(R.string.update_verifying))
        EstadoActualizacion.Instalando -> Texto(stringResource(R.string.update_installing))
        is EstadoActualizacion.Error -> Texto(
            mensajeError(estado),
            color = MaterialTheme.colorScheme.error,
        )

        else -> Texto(
            stringResource(R.string.update_last_known, BuildConfig.VERSION_NAME),
            color = colorsCronicas.tenue,
        )
    }
}

@Composable
private fun Texto(texto: String, color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified) {
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
