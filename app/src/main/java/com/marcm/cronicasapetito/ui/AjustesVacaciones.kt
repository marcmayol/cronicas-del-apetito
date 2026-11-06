package com.marcm.cronicasapetito.ui

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marcm.cronicasapetito.R
import com.marcm.cronicasapetito.data.EntryKind
import com.marcm.cronicasapetito.notifications.ModoVacaciones
import com.marcm.cronicasapetito.notifications.PrefsRecordatorios
import com.marcm.cronicasapetito.notifications.Vacaciones
import com.marcm.cronicasapetito.notifications.VacacionesPrefs
import com.marcm.cronicasapetito.notifications.horaTexto
import java.time.LocalDate
import java.time.ZoneId

/**
 * Sección «Vacaciones» de Ajustes: de qué día a qué día, y si mientras tanto
 * la app calla o pregunta una vez al día. Se guarda al tocar, como lo demás.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SeccionVacaciones() {
    val context = LocalContext.current
    val tinte = visualDe(EntryKind.FOOD).color

    TituloSeccion(stringResource(R.string.vacation_section))
    var vacaciones by remember { mutableStateOf(VacacionesPrefs.vigentes(context)) }

    fun cambiar(nuevas: Vacaciones) {
        val corregidas = if (nuevas.hasta.isBefore(nuevas.desde)) nuevas.copy(hasta = nuevas.desde)
        else nuevas
        vacaciones = corregidas
        ModoVacaciones.guardar(context, corregidas)
    }

    Bloque {
        val v = vacaciones
        FilaInterruptor(
            titulo = stringResource(R.string.vacation_switch),
            subtitulo = if (v != null) resumenVacaciones(context, v)
            else stringResource(R.string.vacation_off),
            marcado = v != null,
            tinte = tinte,
            onCambio = { activar ->
                if (activar) {
                    val hoy = LocalDate.now()
                    cambiar(
                        Vacaciones(
                            desde = hoy,
                            hasta = hoy.plusDays(VacacionesPrefs.DIAS_POR_DEFECTO - 1),
                            modo = Vacaciones.Modo.UNA_VEZ,
                            horaMin = VacacionesPrefs.HORA_POR_DEFECTO,
                        )
                    )
                } else {
                    vacaciones = null
                    ModoVacaciones.terminar(context)
                }
            },
        )

        if (v != null) {
            Separador()
            FilaFecha(
                titulo = stringResource(R.string.vacation_from),
                fecha = v.desde,
                minima = LocalDate.now(),
                onElegir = { cambiar(v.copy(desde = it)) },
            )
            Separador()
            FilaFecha(
                titulo = stringResource(R.string.vacation_to),
                fecha = v.hasta,
                minima = maxOf(v.desde, LocalDate.now()),
                onElegir = { cambiar(v.copy(hasta = it)) },
            )
            Separador()
            BloqueOpciones(
                titulo = stringResource(R.string.vacation_meanwhile),
                nota = stringResource(R.string.vacation_note),
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipOpcion(
                        texto = stringResource(R.string.vacation_mode_once),
                        activo = v.modo == Vacaciones.Modo.UNA_VEZ,
                        tinte = tinte,
                        onClick = { cambiar(v.copy(modo = Vacaciones.Modo.UNA_VEZ)) },
                    )
                    ChipOpcion(
                        texto = stringResource(R.string.vacation_mode_silent),
                        activo = v.modo == Vacaciones.Modo.SIN_PREGUNTAS,
                        tinte = tinte,
                        onClick = { cambiar(v.copy(modo = Vacaciones.Modo.SIN_PREGUNTAS)) },
                    )
                }
            }
            if (v.modo == Vacaciones.Modo.UNA_VEZ) {
                Separador()
                FilaHora(
                    titulo = stringResource(R.string.vacation_at),
                    minutosDelDia = v.horaMin,
                    onElegir = { cambiar(v.copy(horaMin = it)) },
                )
            }
            if (!PrefsRecordatorios.comidaActiva(context)) {
                Separador()
                Text(
                    text = stringResource(R.string.vacation_meal_off),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorsCronicas.tenue,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }
    }
}

/** «Del 10 ago 2026 al 16 ago 2026 · una vez al día, a las 22:00». */
internal fun resumenVacaciones(context: Context, v: Vacaciones): String {
    val periodo = context.getString(
        R.string.vacation_summary_range, Fechas.corta(v.desde), Fechas.corta(v.hasta)
    )
    val modo = when (v.modo) {
        Vacaciones.Modo.UNA_VEZ ->
            context.getString(R.string.vacation_summary_once, horaTexto(v.horaMin))
        Vacaciones.Modo.SIN_PREGUNTAS -> context.getString(R.string.vacation_summary_silent)
    }
    return "$periodo · $modo"
}

@Composable
private fun FilaFecha(
    titulo: String,
    fecha: LocalDate,
    minima: LocalDate,
    onElegir: (LocalDate) -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        OutlinedButton(
            onClick = { pedirFecha(context, fecha, minima, onElegir) },
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Text(Fechas.corta(fecha), fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Selector nativo con tope por abajo: las vacaciones no empiezan en el pasado. */
private fun pedirFecha(
    context: Context,
    inicial: LocalDate,
    minima: LocalDate,
    onElegir: (LocalDate) -> Unit,
) {
    DatePickerDialog(
        context,
        TemaApp.temaSelector(context),
        { _, anio, mes, dia -> onElegir(LocalDate.of(anio, mes + 1, dia)) },
        inicial.year,
        inicial.monthValue - 1,
        inicial.dayOfMonth,
    ).apply {
        datePicker.minDate =
            minima.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.show()
}
