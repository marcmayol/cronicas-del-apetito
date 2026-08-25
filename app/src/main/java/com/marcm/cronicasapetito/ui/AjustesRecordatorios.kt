package com.marcm.cronicasapetito.ui

import android.app.TimePickerDialog
import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marcm.cronicasapetito.data.EntryKind
import com.marcm.cronicasapetito.notifications.GymAlarmScheduler
import com.marcm.cronicasapetito.notifications.MealAlarmScheduler
import com.marcm.cronicasapetito.notifications.PrefsRecordatorios
import com.marcm.cronicasapetito.notifications.frecuenciaTexto
import com.marcm.cronicasapetito.notifications.horaTexto
import java.util.Calendar

/**
 * Sección «Recordatorios» de Ajustes: la horquilla horaria y la frecuencia de la
 * pregunta de comida, si la caminata asoma en el aviso, y el carril del gimnasio
 * con su hora, sus días y su objetivo semanal.
 *
 * Cada cambio se guarda y reprograma la alarma en el acto: no hay botón de
 * «aplicar» que se pueda olvidar.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SeccionRecordatorios() {
    val context = LocalContext.current

    // -----------------------------------------------------------------------
    // Comida
    // -----------------------------------------------------------------------
    TituloSeccion("Recordatorios de comida")
    var comidaActiva by remember { mutableStateOf(PrefsRecordatorios.comidaActiva(context)) }
    var inicioMin by remember { mutableIntStateOf(PrefsRecordatorios.comidaInicioMin(context)) }
    var finMin by remember { mutableIntStateOf(PrefsRecordatorios.comidaFinMin(context)) }
    var cadaMin by remember { mutableIntStateOf(PrefsRecordatorios.comidaCadaMin(context)) }
    var caminataEnAviso by remember {
        mutableStateOf(PrefsRecordatorios.caminataEnAviso(context))
    }

    Bloque {
        FilaInterruptor(
            titulo = "Preguntarme si he comido",
            subtitulo = if (comidaActiva) resumenVentana(inicioMin, finMin, cadaMin)
            else "Desactivado: no llegará ningún aviso",
            marcado = comidaActiva,
            tinte = visualDe(EntryKind.FOOD).color,
            onCambio = {
                comidaActiva = it
                PrefsRecordatorios.setComidaActiva(context, it)
                MealAlarmScheduler.scheduleNext(context)
            },
        )

        if (comidaActiva) {
            Separador()
            FilaHora(
                titulo = "Empiezan a las",
                minutosDelDia = inicioMin,
                onElegir = { elegido ->
                    inicioMin = elegido
                    PrefsRecordatorios.setComidaVentana(context, elegido, finMin)
                    MealAlarmScheduler.scheduleNext(context)
                },
            )
            Separador()
            FilaHora(
                titulo = "Terminan a las",
                subtitulo = when {
                    finMin == inicioMin -> "Todo el día"
                    finMin < inicioMin -> "Del día siguiente"
                    else -> null
                },
                minutosDelDia = finMin,
                onElegir = { elegido ->
                    finMin = elegido
                    PrefsRecordatorios.setComidaVentana(context, inicioMin, elegido)
                    MealAlarmScheduler.scheduleNext(context)
                },
            )
            Separador()
            BloqueOpciones(titulo = "Cada cuánto") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrefsRecordatorios.FRECUENCIAS.forEach { opcion ->
                        ChipOpcion(
                            texto = frecuenciaTexto(opcion).removePrefix("cada "),
                            activo = cadaMin == opcion,
                            tinte = visualDe(EntryKind.FOOD).color,
                            onClick = {
                                cadaMin = opcion
                                PrefsRecordatorios.setComidaCadaMin(context, opcion)
                                MealAlarmScheduler.scheduleNext(context)
                            },
                        )
                    }
                }
            }
            Separador()
            FilaInterruptor(
                titulo = "Ofrecer «Caminar» en el aviso",
                subtitulo = if (caminataEnAviso)
                    "El aviso de comida trae también el botón de caminata"
                else "El aviso solo pregunta por la comida",
                marcado = caminataEnAviso,
                tinte = visualDe(EntryKind.WALK).color,
                onCambio = {
                    caminataEnAviso = it
                    PrefsRecordatorios.setCaminataEnAviso(context, it)
                },
            )
        }
    }

    // -----------------------------------------------------------------------
    // Gimnasio
    // -----------------------------------------------------------------------
    TituloSeccion("Recordatorio de gimnasio")
    var gymActivo by remember { mutableStateOf(PrefsRecordatorios.gymActivo(context)) }
    var gymHora by remember { mutableIntStateOf(PrefsRecordatorios.gymHoraMin(context)) }
    var gymDias by remember { mutableStateOf(PrefsRecordatorios.gymDias(context)) }
    var gymObjetivo by remember {
        mutableIntStateOf(PrefsRecordatorios.gymObjetivoSemanal(context))
    }

    Bloque {
        FilaInterruptor(
            titulo = "Preguntarme por el gimnasio",
            subtitulo = if (gymActivo) resumenGimnasio(gymHora, gymDias, gymObjetivo)
            else "Desactivado: no se preguntará nunca",
            marcado = gymActivo,
            tinte = visualDe(EntryKind.GYM).color,
            onCambio = {
                gymActivo = it
                PrefsRecordatorios.setGymActivo(context, it)
                GymAlarmScheduler.scheduleNext(context)
            },
        )

        if (gymActivo) {
            Separador()
            FilaHora(
                titulo = "A las",
                minutosDelDia = gymHora,
                onElegir = { elegido ->
                    gymHora = elegido
                    PrefsRecordatorios.setGymHoraMin(context, elegido)
                    GymAlarmScheduler.scheduleNext(context)
                },
            )
            Separador()
            BloqueOpciones(
                titulo = "Qué días",
                nota = if (gymDias.isEmpty()) "Sin ningún día no se preguntará." else null,
            ) {
                SelectorDias(
                    dias = gymDias,
                    tinte = visualDe(EntryKind.GYM).color,
                    onCambio = { nuevos ->
                        gymDias = nuevos
                        PrefsRecordatorios.setGymDias(context, nuevos)
                        GymAlarmScheduler.scheduleNext(context)
                    },
                )
            }
            Separador()
            BloqueOpciones(
                titulo = "Dejar de preguntar al llegar a",
                nota = "Veces por semana. Cuando ya has ido esas veces, la semana se da por hecha.",
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..7).forEach { veces ->
                        ChipOpcion(
                            texto = "$veces",
                            activo = gymObjetivo == veces,
                            tinte = visualDe(EntryKind.GYM).color,
                            onClick = {
                                gymObjetivo = veces
                                PrefsRecordatorios.setGymObjetivoSemanal(context, veces)
                            },
                        )
                    }
                    ChipOpcion(
                        texto = "Sin tope",
                        activo = gymObjetivo == PrefsRecordatorios.GYM_SIN_OBJETIVO,
                        tinte = visualDe(EntryKind.GYM).color,
                        onClick = {
                            gymObjetivo = PrefsRecordatorios.GYM_SIN_OBJETIVO
                            PrefsRecordatorios.setGymObjetivoSemanal(
                                context, PrefsRecordatorios.GYM_SIN_OBJETIVO
                            )
                        },
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Resúmenes: lo que se lee bajo cada interruptor sin abrir nada
// ---------------------------------------------------------------------------

private fun resumenVentana(inicioMin: Int, finMin: Int, cadaMin: Int): String {
    val duracion = PrefsRecordatorios.duracion(inicioMin, finMin)
    val avisos = duracion / cadaMin + 1
    return "${horaTexto(inicioMin)} → ${horaTexto(finMin)} · ${frecuenciaTexto(cadaMin)} " +
        "· $avisos ${if (avisos == 1) "aviso" else "avisos"} al día"
}

private fun resumenGimnasio(horaMin: Int, dias: Set<Int>, objetivo: Int): String {
    val diasTexto = when {
        dias.isEmpty() -> "ningún día"
        dias.size == 7 -> "todos los días"
        else -> ORDEN_DIAS.filter { it in dias }.joinToString(" ") { INICIALES.getValue(it) }
    }
    val tope = if (objetivo == PrefsRecordatorios.GYM_SIN_OBJETIVO) "sin tope"
    else "hasta $objetivo/semana"
    return "${horaTexto(horaMin)} · $diasTexto · $tope"
}

/** La semana empieza en lunes, como en el resto de la app. */
private val ORDEN_DIAS = listOf(
    Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
    Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY,
)

private val INICIALES = mapOf(
    Calendar.MONDAY to "L", Calendar.TUESDAY to "M", Calendar.WEDNESDAY to "X",
    Calendar.THURSDAY to "J", Calendar.FRIDAY to "V", Calendar.SATURDAY to "S",
    Calendar.SUNDAY to "D",
)

// ---------------------------------------------------------------------------
// Piezas de la sección
// ---------------------------------------------------------------------------

@Composable
private fun FilaInterruptor(
    titulo: String,
    subtitulo: String,
    marcado: Boolean,
    tinte: Color,
    onCambio: (Boolean) -> Unit,
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
                text = subtitulo,
                style = MaterialTheme.typography.bodySmall,
                color = if (marcado) tinte else colorsCronicas.tenue,
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = marcado, onCheckedChange = onCambio)
    }
}

/** Fila con una hora y su botón: el mismo gesto que «Fecha y hora» al anotar. */
@Composable
private fun FilaHora(
    titulo: String,
    minutosDelDia: Int,
    subtitulo: String? = null,
    onElegir: (Int) -> Unit,
) {
    val context = LocalContext.current
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
            if (subtitulo != null) {
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorsCronicas.tenue,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(
            onClick = { pedirHora(context, minutosDelDia, onElegir) },
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Text(horaTexto(minutosDelDia), fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun pedirHora(context: Context, actual: Int, onElegir: (Int) -> Unit) {
    TimePickerDialog(
        context,
        { _, hora, minuto -> onElegir(hora * 60 + minuto) },
        actual / 60,
        actual % 60,
        DateFormat.is24HourFormat(context),
    ).show()
}

@Composable
private fun BloqueOpciones(
    titulo: String,
    nota: String? = null,
    contenido: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(14.dp)) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(10.dp))
        contenido()
        if (nota != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = nota,
                style = MaterialTheme.typography.bodySmall,
                color = colorsCronicas.tenue,
            )
        }
    }
}

@Composable
private fun ChipOpcion(texto: String, activo: Boolean, tinte: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (activo) tinte.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (activo) 1.4.dp else 1.dp,
            if (activo) tinte else MaterialTheme.colorScheme.outline,
        ),
        onClick = onClick,
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (activo) FontWeight.SemiBold else FontWeight.Medium,
            color = if (activo) tinte else MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

/** L M X J V S D: los mismos rótulos que el calendario de la vista Mes. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectorDias(dias: Set<Int>, tinte: Color, onCambio: (Set<Int>) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ORDEN_DIAS.forEach { dia ->
            val activo = dia in dias
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = if (activo) tinte.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    if (activo) 1.4.dp else 1.dp,
                    if (activo) tinte else MaterialTheme.colorScheme.outline,
                ),
                onClick = { onCambio(if (activo) dias - dia else dias + dia) },
            ) {
                Text(
                    text = INICIALES.getValue(dia),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (activo) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (activo) tinte else MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 11.dp),
                )
            }
        }
    }
}
