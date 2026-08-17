package com.marcm.cronicasapetito.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Los agregados que alimentan Semana, Mes y lo que se comparte. */
class ResumenesTest {

    private fun instante(dia: LocalDate, hora: Int, minuto: Int = 0): Long =
        LocalDateTime.of(dia, java.time.LocalTime.of(hora, minuto))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun comida(dia: LocalDate, hora: Int, texto: String = "Algo") =
        MealEntry(timestampMillis = instante(dia, hora), content = texto, kind = EntryKind.FOOD)

    private fun caminata(dia: LocalDate, hora: Int, minutos: Int) = MealEntry(
        timestampMillis = instante(dia, hora),
        content = "$minutos min",
        kind = EntryKind.WALK,
        minutes = minutos,
    )

    private fun animo(dia: LocalDate, hora: Int) =
        MealEntry(timestampMillis = instante(dia, hora), content = "Nervioso", kind = EntryKind.MOOD)

    private fun gimnasio(dia: LocalDate, fue: Boolean) = MealEntry(
        timestampMillis = instante(dia, 22),
        content = if (fue) "Sí" else "No",
        kind = EntryKind.GYM,
    )

    private val lunes = LocalDate.of(2026, 8, 10)

    @Test
    fun `cuenta comidas notas y suma minutos por dia`() {
        val resumen = Resumenes.porDia(
            listOf(
                comida(lunes, 9), comida(lunes, 14), comida(lunes, 21),
                caminata(lunes, 18, 30),
                caminata(lunes, 20, 15),
                animo(lunes, 22),
            )
        )[lunes]!!

        assertEquals(3, resumen.comidas)
        assertEquals(45, resumen.minutosCaminados)
        assertEquals(1, resumen.notasAnimo)
        assertNull(resumen.gimnasio)
    }

    @Test
    fun `un No de gimnasio es dia respondido, no dia sin dato`() {
        val resumen = Resumenes.porDia(listOf(gimnasio(lunes, false)))[lunes]!!
        assertEquals(false, resumen.gimnasio)
        assertFalse(resumen.vacio)
        assertTrue(resumen.tiposPresentes.isEmpty())
    }

    @Test
    fun `un Si de gimnasio aparece entre los tipos del dia`() {
        val resumen = Resumenes.porDia(listOf(gimnasio(lunes, true)))[lunes]!!
        assertEquals(true, resumen.gimnasio)
        assertEquals(listOf(EntryKind.GYM), resumen.tiposPresentes)
    }

    @Test
    fun `los tipos presentes salen siempre en el mismo orden`() {
        val resumen = Resumenes.porDia(
            listOf(gimnasio(lunes, true), animo(lunes, 12), caminata(lunes, 18, 20), comida(lunes, 9))
        )[lunes]!!

        assertEquals(
            listOf(EntryKind.FOOD, EntryKind.WALK, EntryKind.MOOD, EntryKind.GYM),
            resumen.tiposPresentes,
        )
    }

    @Test
    fun `registros de dias distintos no se mezclan`() {
        val martes = lunes.plusDays(1)
        val porDia = Resumenes.porDia(listOf(comida(lunes, 9), comida(martes, 9), comida(martes, 14)))

        assertEquals(1, porDia[lunes]!!.comidas)
        assertEquals(2, porDia[martes]!!.comidas)
    }

    @Test
    fun `un dia sin registros no aparece en el mapa y cuenta como vacio`() {
        val porDia = Resumenes.porDia(listOf(comida(lunes, 9)))
        val jueves = lunes.plusDays(3)

        assertNull(porDia[jueves])
        assertTrue(ResumenDia(jueves).vacio)
    }

    @Test
    fun `el resumen del periodo solo suma los dias que se le pasan`() {
        val martes = lunes.plusDays(1)
        val porDia = Resumenes.porDia(
            listOf(
                comida(lunes, 9), caminata(lunes, 18, 30), gimnasio(lunes, true),
                comida(martes, 9), comida(martes, 14), caminata(martes, 19, 45),
            )
        )

        val soloLunes = Resumenes.delPeriodo(porDia, listOf(lunes))
        assertEquals(1, soloLunes.comidas)
        assertEquals(30, soloLunes.minutosCaminados)
        assertEquals(1, soloLunes.diasGimnasio)

        val ambos = Resumenes.delPeriodo(porDia, listOf(lunes, martes))
        assertEquals(3, ambos.comidas)
        assertEquals(75, ambos.minutosCaminados)
        assertEquals(1, ambos.diasGimnasio)
    }

    @Test
    fun `el filtro recorta el periodo — es la combinacion vista mas rango`() {
        val dias = Periodos.semanaDe(lunes)
        val porDia = Resumenes.porDia(dias.map { comida(it, 13) })
        val rango = RangoFechas(lunes, lunes.plusDays(2))

        val dentro = Resumenes.delPeriodo(porDia, dias.filter { it in rango })

        assertEquals(3, dentro.comidas)
        assertEquals(7, Resumenes.delPeriodo(porDia, dias).comidas)
    }

    @Test
    fun `una caminata sin minutos no rompe la suma`() {
        val sinMinutos = MealEntry(
            timestampMillis = instante(lunes, 18),
            content = "paseo",
            kind = EntryKind.WALK,
            minutes = null,
        )
        val resumen = Resumenes.porDia(listOf(sinMinutos, caminata(lunes, 19, 20)))[lunes]!!
        assertEquals(20, resumen.minutosCaminados)
    }
}
