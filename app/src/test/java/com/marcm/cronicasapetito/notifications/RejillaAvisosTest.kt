package com.marcm.cronicasapetito.notifications

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

/**
 * La rejilla de avisos es lo único de la configuración que no se puede
 * comprobar mirando la pantalla: si se equivoca, el fallo aparece mañana a
 * las tres de la tarde. De ahí los tests.
 */
class RejillaAvisosTest {

    private fun instante(dia: Int, hora: Int, minuto: Int = 0): Long =
        Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, dia, hora, minuto, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun horaDe(millis: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return "%d %02d:%02d".format(
            c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)
        )
    }

    /** Ventana por defecto: 8:00 a medianoche, cada hora — lo de siempre. */
    private fun porDefecto(ahora: Long): Long = RejillaAvisos.proximoDisparo(
        ahora = ahora, inicioMin = 8 * 60, duracionMin = 16 * 60, cadaMin = 60
    )

    @Test
    fun `dentro de la ventana toca la siguiente hora en punto`() {
        assertEquals("25 15:00", horaDe(porDefecto(instante(25, 14, 30))))
    }

    @Test
    fun `justo en un aviso salta al siguiente, no se repite`() {
        assertEquals("25 15:00", horaDe(porDefecto(instante(25, 14, 0))))
    }

    @Test
    fun `de madrugada espera a que abra la ventana`() {
        assertEquals("25 08:00", horaDe(porDefecto(instante(25, 3, 20))))
    }

    @Test
    fun `el ultimo aviso del dia es la medianoche siguiente`() {
        assertEquals("26 00:00", horaDe(porDefecto(instante(25, 23, 30))))
    }

    @Test
    fun `pasada la medianoche vuelve a la apertura de hoy`() {
        assertEquals("26 08:00", horaDe(porDefecto(instante(26, 0, 10))))
    }

    @Test
    fun `una frecuencia de tres horas solo para en su rejilla`() {
        val trasComer = RejillaAvisos.proximoDisparo(
            ahora = instante(25, 12, 15), inicioMin = 8 * 60, duracionMin = 12 * 60, cadaMin = 180
        )
        assertEquals("25 14:00", horaDe(trasComer))
    }

    @Test
    fun `media hora entre avisos parte las horas por la mitad`() {
        val siguiente = RejillaAvisos.proximoDisparo(
            ahora = instante(25, 9, 5), inicioMin = 8 * 60, duracionMin = 12 * 60, cadaMin = 30
        )
        assertEquals("25 09:30", horaDe(siguiente))
    }

    @Test
    fun `una ventana nocturna cruza la medianoche sin cortarse`() {
        // De 22:00 a 4:00: a las 2 de la madrugada aún quedan avisos de anoche.
        val ventanaNocturna = RejillaAvisos.proximoDisparo(
            ahora = instante(26, 2, 10), inicioMin = 22 * 60, duracionMin = 6 * 60, cadaMin = 60
        )
        assertEquals("26 03:00", horaDe(ventanaNocturna))
    }

    @Test
    fun `fuera de una ventana nocturna espera a la apertura de la noche`() {
        val ventanaNocturna = RejillaAvisos.proximoDisparo(
            ahora = instante(26, 11, 0), inicioMin = 22 * 60, duracionMin = 6 * 60, cadaMin = 60
        )
        assertEquals("26 22:00", horaDe(ventanaNocturna))
    }

    @Test
    fun `sin duracion solo queda la apertura del dia siguiente`() {
        // Es lo que usa el modo a dormir para saber cuándo volver.
        val despertar = RejillaAvisos.proximoDisparo(
            ahora = instante(25, 23, 40), inicioMin = 8 * 60, duracionMin = 0, cadaMin = 60
        )
        assertEquals("26 08:00", horaDe(despertar))
    }

    @Test
    fun `las horas se escriben siempre con dos cifras`() {
        assertEquals("08:00", horaTexto(8 * 60))
        assertEquals("00:00", horaTexto(0))
        assertEquals("22:30", horaTexto(22 * 60 + 30))
    }

    @Test
    fun `la frecuencia se lee en horas y minutos`() {
        assertEquals("cada 30 min", frecuenciaTexto(30))
        assertEquals("cada 1 h", frecuenciaTexto(60))
        assertEquals("cada 1 h 30 min", frecuenciaTexto(90))
        assertEquals("cada 4 h", frecuenciaTexto(240))
    }
}
