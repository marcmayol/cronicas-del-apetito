package com.marcm.cronicasapetito.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marcm.cronicasapetito.data.EntryKind
import com.marcm.cronicasapetito.data.Periodos
import com.marcm.cronicasapetito.data.ResumenDia
import java.time.LocalDate

// ---------------------------------------------------------------------------
// Vista Día — la lista de siempre, con el sistema nuevo encima
// ---------------------------------------------------------------------------

@Composable
fun VistaDia(
    estado: EstadoPrincipal,
    modifier: Modifier = Modifier,
) {
    val porDia = remember(estado.entradasVisibles) {
        estado.entradasVisibles.groupBy { it.dia() }.toList().sortedByDescending { it.first }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        porDia.forEach { (dia, registros) ->
            item(key = "dia-$dia") {
                CabeceraDia(dia, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
            }
            items(registros, key = { it.id }) { entry ->
                TarjetaRegistro(entry)
            }
        }
        if (porDia.isNotEmpty() && estado.entradasVisibles.size <= 3) {
            item(key = "cierre") {
                Text(
                    text = "Eso es todo por hoy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorsCronicas.tenue,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Vista Semana — los siete días de un vistazo
// ---------------------------------------------------------------------------

@Composable
fun VistaSemana(
    estado: EstadoPrincipal,
    onAbrirDia: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resumen = estado.resumenPeriodo

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TarjetaCifra(
                valor = resumen.comidas.toString(),
                etiqueta = "comidas",
                color = visualDe(EntryKind.FOOD).color,
                modifier = Modifier.weight(1f),
            )
            // Etiquetas cortas a propósito: con el zoom de fuente al 150% una
            // palabra larga se parte por la mitad dentro de la tarjeta.
            TarjetaCifra(
                valor = "${resumen.minutosCaminados}′",
                etiqueta = "min",
                color = visualDe(EntryKind.WALK).color,
                modifier = Modifier.weight(1f),
            )
            TarjetaCifra(
                valor = resumen.notasAnimo.toString(),
                etiqueta = "notas",
                color = visualDe(EntryKind.MOOD).color,
                modifier = Modifier.weight(1f),
            )
            TarjetaCifra(
                valor = resumen.diasGimnasio.toString(),
                etiqueta = "gimnasio",
                color = visualDe(EntryKind.GYM).color,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(2.dp))

        estado.semana.forEach { dia ->
            val fuera = estado.filtro?.let { dia !in it } ?: false
            FilaDia(
                dia = dia,
                resumen = estado.resumenPorDia[dia] ?: ResumenDia(dia),
                atenuada = fuera,
                onClick = { if (!fuera) onAbrirDia(dia) },
            )
        }

        if (estado.filtro != null) {
            NotaFueraDeRango(
                "Los días fuera del filtro se ven atenuados y no cuentan en el resumen " +
                    "ni en lo que se comparte."
            )
        }
    }
}

@Composable
private fun FilaDia(
    dia: LocalDate,
    resumen: ResumenDia,
    atenuada: Boolean,
    onClick: () -> Unit,
) {
    val alfa = if (atenuada) 0.38f else 1f
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !atenuada, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (atenuada) 0.5f else 1f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Sin ancho fijo y en una sola línea: con el zoom al 150% un ancho
            // en dp partía el número del día en dos («M 1 / 8»).
            Text(
                text = "${inicialDia(dia)} ${dia.dayOfMonth}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alfa),
                maxLines = 1,
                modifier = Modifier.widthIn(min = 44.dp),
            )
            Spacer(Modifier.width(10.dp))
            ResumenEnLinea(
                resumen = resumen,
                modifier = Modifier.weight(1f),
            )
            if (!resumen.vacio && !atenuada) {
                Icon(
                    Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Vista Mes — el calendario, con glifos de forma propia por tipo
// ---------------------------------------------------------------------------

@Composable
fun VistaMes(
    estado: EstadoPrincipal,
    onSeleccionarDia: (LocalDate) -> Unit,
    onAbrirDia: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val casillas = remember(estado.mes) { Periodos.casillasDe(estado.mes) }
    val hoy = LocalDate.now()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp)
            .padding(bottom = 96.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            listOf("L", "M", "X", "J", "V", "S", "D").forEach { inicial ->
                Text(
                    text = inicial,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorsCronicas.tenue,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        casillas.chunked(7).forEach { semana ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                semana.forEach { dia ->
                    if (dia == null) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        CasillaDia(
                            dia = dia,
                            resumen = estado.resumenPorDia[dia],
                            esHoy = dia == hoy,
                            futuro = dia.isAfter(hoy),
                            fueraDeRango = estado.filtro?.let { dia !in it } ?: false,
                            seleccionado = dia == estado.diaSeleccionado,
                            onClick = { onSeleccionarDia(dia) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                repeat(7 - semana.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(3.dp))
        }

        val seleccionado = estado.diaSeleccionado
        if (seleccionado != null) {
            Spacer(Modifier.height(8.dp))
            TarjetaDiaSeleccionado(
                dia = seleccionado,
                resumen = estado.resumenPorDia[seleccionado] ?: ResumenDia(seleccionado),
                onAbrir = { onAbrirDia(seleccionado) },
            )
        }

        if (estado.filtro != null) {
            Spacer(Modifier.height(8.dp))
            NotaFueraDeRango(
                "Fuera del rango: los días atenuados conservan sus marcas, pero no cuentan " +
                    "en el resumen ni en lo que se comparte."
            )
        }
    }
}

@Composable
private fun CasillaDia(
    dia: LocalDate,
    resumen: ResumenDia?,
    esHoy: Boolean,
    futuro: Boolean,
    fueraDeRango: Boolean,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alfa = if (fueraDeRango) 0.38f else 1f
    val colorBordeFuturo = MaterialTheme.colorScheme.outlineVariant
    val fondo = when {
        fueraDeRango -> Color.Transparent
        seleccionado -> MaterialTheme.colorScheme.secondaryContainer
        esHoy -> MaterialTheme.colorScheme.secondaryContainer
        futuro -> MaterialTheme.colorScheme.background
        else -> MaterialTheme.colorScheme.surface
    }
    val borde = when {
        fueraDeRango -> Color.Transparent
        esHoy || seleccionado -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    // Los días que aún no han llegado van con borde discontinuo: se ve que la
    // casilla existe pero que todavía no hay nada que contar. Nunca en rojo.
    val bordeModifier = if (futuro && !esHoy && !fueraDeRango) {
        Modifier.drawBehind {
            drawRoundRect(
                color = colorBordeFuturo,
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                ),
                cornerRadius = CornerRadius(9.dp.toPx()),
            )
        }
    } else {
        Modifier.border(
            if (esHoy || seleccionado) 1.6.dp else 1.dp,
            borde,
            RoundedCornerShape(9.dp)
        )
    }

    Column(
        modifier = modifier
            .heightIn(min = 46.dp)
            .background(fondo, RoundedCornerShape(9.dp))
            .then(bordeModifier)
            .clickable(enabled = !fueraDeRango, onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 4.dp),
    ) {
        Text(
            text = dia.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (esHoy) FontWeight.Bold else FontWeight.Medium,
            color = when {
                esHoy -> MaterialTheme.colorScheme.onSecondaryContainer
                futuro -> colorsCronicas.tenue
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }.copy(alpha = alfa),
        )
        val tipos = resumen?.tiposPresentes.orEmpty()
        if (tipos.isNotEmpty()) {
            Spacer(Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                tipos.forEach { kind ->
                    val visual = visualDe(kind)
                    Text(
                        text = visual.glifo,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = visual.color.copy(alpha = alfa),
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaDiaSeleccionado(
    dia: LocalDate,
    resumen: ResumenDia,
    onAbrir: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Fechas.dia(dia),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (!resumen.vacio) {
                    TextButton(onClick = onAbrir, contentPadding = PaddingValues(4.dp)) {
                        Text(
                            text = "Ver día completo ›",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            ResumenEnLinea(resumen = resumen, detallado = true)
        }
    }
}

@Composable
private fun NotaFueraDeRango(texto: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
        )
    }
}

/** Iconos de los dos estados vacíos: cuaderno en blanco y calendario filtrado. */
val IconoCuaderno = Icons.Filled.MenuBook
val IconoCalendario = Icons.Filled.CalendarMonth
