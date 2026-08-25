package com.marcm.cronicasapetito.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marcm.cronicasapetito.R
import com.marcm.cronicasapetito.data.EntryKind
import com.marcm.cronicasapetito.data.GymAnswer
import com.marcm.cronicasapetito.data.MealEntry
import com.marcm.cronicasapetito.data.Periodos
import com.marcm.cronicasapetito.data.RangoFechas
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------------------
// Fechas
//
// Los patrones viven en strings.xml porque cada idioma ordena la fecha a su
// manera: «lunes 17 de agosto» y «Monday, August 17» no son el mismo formato
// con otras palabras. [init] lo llama CronicasApp al arrancar.
// ---------------------------------------------------------------------------

object Fechas {

    private var contexto: Context? = null

    fun init(context: Context) {
        contexto = context.applicationContext
    }

    private fun patron(@StringRes res: Int, pordefecto: String): String =
        contexto?.getString(res) ?: pordefecto

    private val locale: Locale get() = Locale.getDefault()

    private fun formato(@StringRes res: Int, pordefecto: String) =
        DateTimeFormatter.ofPattern(patron(res, pordefecto), locale)

    private val diaLargo get() = formato(R.string.fmt_day_long, "EEEE d 'de' MMMM")
    private val diaConAnio get() = formato(R.string.fmt_day_long_year, "EEEE d 'de' MMMM 'de' yyyy")
    private val diaYMes get() = formato(R.string.fmt_day_month, "d 'de' MMMM")
    private val diaCorto get() = formato(R.string.fmt_day_month_short_year, "d MMM yyyy")
    private val mesYAnio get() = formato(R.string.fmt_month_year, "MMMM yyyy")

    private fun capitalizar(texto: String) = texto.replaceFirstChar { it.uppercase(locale) }

    private fun texto(@StringRes res: Int, vararg args: Any): String =
        contexto?.getString(res, *args) ?: args.joinToString(" ")

    fun dia(d: LocalDate): String = capitalizar(diaLargo.format(d))
    fun diaCompleto(d: LocalDate): String = capitalizar(diaConAnio.format(d))
    fun corta(d: LocalDate): String = diaCorto.format(d)
    fun mes(m: YearMonth): String = capitalizar(mesYAnio.format(m.atDay(1)))

    /** «Semana del 10 al 16 de agosto», o con los dos meses si los cruza. */
    fun semana(lunes: LocalDate): String {
        val domingo = lunes.plusDays(6)
        return if (lunes.month == domingo.month) {
            texto(R.string.week_range_same_month, lunes.dayOfMonth, diaYMes.format(domingo))
        } else {
            texto(
                R.string.week_range_cross_month,
                diaYMes.format(lunes),
                diaYMes.format(domingo),
            )
        }
    }

    /** «Del 1 al 15 de agosto» — lo que dice el chip del filtro. */
    fun rango(r: RangoFechas): String = when {
        r.desde == r.hasta -> capitalizar(diaYMes.format(r.desde))
        r.desde.month == r.hasta.month && r.desde.year == r.hasta.year ->
            texto(R.string.date_range_same_month, r.desde.dayOfMonth, diaYMes.format(r.hasta))
        else -> texto(
            R.string.date_range_cross_month,
            diaYMes.format(r.desde),
            diaYMes.format(r.hasta),
        )
    }

    /** Igual pero con año: es lo que viaja en lo que se comparte. */
    fun rangoConAnio(r: RangoFechas): String =
        texto(R.string.date_range_with_year, rango(r), r.hasta.year)

    fun hora(millis: Long): String =
        java.text.SimpleDateFormat("HH:mm", locale).format(Date(millis))
}

/** Inicial del día de la semana como se pinta en el calendario: L M X J V S D. */
fun inicialDia(context: Context, d: LocalDate): String {
    val iniciales = context.getString(R.string.weekday_initials)
    return iniciales.getOrNull(d.dayOfWeek.value - 1)?.toString() ?: "?"
}

fun iconoDe(kind: String): ImageVector = when (kind) {
    EntryKind.FOOD -> Icons.Filled.Restaurant
    EntryKind.WALK -> Icons.Filled.DirectionsWalk
    EntryKind.MOOD -> Icons.Outlined.ChatBubbleOutline
    else -> Icons.Filled.FitnessCenter
}

/**
 * Cómo se lee un registro: los minutos se dicen en palabras y la respuesta del
 * gimnasio, que por dentro es un valor, vuelve a ser una frase. La usan la lista
 * y también el PDF, para que digan exactamente lo mismo.
 */
fun contenidoLegible(context: Context, entry: MealEntry): String = when (entry.kind) {
    EntryKind.WALK -> entry.minutes?.let { context.getString(R.string.content_walk_minutes, it) }
        ?: entry.content
    EntryKind.GYM -> context.getString(
        if (GymAnswer.esAfirmativo(entry.content)) R.string.content_gym_yes
        else R.string.content_gym_no
    )
    else -> entry.content
}

// ---------------------------------------------------------------------------
// Piezas del sistema
// ---------------------------------------------------------------------------

/**
 * Selector de vista: segmentado siempre visible bajo la barra. Se ve en qué
 * vista estás sin abrir nada y se cambia de una en un toque.
 */
@Composable
fun SelectorVista(
    vista: Vista,
    onCambiar: (Vista) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .background(MaterialTheme.colorScheme.surface, CircleShape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Vista.entries.forEachIndexed { indice, opcion ->
            val activa = opcion == vista
            if (indice > 0) {
                Box(
                    Modifier
                        .width(1.dp)
                        .height(38.dp)
                        .background(MaterialTheme.colorScheme.outline)
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (activa) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                    )
                    .clickable { onCambiar(opcion) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (activa) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(Modifier.width(5.dp))
                }
                Text(
                    text = when (opcion) {
                        Vista.DIA -> stringResource(R.string.view_day)
                        Vista.SEMANA -> stringResource(R.string.view_week)
                        Vista.MES -> stringResource(R.string.view_month)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (activa) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (activa) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Chip del filtro activo. Es una de las tres señales de que hay filtro (con el
 * punto del botón y el contador): mirar una pantalla medio vacía sin saber por
 * qué es el peor resultado posible de esta función.
 */
@Composable
fun ChipFiltro(
    rango: RangoFechas,
    registros: Int,
    onQuitar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .clickable { onQuitar() }
                .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = Fechas.rango(rango),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.width(7.dp))
            Box(
                Modifier
                    .size(22.dp)
                    .background(Color.White.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.filter_clear),
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = pluralStringResource(R.plurals.entry_count, registros, registros),
            style = MaterialTheme.typography.bodySmall,
            color = colorsCronicas.tenue,
        )
    }
}

/** Cabecera de día en la lista: «Lunes 17 de agosto» + «hoy». */
@Composable
fun CabeceraDia(dia: LocalDate, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = Fechas.dia(dia),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (dia == LocalDate.now()) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.label_today),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorsCronicas.tenue,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}

/**
 * Tarjeta de un registro. Triple código del tipo — color, icono y glifo — sobre
 * fondo blanco neutro: así una lista de doce registros respira, y una fotocopia
 * en blanco y negro sigue distinguiendo los cuatro tipos.
 */
@Composable
fun TarjetaRegistro(entry: MealEntry, modifier: Modifier = Modifier) {
    val visual = visualDe(entry.kind)
    var fotoAbierta by remember(entry.id) { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(visual.contenedor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    iconoDe(entry.kind),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = visual.color,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${visual.glifo} ${stringResource(visual.etiqueta).uppercase(Locale.getDefault())}",
                        style = MaterialTheme.typography.labelSmall,
                        color = visual.color,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = Fechas.hora(entry.timestampMillis),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colorsCronicas.tenue,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = contenidoLegible(LocalContext.current, entry),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (!entry.photoPath.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { fotoAbierta = !fotoAbierta }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Image,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = visual.color,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(if (fotoAbierta) R.string.photo_hide else R.string.photo_show),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = visual.color,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            if (fotoAbierta) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = visual.color,
                        )
                    }
                    AnimatedVisibility(visible = fotoAbierta) {
                        PhotoThumb(
                            path = entry.photoPath,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .padding(top = 4.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }
        }
    }
}

/** Línea de glifos + cifras de un día: «● 4 comidas ▲ 30' ■ gym». */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ResumenEnLinea(
    resumen: com.marcm.cronicasapetito.data.ResumenDia,
    modifier: Modifier = Modifier,
    detallado: Boolean = false,
) {
    val piezas = buildList {
        if (resumen.comidas > 0) add(
            EntryKind.FOOD to "${resumen.comidas} ${if (resumen.comidas == 1) "comida" else "comidas"}"
        )
        if (resumen.minutosCaminados > 0) add(
            EntryKind.WALK to if (detallado) "${resumen.minutosCaminados} min" else "${resumen.minutosCaminados}′"
        )
        if (resumen.notasAnimo > 0) add(
            EntryKind.MOOD to "${resumen.notasAnimo} ${if (resumen.notasAnimo == 1) "nota" else "notas"}"
        )
        if (resumen.gimnasio == true) add(EntryKind.GYM to if (detallado) "gimnasio" else "gym")
    }

    if (piezas.isEmpty()) {
        Text(
            text = stringResource(R.string.summary_none),
            style = MaterialTheme.typography.bodyMedium,
            color = colorsCronicas.tenue,
            modifier = modifier,
        )
        return
    }

    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        piezas.forEach { (kind, texto) ->
            val visual = visualDe(kind)
            Text(
                text = "${visual.glifo} $texto",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = visual.color,
            )
        }
    }
}

/** Cifra grande de resumen con su etiqueta, en la rejilla de la vista Semana. */
@Composable
fun TarjetaCifra(
    valor: String,
    etiqueta: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = valor, style = estiloCifra, color = color)
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Navegación de periodo: ‹ título › */
@Composable
fun NavegadorPeriodo(
    titulo: String,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BotonNavegacion(
            icono = Icons.Filled.KeyboardArrowLeft,
            descripcion = stringResource(R.string.period_previous),
            onClick = onAnterior,
        )
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        BotonNavegacion(
            icono = Icons.Filled.KeyboardArrowRight,
            descripcion = stringResource(R.string.period_next),
            onClick = onSiguiente,
        )
    }
}

@Composable
private fun BotonNavegacion(
    icono: ImageVector,
    descripcion: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icono,
            contentDescription = descripcion,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Estado vacío con voz propia: no es lo mismo no tener nada que estar filtrando. */
@Composable
fun EstadoVacio(
    icono: ImageVector,
    mensaje: String,
    modifier: Modifier = Modifier,
    accion: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(52.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icono,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        accion?.invoke()
    }
}

/** Día natural de un registro, para agrupar sin repetir el cálculo. */
fun MealEntry.dia(): LocalDate = Periodos.fechaDe(timestampMillis)
