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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marcm.cronicasapetito.data.RangoFechas
import java.time.LocalDate

/** Atajos: cubren el 90% de los casos, el rango manual queda debajo. */
private enum class Atajo(val etiqueta: String) {
    SIETE("Últimos 7 días"),
    TREINTA("Últimos 30 días"),
    ESTE_MES("Este mes"),
    TODO("Todo");

    fun rango(hoy: LocalDate): RangoFechas? = when (this) {
        SIETE -> RangoFechas.ultimosDias(7, hoy)
        TREINTA -> RangoFechas.ultimosDias(30, hoy)
        ESTE_MES -> RangoFechas.esteMes(hoy)
        TODO -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FiltroSheet(
    filtroActual: RangoFechas?,
    onCerrar: () -> Unit,
    onAplicar: (RangoFechas?) -> Unit,
) {
    val hoy = remember { LocalDate.now() }
    var desde by remember { mutableStateOf(filtroActual?.desde ?: hoy.minusDays(29)) }
    var hasta by remember { mutableStateOf(filtroActual?.hasta ?: hoy) }
    var atajoElegido by remember { mutableStateOf<Atajo?>(null) }

    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Filtrar por fechas", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Acota lo que ves en las tres vistas. No afecta a lo guardado.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Atajo.entries.forEach { atajo ->
                    ChipAtajo(
                        texto = atajo.etiqueta,
                        activo = atajoElegido == atajo,
                        onClick = {
                            atajoElegido = atajo
                            atajo.rango(hoy)?.let { desde = it.desde; hasta = it.hasta }
                        },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoFecha(
                    etiqueta = "Desde",
                    fecha = desde,
                    modifier = Modifier.weight(1f),
                    onElegir = { desde = it; atajoElegido = null },
                )
                CampoFecha(
                    etiqueta = "Hasta",
                    fecha = hasta,
                    modifier = Modifier.weight(1f),
                    onElegir = { hasta = it; atajoElegido = null },
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Las fechas futuras no se pueden elegir.",
                style = MaterialTheme.typography.bodySmall,
                color = colorsCronicas.tenue,
            )

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCerrar) { Text("Cancelar") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        // Si el rango viene al revés se ordena solo: nunca un
                        // botón muerto como en el diálogo de exportar de antes.
                        onAplicar(
                            if (atajoElegido == Atajo.TODO) null else RangoFechas.de(desde, hasta)
                        )
                    },
                    shape = RoundedCornerShape(999.dp),
                ) { Text("Aplicar") }
            }
        }
    }
}

@Composable
private fun ChipAtajo(texto: String, activo: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (activo) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (activo) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = texto,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (activo) FontWeight.SemiBold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun CampoFecha(
    etiqueta: String,
    fecha: LocalDate,
    onElegir: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        onClick = { elegirFecha(context, fecha, onElegir) },
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = etiqueta.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = colorsCronicas.tenue,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = visualDe(com.marcm.cronicasapetito.data.EntryKind.FOOD).color,
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = Fechas.corta(fecha),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/** Selector nativo, con tope en hoy: no se puede anotar ni mirar el futuro. */
private fun elegirFecha(context: Context, inicial: LocalDate, onElegir: (LocalDate) -> Unit) {
    DatePickerDialog(
        context,
        { _, anio, mes, dia -> onElegir(LocalDate.of(anio, mes + 1, dia)) },
        inicial.year,
        inicial.monthValue - 1,
        inicial.dayOfMonth,
    ).apply {
        datePicker.maxDate = System.currentTimeMillis()
    }.show()
}
