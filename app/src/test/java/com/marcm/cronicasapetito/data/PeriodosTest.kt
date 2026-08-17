package com.marcm.cronicasapetito.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * Los bordes del calendario: es donde se rompen las vistas Semana y Mes, y son
 * baratos de cubrir aquí en vez de descubrirlos en el móvil.
 */
class PeriodosTest {

    @Test
    fun `la semana empieza en lunes`() {
        val miercoles = LocalDate.of(2026, 8, 12)
        assertEquals(LocalDate.of(2026, 8, 10), Periodos.lunesDe(miercoles))
        assertEquals(LocalDate.of(2026, 8, 16), Periodos.domingoDe(miercoles))
    }

    @Test
    fun `el domingo pertenece a la semana que empezo el lunes anterior`() {
        val domingo = LocalDate.of(2026, 8, 16)
        assertEquals(LocalDate.of(2026, 8, 10), Periodos.lunesDe(domingo))
    }

    @Test
    fun `una semana a caballo entre dos meses tiene siete dias`() {
        val semana = Periodos.semanaDe(LocalDate.of(2026, 7, 30))
        assertEquals(7, semana.size)
        assertEquals(LocalDate.of(2026, 7, 27), semana.first())
        assertEquals(LocalDate.of(2026, 8, 2), semana.last())
    }

    @Test
    fun `las casillas del mes llevan delante los huecos hasta el lunes`() {
        // Agosto de 2026 empieza en sábado: cinco huecos delante.
        val casillas = Periodos.casillasDe(YearMonth.of(2026, 8))
        assertEquals(5 + 31, casillas.size)
        assertTrue(casillas.take(5).all { it == null })
        assertEquals(LocalDate.of(2026, 8, 1), casillas[5])
    }

    @Test
    fun `un mes que empieza en lunes no lleva huecos`() {
        val casillas = Periodos.casillasDe(YearMonth.of(2026, 6))
        assertEquals(30, casillas.size)
        assertEquals(LocalDate.of(2026, 6, 1), casillas.first())
    }

    @Test
    fun `febrero bisiesto tiene veintinueve dias`() {
        val casillas = Periodos.casillasDe(YearMonth.of(2028, 2)).filterNotNull()
        assertEquals(29, casillas.size)
    }

    @Test
    fun `el inicio y el fin de un dia cubren el dia entero sin solaparse`() {
        val dia = LocalDate.of(2026, 8, 17)
        assertEquals(Periodos.inicioDe(dia.plusDays(1)), Periodos.finDe(dia) + 1)
    }

    @Test
    fun `un rango al reves se ordena solo`() {
        val rango = RangoFechas.de(LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 1))
        assertEquals(LocalDate.of(2026, 8, 1), rango.desde)
        assertEquals(LocalDate.of(2026, 8, 15), rango.hasta)
    }

    @Test
    fun `el rango incluye sus dos extremos`() {
        val rango = RangoFechas(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 15))
        assertTrue(LocalDate.of(2026, 8, 1) in rango)
        assertTrue(LocalDate.of(2026, 8, 15) in rango)
        assertTrue(LocalDate.of(2026, 8, 8) in rango)
        assertFalse(LocalDate.of(2026, 7, 31) in rango)
        assertFalse(LocalDate.of(2026, 8, 16) in rango)
    }

    @Test
    fun `un rango de un solo dia es valido`() {
        val hoy = LocalDate.of(2026, 8, 17)
        val rango = RangoFechas(hoy, hoy)
        assertTrue(hoy in rango)
        assertTrue(rango.finMillis > rango.inicioMillis)
    }

    @Test
    fun `ultimos siete dias incluye hoy`() {
        val hoy = LocalDate.of(2026, 8, 17)
        val rango = RangoFechas.ultimosDias(7, hoy)
        assertEquals(LocalDate.of(2026, 8, 11), rango.desde)
        assertEquals(hoy, rango.hasta)
        assertTrue(hoy in rango)
    }

    @Test
    fun `este mes va del dia uno a hoy`() {
        val rango = RangoFechas.esteMes(LocalDate.of(2026, 8, 17))
        assertEquals(LocalDate.of(2026, 8, 1), rango.desde)
        assertEquals(LocalDate.of(2026, 8, 17), rango.hasta)
    }
}
