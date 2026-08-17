package com.marcm.cronicasapetito.data

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Utilidades de calendario para las vistas Día / Semana / Mes y el filtro por
 * rango. Todo en [java.time] (disponible desde API 26, que es nuestro mínimo):
 * `Calendar` es demasiado fácil de equivocar en los bordes de mes y de semana.
 *
 * Convenio de la app: la semana empieza en **lunes**, igual que ya hacía el
 * recuento de gimnasio.
 */
object Periodos {

    fun zona(): ZoneId = ZoneId.systemDefault()

    fun fechaDe(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zona()).toLocalDate()

    /** Primer milisegundo del día. */
    fun inicioDe(dia: LocalDate): Long =
        dia.atStartOfDay(zona()).toInstant().toEpochMilli()

    /** Último milisegundo del día. */
    fun finDe(dia: LocalDate): Long =
        dia.plusDays(1).atStartOfDay(zona()).toInstant().toEpochMilli() - 1

    fun lunesDe(dia: LocalDate): LocalDate = dia.minusDays((dia.dayOfWeek.value - 1).toLong())

    fun domingoDe(dia: LocalDate): LocalDate = lunesDe(dia).plusDays(6)

    /** Los siete días de la semana de [dia], de lunes a domingo. */
    fun semanaDe(dia: LocalDate): List<LocalDate> {
        val lunes = lunesDe(dia)
        return (0L..6L).map { lunes.plusDays(it) }
    }

    /**
     * Las casillas del mes de [mes] tal y como se pintan: huecos delante hasta
     * cuadrar con el lunes, luego los días del mes. Los huecos son `null`.
     */
    fun casillasDe(mes: YearMonth): List<LocalDate?> {
        val primero = mes.atDay(1)
        val huecos = primero.dayOfWeek.value - 1
        return List(huecos) { null } + (1..mes.lengthOfMonth()).map { mes.atDay(it) }
    }
}

/**
 * Un intervalo de días cerrado por ambos extremos, en días naturales. Es lo que
 * el usuario elige en el filtro y lo que viaja con lo que se comparte.
 */
data class RangoFechas(val desde: LocalDate, val hasta: LocalDate) {

    /** Normaliza un rango al revés en vez de rechazarlo: nunca un botón muerto. */
    companion object {
        fun de(a: LocalDate, b: LocalDate): RangoFechas =
            if (a.isAfter(b)) RangoFechas(b, a) else RangoFechas(a, b)

        fun ultimosDias(n: Long, hoy: LocalDate = LocalDate.now()): RangoFechas =
            RangoFechas(hoy.minusDays(n - 1), hoy)

        fun esteMes(hoy: LocalDate = LocalDate.now()): RangoFechas =
            RangoFechas(hoy.withDayOfMonth(1), hoy)
    }

    val inicioMillis: Long get() = Periodos.inicioDe(desde)
    val finMillis: Long get() = Periodos.finDe(hasta)

    operator fun contains(dia: LocalDate): Boolean = !dia.isBefore(desde) && !dia.isAfter(hasta)
}
