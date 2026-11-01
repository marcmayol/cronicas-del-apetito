package com.marcm.cronicasapetito.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.marcm.cronicasapetito.R
import com.marcm.cronicasapetito.data.CopiaAutomatica
import com.marcm.cronicasapetito.data.MealRepository
import com.marcm.cronicasapetito.data.RespaldoDatos
import com.marcm.cronicasapetito.data.abrirParaEscribir
import com.marcm.cronicasapetito.data.abrirParaLeer
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sección «Tus datos» de Ajustes: sacar una copia y volver a meterla.
 *
 * Aquí no hay nube ni cuenta, que es justo lo que la hace privada y también lo
 * que la deja a merced de un móvil perdido. Esto es la salida: un ZIP que te
 * llevas donde quieras.
 */
@Composable
fun SeccionDatos(repositorio: MealRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmarImportacion by remember { mutableStateOf<android.net.Uri?>(null) }

    val exportar = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { destino ->
        if (destino == null) return@rememberLauncherForActivityResult
        scope.launch {
            val salida = context.abrirParaEscribir(destino)
            if (salida == null) {
                avisar(context, context.getString(R.string.backup_export_failed))
                return@launch
            }
            val resultado = salida.use {
                RespaldoDatos.exportar(repositorio.getAll(), it)
            }
            avisar(
                context,
                context.getString(
                    R.string.backup_export_done, resultado.registros, resultado.fotos
                )
            )
        }
    }

    val elegirRespaldo = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { origen -> if (origen != null) confirmarImportacion = origen }

    TituloSeccion(stringResource(R.string.backup_section))
    Bloque {
        FilaAccion(
            titulo = stringResource(R.string.backup_export),
            detalle = stringResource(R.string.backup_export_desc),
            boton = stringResource(R.string.backup_export_action),
            onClick = { exportar.launch(RespaldoDatos.nombreSugerido(selloDeHoy())) },
        )
        Separador()
        FilaAccion(
            titulo = stringResource(R.string.backup_import),
            detalle = stringResource(R.string.backup_import_desc),
            boton = stringResource(R.string.backup_import_action),
            onClick = { elegirRespaldo.launch(arrayOf("application/zip", "*/*")) },
        )
        Separador()
        BloqueCopiaAutomatica()
    }

    confirmarImportacion?.let { origen ->
        AlertDialog(
            onDismissRequest = { confirmarImportacion = null },
            title = { Text(stringResource(R.string.backup_import_title)) },
            text = { Text(stringResource(R.string.backup_import_explain)) },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.background,
            confirmButton = {
                TextButton(onClick = {
                    confirmarImportacion = null
                    scope.launch {
                        val entrada = context.abrirParaLeer(origen)
                        val contenido = entrada?.use { RespaldoDatos.leer(it) }
                        if (contenido == null) {
                            avisar(context, context.getString(R.string.backup_import_invalid))
                            return@launch
                        }
                        val resultado =
                            RespaldoDatos.restaurar(context, contenido, repositorio)
                        avisar(
                            context,
                            context.getString(
                                R.string.backup_import_done,
                                resultado.registros,
                                resultado.fotos,
                            )
                        )
                    }
                }) { Text(stringResource(R.string.backup_import_action)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarImportacion = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/**
 * La copia automática, dentro del mismo bloque que la copia a mano: es lo
 * mismo, solo que sin acordarse. Apagada por defecto; al encenderla se elige
 * la carpeta y se hace la primera copia en el acto, para que se vea que va.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BloqueCopiaAutomatica() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var estado by remember { mutableStateOf(CopiaAutomatica.estado(context)) }
    var copiando by remember { mutableStateOf(false) }

    fun copiar() {
        if (copiando) return
        copiando = true
        scope.launch {
            val resultado = CopiaAutomatica.copiarAhora(context)
            copiando = false
            estado = CopiaAutomatica.estado(context)
            avisar(
                context,
                if (resultado == null) context.getString(R.string.autobackup_failed)
                else context.getString(
                    R.string.autobackup_done, resultado.registros, resultado.fotos
                ),
            )
        }
    }

    val elegirCarpeta = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { carpeta ->
        if (carpeta == null) return@rememberLauncherForActivityResult
        CopiaAutomatica.activar(context, carpeta)
        estado = CopiaAutomatica.estado(context)
        copiar()
    }

    val subtitulo = when {
        !estado.activa -> stringResource(R.string.autobackup_off)
        estado.fallo -> stringResource(R.string.autobackup_on_failed)
        estado.ultima == 0L -> stringResource(R.string.autobackup_on_never)
        else -> stringResource(
            R.string.autobackup_on_last,
            java.text.DateFormat
                .getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
                .format(Date(estado.ultima)),
        )
    }

    FilaInterruptor(
        titulo = stringResource(R.string.autobackup_title),
        subtitulo = subtitulo,
        marcado = estado.activa,
        tinte = if (estado.fallo) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.primary,
        onCambio = { encender ->
            if (encender) {
                elegirCarpeta.launch(estado.carpeta)
            } else {
                CopiaAutomatica.desactivar(context)
                estado = CopiaAutomatica.estado(context)
            }
        },
    )

    if (!estado.activa) return

    val nombreCarpeta = remember(estado.carpeta) {
        estado.carpeta?.let {
            runCatching { DocumentFile.fromTreeUri(context, it)?.name }.getOrNull()
        } ?: "—"
    }

    BloqueOpciones(
        titulo = stringResource(R.string.autobackup_frequency),
        nota = stringResource(R.string.autobackup_note),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CopiaAutomatica.Frecuencia.entries.forEach { opcion ->
                ChipOpcion(
                    texto = stringResource(
                        when (opcion) {
                            CopiaAutomatica.Frecuencia.DIARIA -> R.string.autobackup_daily
                            CopiaAutomatica.Frecuencia.SEMANAL -> R.string.autobackup_weekly
                        }
                    ),
                    activo = estado.frecuencia == opcion,
                    tinte = MaterialTheme.colorScheme.primary,
                    onClick = {
                        CopiaAutomatica.cambiarFrecuencia(context, opcion)
                        estado = CopiaAutomatica.estado(context)
                    },
                )
            }
        }
    }

    Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
        Text(
            text = stringResource(R.string.autobackup_folder, nombreCarpeta),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { elegirCarpeta.launch(estado.carpeta) },
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) { Text(stringResource(R.string.autobackup_change_folder), fontWeight = FontWeight.SemiBold) }
            OutlinedButton(
                onClick = { copiar() },
                enabled = !copiando,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) { Text(stringResource(R.string.autobackup_copy_now), fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun FilaAccion(
    titulo: String,
    detalle: String,
    boton: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(10.dp))
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) { Text(boton, fontWeight = FontWeight.SemiBold) }
    }
}

private fun avisar(context: android.content.Context, texto: String) {
    Toast.makeText(context, texto, Toast.LENGTH_LONG).show()
}

private fun selloDeHoy(): String =
    SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

/**
 * Elegir el tema. Va con los tres de siempre —seguir al móvil, claro u
 * oscuro— porque «seguir al móvil» es lo que casi todo el mundo quiere, pero
 * quien tiene el sistema en oscuro y prefiere leer esto en claro (o al revés)
 * no debería tener que cambiar el móvil entero para conseguirlo. Los tres son
 * neutros; el cuaderno marrón con Lora queda como cuarta opción, «Grimorio».
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SeccionApariencia() {
    val context = LocalContext.current

    TituloSeccion(stringResource(R.string.appearance_section))
    Bloque {
        BloqueOpciones(titulo = stringResource(R.string.appearance_theme)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TemaApp.Modo.entries.forEach { opcion ->
                    ChipOpcion(
                        texto = stringResource(
                            when (opcion) {
                                TemaApp.Modo.SISTEMA -> R.string.appearance_system
                                TemaApp.Modo.CLARO -> R.string.appearance_light
                                TemaApp.Modo.OSCURO -> R.string.appearance_dark
                                TemaApp.Modo.GRIMORIO -> R.string.appearance_grimoire
                            }
                        ),
                        activo = TemaApp.modo == opcion,
                        tinte = MaterialTheme.colorScheme.primary,
                        onClick = { TemaApp.cambiar(context, opcion) },
                    )
                }
            }
        }
    }
}
