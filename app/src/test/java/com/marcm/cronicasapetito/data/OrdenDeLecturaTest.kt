package com.marcm.cronicasapetito.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Dentro de un día manda la hora a la que pasó, no el orden en que se apuntó:
 * el desayuno de las 8 va arriba aunque se anotara al final del día.
 */
class OrdenDeLecturaTest {

    private fun registro(id: Long, dia: LocalDate, hora: Int, minuto: Int = 0) = MealEntry(
        id = id,
        timestampMillis = dia.atTime(LocalTime.of(hora, minuto))
            .atZone(Periodos.zona()).toInstant().toEpochMilli(),
        content = "r$id",
    )

    @Test
    fun `dentro del dia va de la manana a la noche aunque se apuntara desordenado`() {
        val dia = LocalDate.of(2026, 9, 25)
        val cena = registro(1, dia, 23)
        val desayuno = registro(2, dia, 8)
        val media = registro(3, dia, 9, 30)

        val orden = listOf(cena, desayuno, media).enOrdenDeLectura()

        assertEquals(listOf(desayuno, media, cena), orden)
    }

    @Test
    fun `el dia mas reciente sigue arriba`() {
        val ayer = LocalDate.of(2026, 9, 24)
        val hoy = LocalDate.of(2026, 9, 25)
        val ayerNoche = registro(1, ayer, 23)
        val ayerManana = registro(2, ayer, 7)
        val hoyManana = registro(3, hoy, 8)
        val hoyTarde = registro(4, hoy, 17)

        val orden = listOf(ayerManana, hoyTarde, ayerNoche, hoyManana).enOrdenDeLectura()

        assertEquals(listOf(hoyManana, hoyTarde, ayerManana, ayerNoche), orden)
    }
}
