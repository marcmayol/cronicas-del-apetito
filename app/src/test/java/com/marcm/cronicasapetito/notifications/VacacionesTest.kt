package com.marcm.cronicasapetito.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Como con la rejilla: si las vacaciones se equivocan, el fallo sale a las
 * diez de la noche del tercer día fuera de casa.
 */
class VacacionesTest {

    private val zona = ZoneId.of("Europe/Madrid")

    private fun t(dia: Int, hora: Int, minuto: Int = 0, mes: Int = 8): Long =
        LocalDateTime.of(2026, mes, dia, hora, minuto).atZone(zona).toInstant().toEpochMilli()

    private fun texto(millis: Long): String {
        val d = java.time.Instant.ofEpochMilli(millis).atZone(zona)
        return "%d %02d:%02d".format(d.dayOfMonth, d.hour, d.minute)
    }

    private fun vac(desde: Int, hasta: Int, modo: Vacaciones.Modo, mes: Int = 8) = Vacaciones(
        desde = LocalDate.of(2026, mes, desde),
        hasta = LocalDate.of(2026, mes, hasta),
        modo = modo,
        horaMin = 22 * 60,
    )

    /** La ventana por defecto, de 8:00 a medianoche cada hora, en Madrid. */
    private fun rejilla(desde: Long): Long {
        var d = java.time.Instant.ofEpochMilli(desde).atZone(zona)
            .withMinute(0).withSecond(0).withNano(0).plusHours(1)
        while (d.hour in 1..7) d = d.plusHours(1)
        return d.toInstant().toEpochMilli()
    }

    private fun proximo(ahora: Long, v: Vacaciones?) =
        texto(PlanVacaciones.proximoDisparo(ahora, v, 8 * 60, zona, ::rejilla))

    @Test
    fun `sin vacaciones manda la rejilla de siempre`() {
        assertEquals("10 15:00", proximo(t(10, 14, 30), null))
    }

    @Test
    fun `antes de empezar todo sigue igual`() {
        val v = vac(12, 15, Vacaciones.Modo.UNA_VEZ)
        assertEquals("10 15:00", proximo(t(10, 14, 30), v))
    }

    @Test
    fun `la noche antes de empezar se para a medianoche`() {
        // Las vacaciones empiezan a las 0:00 del primer día: el último aviso es
        // el de las 23:00 y lo siguiente es la pregunta diaria.
        val v = vac(12, 15, Vacaciones.Modo.UNA_VEZ)
        assertEquals("11 23:00", proximo(t(11, 22, 30), v))
        assertEquals("12 22:00", proximo(t(11, 23, 30), v))
    }

    @Test
    fun `una vez al dia pregunta a la hora elegida`() {
        val v = vac(12, 15, Vacaciones.Modo.UNA_VEZ)
        assertEquals("13 22:00", proximo(t(13, 9, 0), v))
        assertEquals("14 22:00", proximo(t(13, 22, 0), v))
    }

    @Test
    fun `tras el ultimo dia vuelve el primer aviso normal`() {
        val v = vac(12, 15, Vacaciones.Modo.UNA_VEZ)
        // Ni el de las 23:00 ni el de las 0:00 del 15 (aún son del día 15).
        assertEquals("16 08:00", proximo(t(15, 22, 0), v))
    }

    @Test
    fun `sin preguntas salta directo a la vuelta`() {
        val v = vac(12, 15, Vacaciones.Modo.SIN_PREGUNTAS)
        assertEquals("16 08:00", proximo(t(12, 10, 0), v))
    }

    @Test
    fun `cruza el cambio de hora de octubre sin descuadrarse`() {
        // El 25 de octubre de 2026 la hora vuelve atrás en Madrid.
        val v = vac(24, 26, Vacaciones.Modo.UNA_VEZ, mes = 10)
        assertEquals("25 22:00", proximo(t(24, 22, 30, mes = 10), v))
    }

    @Test
    fun `la diaria no se repite el mismo dia`() {
        val v = vac(12, 15, Vacaciones.Modo.UNA_VEZ)
        val hoy = LocalDate.of(2026, 8, 13)
        assertEquals(
            AvisoComida.VACACIONES,
            PlanVacaciones.alSonar(t(13, 22, 0), v, 8 * 60, null, zona),
        )
        assertEquals(
            AvisoComida.NADA,
            PlanVacaciones.alSonar(t(13, 22, 5), v, 8 * 60, hoy, zona),
        )
    }

    @Test
    fun `una alarma vieja de la rejilla no suena en vacaciones`() {
        val v = vac(12, 15, Vacaciones.Modo.UNA_VEZ)
        assertEquals(
            AvisoComida.NADA,
            PlanVacaciones.alSonar(t(13, 15, 0), v, 8 * 60, null, zona),
        )
        assertEquals(
            AvisoComida.NADA,
            PlanVacaciones.alSonar(t(13, 22, 0), vac(12, 15, Vacaciones.Modo.SIN_PREGUNTAS), 8 * 60, null, zona),
        )
    }

    @Test
    fun `fuera de las vacaciones suena el aviso normal`() {
        val v = vac(12, 15, Vacaciones.Modo.UNA_VEZ)
        assertEquals(
            AvisoComida.NORMAL,
            PlanVacaciones.alSonar(t(16, 8, 0), v, 8 * 60, null, zona),
        )
    }

    @Test
    fun `pasada la ultima hora no queda diaria`() {
        val v = vac(12, 15, Vacaciones.Modo.UNA_VEZ)
        assertNull(PlanVacaciones.proximoDiario(t(15, 22, 0), v, zona))
    }
}
