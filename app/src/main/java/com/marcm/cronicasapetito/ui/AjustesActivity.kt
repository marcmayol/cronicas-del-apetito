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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marcm.actualizador.Actualizador
import com.marcm.actualizador.EstadoActualizacion
import com.marcm.actualizador.Modo
import com.marcm.actualizador.TipoError
import com.marcm.cronicasapetito.BuildConfig
import com.marcm.cronicasapetito.CronicasApp
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
                title = { Text("Ajustes") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
            TituloSeccion("Actualizaciones")
            Bloque {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Buscar automáticamente",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "Comprueba si hay una versión nueva",
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
                            text = "Buscar ahora",
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
                    ) { Text("Buscar", fontWeight = FontWeight.SemiBold) }
                }
            }

            if (estado is EstadoActualizacion.Disponible) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { actualizador.actualizarAhora() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Descargar e instalar") }
            }

            TituloSeccion("Acerca de")
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
                            text = "Crónicas del Apetito",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "Versión ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) " +
                                "· datos solo en este móvil",
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
private fun TituloSeccion(texto: String) {
    Text(
        text = texto.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = colorsCronicas.tenue,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp, start = 2.dp),
    )
}

@Composable
private fun Bloque(contenido: @Composable () -> Unit) {
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
private fun Separador() {
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
            Texto("Comprobando…")
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
                "Estás al día · ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                color = visualDe(com.marcm.cronicasapetito.data.EntryKind.WALK).color,
            )
        }

        is EstadoActualizacion.Disponible -> Texto("Hay una versión nueva: ${estado.info.versionName}")
        is EstadoActualizacion.Descargando -> Texto("Descargando… ${estado.porcentaje}%")
        EstadoActualizacion.Verificando -> Texto("Verificando…")
        EstadoActualizacion.Instalando -> Texto("Instalando…")
        is EstadoActualizacion.Error -> Texto(
            mensajeError(estado),
            color = MaterialTheme.colorScheme.error,
        )

        else -> Texto(
            "Última versión conocida: ${BuildConfig.VERSION_NAME}",
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

private fun mensajeError(e: EstadoActualizacion.Error): String = when (e.tipo) {
    TipoError.SIN_RED -> "Sin conexión."
    TipoError.HTTP -> "No se pudo contactar con el servidor."
    TipoError.MANIFIESTO -> "La información de actualización no es válida."
    TipoError.DESCARGA -> "Falló la descarga."
    TipoError.HASH -> "La descarga estaba corrupta (se descartó)."
    TipoError.INSTALACION -> "No se pudo instalar" + (e.mensaje?.let { ": $it" } ?: ".")
}
