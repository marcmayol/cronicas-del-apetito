package com.marcm.cronicasapetito.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.marcm.cronicasapetito.R
import com.marcm.cronicasapetito.data.EntryKind
import com.marcm.cronicasapetito.notifications.GymAlarmScheduler
import com.marcm.cronicasapetito.notifications.MealAlarmScheduler
import com.marcm.cronicasapetito.notifications.PrefsRecordatorios
import com.marcm.cronicasapetito.notifications.frecuenciaCorta
import com.marcm.cronicasapetito.notifications.horaTexto

/**
 * Lo primero que se ve al instalar. No es un tutorial: son las tres preguntas
 * cuya respuesta la app necesita para no equivocarse desde el primer día —qué
 * quieres llevar, a qué horas te viene bien, y el permiso para avisarte—, y se
 * sale de aquí con todo montado.
 *
 * Nada de lo que se elige aquí queda cerrado: todo vive luego en Ajustes.
 */
class BienvenidaActivity : ComponentActivity() {

    private val permisoAvisos = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* concedido o no, se sigue: la app funciona igual sin avisos */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 15 en adelante dibuja bajo las barras del sistema quiera o no.
        // Declararlo aquí hace que se comporte igual en todas las versiones, y
        // que los insets que aplica la UI sean los mismos en el móvil de 2019
        // que en el de este año.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CronicasTheme {
                Bienvenida(
                    onPedirPermiso = { pedirPermisoAvisos() },
                    onTerminar = {
                        PrefsRecordatorios.setBienvenidaVista(this, true)
                        MealAlarmScheduler.scheduleNext(this)
                        GymAlarmScheduler.scheduleNext(this)
                        finish()
                    },
                )
            }
        }
    }

    private fun pedirPermisoAvisos() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val concedido = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!concedido) permisoAvisos.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

private enum class PasoBienvenida { CARRILES, HORARIO, AVISOS }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Bienvenida(onPedirPermiso: () -> Unit, onTerminar: () -> Unit) {
    val context = LocalContext.current
    var paso by remember { mutableStateOf(PasoBienvenida.CARRILES) }

    var caminata by remember { mutableStateOf(PrefsRecordatorios.caminataEnAviso(context)) }
    var gimnasio by remember { mutableStateOf(PrefsRecordatorios.gymActivo(context)) }
    var animo by remember { mutableStateOf(PrefsRecordatorios.animoActivo(context)) }
    var inicioMin by remember { mutableIntStateOf(PrefsRecordatorios.comidaInicioMin(context)) }
    var finMin by remember { mutableIntStateOf(PrefsRecordatorios.comidaFinMin(context)) }
    var cadaMin by remember { mutableIntStateOf(PrefsRecordatorios.comidaCadaMin(context)) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BarraPasos(
                paso = paso,
                onAtras = { paso = PasoBienvenida.entries[paso.ordinal - 1] },
                onAvanzar = {
                    when (paso) {
                        PasoBienvenida.CARRILES -> {
                            PrefsRecordatorios.setCaminataEnAviso(context, caminata)
                            PrefsRecordatorios.setGymActivo(context, gimnasio)
                            PrefsRecordatorios.setAnimoActivo(context, animo)
                            paso = PasoBienvenida.HORARIO
                        }

                        PasoBienvenida.HORARIO -> {
                            PrefsRecordatorios.setComidaVentana(context, inicioMin, finMin)
                            PrefsRecordatorios.setComidaCadaMin(context, cadaMin)
                            paso = PasoBienvenida.AVISOS
                            onPedirPermiso()
                        }

                        PasoBienvenida.AVISOS -> onTerminar()
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))
            Tramos(total = PasoBienvenida.entries.size, hechos = paso.ordinal + 1)
            Spacer(Modifier.height(22.dp))

            when (paso) {
                PasoBienvenida.CARRILES -> {
                    Titulo(stringResource(R.string.welcome_kinds_title))
                    Explicacion(stringResource(R.string.welcome_kinds_body))
                    Spacer(Modifier.height(18.dp))
                    Bloque {
                        FilaCarril(
                            kind = EntryKind.FOOD,
                            titulo = stringResource(R.string.kind_food),
                            detalle = stringResource(R.string.welcome_food_always),
                            marcado = true,
                            fijo = true,
                            onCambio = {},
                        )
                        Separador()
                        FilaCarril(
                            kind = EntryKind.WALK,
                            titulo = stringResource(R.string.kind_walk),
                            detalle = stringResource(R.string.welcome_walk_detail),
                            marcado = caminata,
                            onCambio = { caminata = it },
                        )
                        Separador()
                        FilaCarril(
                            kind = EntryKind.GYM,
                            titulo = stringResource(R.string.kind_gym),
                            detalle = stringResource(R.string.welcome_gym_detail),
                            marcado = gimnasio,
                            onCambio = { gimnasio = it },
                        )
                        Separador()
                        FilaCarril(
                            kind = EntryKind.MOOD,
                            titulo = stringResource(R.string.kind_mood),
                            detalle = stringResource(R.string.welcome_mood_detail),
                            marcado = animo,
                            onCambio = { animo = it },
                        )
                    }
                }

                PasoBienvenida.HORARIO -> {
                    Titulo(stringResource(R.string.welcome_when_title))
                    Explicacion(stringResource(R.string.welcome_when_body))
                    Spacer(Modifier.height(18.dp))
                    Bloque {
                        FilaHora(
                            titulo = stringResource(R.string.reminders_start_at),
                            minutosDelDia = inicioMin,
                            onElegir = { inicioMin = it },
                        )
                        Separador()
                        FilaHora(
                            titulo = stringResource(R.string.reminders_end_at),
                            subtitulo = when {
                                finMin == inicioMin -> stringResource(R.string.reminders_all_day)
                                finMin < inicioMin -> stringResource(R.string.reminders_next_day)
                                else -> null
                            },
                            minutosDelDia = finMin,
                            onElegir = { finMin = it },
                        )
                        Separador()
                        BloqueOpciones(titulo = stringResource(R.string.reminders_how_often)) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PrefsRecordatorios.FRECUENCIAS.forEach { opcion ->
                                    ChipOpcion(
                                        texto = frecuenciaCorta(context, opcion),
                                        activo = cadaMin == opcion,
                                        tinte = visualDe(EntryKind.FOOD).color,
                                        onClick = { cadaMin = opcion },
                                    )
                                }
                            }
                        }
                    }
                }

                PasoBienvenida.AVISOS -> {
                    Titulo(stringResource(R.string.welcome_ready_title))
                    Explicacion(stringResource(R.string.welcome_ready_body))
                    Spacer(Modifier.height(18.dp))
                    Bloque {
                        Column(Modifier.padding(16.dp)) {
                            ResumenLinea(
                                stringResource(
                                    R.string.welcome_summary_meals,
                                    horaTexto(inicioMin),
                                    horaTexto(finMin),
                                    frecuenciaCorta(context, cadaMin),
                                )
                            )
                            if (caminata) ResumenLinea(stringResource(R.string.welcome_summary_walk))
                            if (gimnasio) ResumenLinea(stringResource(R.string.welcome_summary_gym))
                            if (animo) ResumenLinea(stringResource(R.string.welcome_summary_mood))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Explicacion(stringResource(R.string.welcome_change_later))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun Explicacion(texto: String) {
    Spacer(Modifier.height(8.dp))
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ResumenLinea(texto: String) {
    Row(modifier = Modifier.padding(vertical = 5.dp)) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = visualDe(EntryKind.WALK).color,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Fila de un carril. La comida va marcada y sin interruptor: es de lo que trata
 * la app, y ofrecer apagarla en la primera pantalla sería ofrecer nada.
 */
@Composable
private fun FilaCarril(
    kind: String,
    titulo: String,
    detalle: String,
    marcado: Boolean,
    onCambio: (Boolean) -> Unit,
    fijo: Boolean = false,
) {
    val visual = visualDe(kind)
    Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .background(visual.contenedor, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                iconoDe(kind),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = visual.color,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = detalle,
                style = MaterialTheme.typography.bodySmall,
                color = colorsCronicas.tenue,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (fijo) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = visual.color,
            )
        } else {
            Switch(checked = marcado, onCheckedChange = onCambio)
        }
    }
}

@Composable
private fun Tramos(total: Int, hechos: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(total) { indice ->
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(
                        if (indice < hechos) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape,
                    )
            )
        }
    }
}

@Composable
private fun BarraPasos(paso: PasoBienvenida, onAtras: () -> Unit, onAvanzar: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        // Mismo motivo que en BarraGuardar: el fondo llega al borde, los
        // botones se quedan por encima de la barra de navegación.
        Column(
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
            ),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (paso == PasoBienvenida.CARRILES) {
                    Spacer(Modifier.width(1.dp))
                } else {
                    TextButton(onClick = onAtras) {
                        Text(stringResource(R.string.action_previous))
                    }
                }
                Button(onClick = onAvanzar, shape = RoundedCornerShape(999.dp)) {
                    Text(
                        text = stringResource(
                            if (paso == PasoBienvenida.AVISOS) R.string.welcome_start
                            else R.string.action_continue
                        ),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

