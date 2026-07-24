package com.marcm.cronicasapetito.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marcm.actualizador.Actualizador
import com.marcm.actualizador.EstadoActualizacion
import com.marcm.actualizador.Modo
import com.marcm.actualizador.TipoError
import com.marcm.cronicasapetito.BuildConfig
import com.marcm.cronicasapetito.CronicasApp
import kotlinx.coroutines.launch

/** Ajustes y "Acerca de": versión, toggle de autobúsqueda y comprobación manual. */
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
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
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
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Actualizaciones", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Buscar actualizaciones", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Comprueba automáticamente si hay una versión nueva.",
                        style = MaterialTheme.typography.bodySmall,
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = { scope.launch { actualizador.comprobar(Modo.MANUAL) } }) {
                    Text("Buscar actualizaciones")
                }
                EstadoManual(estado)
            }

            if (estado is EstadoActualizacion.Disponible) {
                Button(onClick = { actualizador.actualizarAhora() }) {
                    Text("Descargar e instalar")
                }
            }

            Divider()

            Text("Acerca de", style = MaterialTheme.typography.titleMedium)
            Text(
                "Crónicas del Apetito",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "Versión ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun EstadoManual(estado: EstadoActualizacion) {
    when (estado) {
        EstadoActualizacion.Comprobando ->
            CircularProgressIndicator(modifier = Modifier.size(20.dp))

        EstadoActualizacion.AlDia ->
            Text("Estás al día ✓", style = MaterialTheme.typography.bodyMedium)

        is EstadoActualizacion.Disponible ->
            Text(
                "Hay una versión nueva: ${estado.info.versionName}",
                style = MaterialTheme.typography.bodyMedium,
            )

        is EstadoActualizacion.Descargando ->
            Text("Descargando… ${estado.porcentaje}%", style = MaterialTheme.typography.bodyMedium)

        EstadoActualizacion.Verificando ->
            Text("Verificando…", style = MaterialTheme.typography.bodyMedium)

        EstadoActualizacion.Instalando ->
            Text("Instalando…", style = MaterialTheme.typography.bodyMedium)

        is EstadoActualizacion.Error ->
            Text(mensajeError(estado), color = MaterialTheme.colorScheme.error)

        else -> Unit
    }
}

private fun mensajeError(e: EstadoActualizacion.Error): String = when (e.tipo) {
    TipoError.SIN_RED -> "Sin conexión."
    TipoError.HTTP -> "No se pudo contactar con el servidor."
    TipoError.MANIFIESTO -> "La información de actualización no es válida."
    TipoError.DESCARGA -> "Falló la descarga."
    TipoError.HASH -> "La descarga estaba corrupta (se descartó)."
    TipoError.INSTALACION -> "No se pudo instalar" + (e.mensaje?.let { ": $it" } ?: ".")
}
