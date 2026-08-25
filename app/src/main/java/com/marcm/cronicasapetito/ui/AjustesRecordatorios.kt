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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marcm.cronicasapetito.data.EntryKind
import com.marcm.cronicasapetito.notifications.GymAlarmScheduler
import com.marcm.cronicasapetito.notifications.MealAlarmScheduler
import com.marcm.cronicasapetito.notifications.PrefsRecordatorios
import com.marcm.cronicasapetito.R
import com.marcm.cronicasapetito.notifications.frecuenciaCorta
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
    TituloSeccion(stringResource(R.string.reminders_meal_section))
    var comidaActiva by remember { mutableStateOf(PrefsRecordatorios.comidaActiva(context)) }
    var inicioMin by remember { mutableIntStateOf(PrefsRecordatorios.comidaInicioMin(context)) }
    var finMin by remember { mutableIntStateOf(PrefsRecordatorios.comidaFinMin(context)) }
    var cadaMin by remember { mutableIntStateOf(PrefsRecordatorios.comidaCadaMin(context)) }
    var caminataEnAviso by remember {
        mutableStateOf(PrefsRecordatorios.caminataEnAviso(context))
    }

    Bloque {
        FilaInterruptor(
            titulo = stringResource(R.string.reminders_meal_switch),
            subtitulo = if (comidaActiva) resumenVentana(context, inicioMin, finMin, cadaMin)
            else stringResource(R.string.reminders_meal_off),
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
                titulo = stringResource(R.string.reminders_start_at),
                minutosDelDia = inicioMin,
                onElegir = { elegido ->
                    inicioMin = elegido
                    PrefsRecordatorios.setComidaVentana(context, elegido, finMin)
                    MealAlarmScheduler.scheduleNext(context)
                },
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
                onElegir = { elegido ->
                    finMin = elegido
                    PrefsRecordatorios.setComidaVentana(context, inicioMin, elegido)
                    MealAlarmScheduler.scheduleNext(context)
                },
            )
            Separador()
            BloqueOpciones(titulo = stringResource(R.string.reminders_how_often)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrefsRecordatorios.FRECUENCIAS.forEach { opcion ->
                        ChipOpcion(
                            texto = frecuenciaCorta(context, opcion),
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
                titulo = stringResource(R.string.reminders_walk_switch),
                subtitulo = stringResource(
                    if (caminataEnAviso) R.string.reminders_walk_on
                    else R.string.reminders_walk_off
                ),
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
    // Estado de ánimo
    // -----------------------------------------------------------------------
    TituloSeccion(stringResource(R.string.reminders_mood_section))
    var animoActivo by remember { mutableStateOf(PrefsRecordatorios.animoActivo(context)) }

    Bloque {
        FilaInterruptor(
            titulo = stringResource(R.string.reminders_mood_switch),
            subtitulo = stringResource(
                if (animoActivo) R.string.reminders_mood_on else R.string.reminders_mood_off
            ),
            marcado = animoActivo,
            tinte = visualDe(EntryKind.MOOD).color,
            onCambio = {
                animoActivo = it
                PrefsRecordatorios.setAnimoActivo(context, it)
            },
        )
    }

    // -----------------------------------------------------------------------
    // Gimnasio
    // -----------------------------------------------------------------------
    TituloSeccion(stringResource(R.string.reminders_gym_section))
    var gymActivo by remember { mutableStateOf(PrefsRecordatorios.gymActivo(context)) }
    var gymHora by remember { mutableIntStateOf(PrefsRecordatorios.gymHoraMin(context)) }
    var gymDias by remember { mutableStateOf(PrefsRecordatorios.gymDias(context)) }
    var gymObjetivo by remember {
        mutableIntStateOf(PrefsRecordatorios.gymObjetivoSemanal(context))
    }

    Bloque {
        FilaInterruptor(
            titulo = stringResource(R.string.reminders_gym_switch),
            subtitulo = if (gymActivo) resumenGimnasio(context, gymHora, gymDias, gymObjetivo)
            else stringResource(R.string.reminders_gym_off),
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
                titulo = stringResource(R.string.reminders_gym_at),
                minutosDelDia = gymHora,
                onElegir = { elegido ->
                    gymHora = elegido
                    PrefsRecordatorios.setGymHoraMin(context, elegido)
                    GymAlarmScheduler.scheduleNext(context)
                },
            )
            Separador()
            BloqueOpciones(
                titulo = stringResource(R.string.reminders_gym_days),
                nota = if (gymDias.isEmpty()) stringResource(R.string.reminders_gym_no_days) else null,
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
                titulo = stringResource(R.string.reminders_gym_goal),
                nota = stringResource(R.string.reminders_gym_goal_note),
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
                        texto = stringResource(R.string.reminders_no_cap),
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

private fun resumenVentana(context: Context, inicioMin: Int, finMin: Int, cadaMin: Int): String {
    val duracion = PrefsRecordatorios.duracion(inicioMin, finMin)
    val avisos = duracion / cadaMin + 1
    return context.getString(
        R.string.reminders_summary_meal,
        horaTexto(inicioMin),
        horaTexto(finMin),
        frecuenciaTexto(context, cadaMin),
        context.resources.getQuantityString(R.plurals.reminders_per_day, avisos, avisos),
    )
}

private fun resumenGimnasio(
    context: Context,
    horaMin: Int,
    dias: Set<Int>,
    objetivo: Int,
): String {
    val diasTexto = when {
        dias.isEmpty() -> context.getString(R.string.reminders_no_days_short)
        dias.size == 7 -> context.getString(R.string.reminders_every_day)
        else -> ORDEN_DIAS.filter { it in dias }
            .joinToString(" ") { inicialDeDia(context, it) }
    }
    val tope = if (objetivo == PrefsRecordatorios.GYM_SIN_OBJETIVO)
        context.getString(R.string.reminders_no_cap_short)
    else context.getString(R.string.reminders_cap_short, objetivo)
    return context.getString(R.string.reminders_summary_gym, horaTexto(horaMin), diasTexto, tope)
}

/** La semana empieza en lunes, como en el resto de la app. */
internal val ORDEN_DIAS = listOf(
    Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
    Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY,
)

/** Inicial del día, sacada del mismo sitio que las del calendario. */
internal fun inicialDeDia(context: Context, diaCalendar: Int): String {
    val iniciales = context.getString(R.string.weekday_initials)
    return iniciales.getOrNull(ORDEN_DIAS.indexOf(diaCalendar))?.toString() ?: "?"
}

// ---------------------------------------------------------------------------
// Piezas de la sección
// ---------------------------------------------------------------------------

@Composable
internal fun FilaInterruptor(
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
internal fun FilaHora(
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
internal fun BloqueOpciones(
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
internal fun ChipOpcion(texto: String, activo: Boolean, tinte: Color, onClick: () -> Unit) {
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
                    text = inicialDeDia(LocalContext.current, dia),
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
