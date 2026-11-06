package com.marcm.cronicasapetito.notifications

import android.content.Context
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Unos días marcados en los que la app deja de insistir: o no pregunta por la
 * comida, o pregunta una sola vez al día a la hora elegida. El gimnasio calla
 * en los dos casos. Al acabar el último día, todo vuelve a ser como antes sin
 * tocar nada.
 */
data class Vacaciones(
    val desde: LocalDate,
    val hasta: LocalDate,
    val modo: Modo,
    val horaMin: Int,
) {
    enum class Modo { SIN_PREGUNTAS, UNA_VEZ }

    fun cubre(dia: LocalDate): Boolean = !dia.isBefore(desde) && !dia.isAfter(hasta)
}

/** Qué hacer cuando suena la alarma de comida. */
enum class AvisoComida { NORMAL, VACACIONES, NADA }

/**
 * Las cuentas de las vacaciones, sin Android de por medio para poder probarlas
 * con tests normales (igual que [RejillaAvisos]).
 */
object PlanVacaciones {

    private fun dia(millis: Long, zona: ZoneId): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zona).toLocalDate()

    /**
     * Día al que pertenece un aviso de la ventana normal: el de su inicio. Con
     * la ventana de 8:00 a medianoche, el aviso de las 0:00 del día 11 es aún
     * del día 10.
     */
    private fun diaDeVentana(millis: Long, inicioMin: Int, zona: ZoneId): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zona).minusMinutes(inicioMin.toLong()).toLocalDate()

    /** Si un aviso normal caería dentro de las vacaciones y hay que callarlo. */
    fun silencia(v: Vacaciones, millis: Long, inicioMin: Int, zona: ZoneId): Boolean =
        v.cubre(dia(millis, zona)) || v.cubre(diaDeVentana(millis, inicioMin, zona))

    private fun instante(dia: LocalDate, minutos: Int, zona: ZoneId): Long =
        dia.atTime(LocalTime.of(minutos / 60, minutos % 60)).atZone(zona).toInstant().toEpochMilli()

    /** Próxima pregunta diaria de vacaciones, o null si ya no queda ninguna. */
    fun proximoDiario(ahora: Long, v: Vacaciones, zona: ZoneId): Long? {
        if (v.modo != Vacaciones.Modo.UNA_VEZ) return null
        var d = maxOf(v.desde, dia(ahora, zona))
        while (!d.isAfter(v.hasta)) {
            val t = instante(d, v.horaMin, zona)
            if (t > ahora) return t
            d = d.plusDays(1)
        }
        return null
    }

    /**
     * Próxima alarma de comida. [normal] es la rejilla de siempre (el primer
     * aviso posterior al instante que recibe). Lo que caiga en vacaciones se
     * salta hasta el primer aviso del día siguiente al último, y si se
     * pregunta una vez al día, gana la pregunta diaria cuando llega antes.
     */
    fun proximoDisparo(
        ahora: Long,
        v: Vacaciones?,
        inicioMin: Int,
        zona: ZoneId,
        normal: (Long) -> Long,
    ): Long {
        var t = normal(ahora)
        if (v == null) return t
        if (silencia(v, t, inicioMin, zona)) {
            val vuelta = instante(v.hasta.plusDays(1), inicioMin, zona) - 1
            t = normal(maxOf(vuelta, ahora))
        }
        val diario = proximoDiario(ahora, v, zona)
        return if (diario != null && diario < t) diario else t
    }

    /**
     * Qué enseña la alarma al sonar. Se decide aquí y no se da por buena la
     * alarma: puede venir programada de antes de activar las vacaciones.
     * La pregunta diaria sale como mucho una vez por día ([ultimoDiario]).
     */
    fun alSonar(
        ahora: Long,
        v: Vacaciones?,
        inicioMin: Int,
        ultimoDiario: LocalDate?,
        zona: ZoneId,
    ): AvisoComida {
        if (v == null || !silencia(v, ahora, inicioMin, zona)) return AvisoComida.NORMAL
        if (v.modo != Vacaciones.Modo.UNA_VEZ) return AvisoComida.NADA
        val hoy = dia(ahora, zona)
        if (!v.cubre(hoy) || ultimoDiario == hoy) return AvisoComida.NADA
        // Un minuto de margen por si el reloj del sistema va justo.
        return if (ahora >= instante(hoy, v.horaMin, zona) - 60_000) AvisoComida.VACACIONES
        else AvisoComida.NADA
    }
}

/** Dónde se guardan las vacaciones. Solo hay unas a la vez. */
object VacacionesPrefs {

    private const val PREFS = "vacaciones_prefs"
    private const val K_DESDE = "desde_dia"
    private const val K_HASTA = "hasta_dia"
    private const val K_MODO = "modo"
    private const val K_HORA = "hora_min"
    private const val K_ULTIMO_DIARIO = "ultimo_diario_dia"

    /** 22:00: al final del día, para preguntar por todo de una vez. */
    const val HORA_POR_DEFECTO = 22 * 60
    /** Una semana contando hoy. */
    const val DIAS_POR_DEFECTO = 7L

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Las vacaciones guardadas, aunque ya hayan pasado. */
    fun guardadas(context: Context): Vacaciones? {
        val p = prefs(context)
        if (!p.contains(K_DESDE) || !p.contains(K_HASTA)) return null
        val modo = runCatching { Vacaciones.Modo.valueOf(p.getString(K_MODO, null)!!) }
            .getOrDefault(Vacaciones.Modo.UNA_VEZ)
        return Vacaciones(
            desde = LocalDate.ofEpochDay(p.getLong(K_DESDE, 0)),
            hasta = LocalDate.ofEpochDay(p.getLong(K_HASTA, 0)),
            modo = modo,
            horaMin = p.getInt(K_HORA, HORA_POR_DEFECTO),
        )
    }

    /** Las que aún cuentan: en curso o por venir. Las ya pasadas no existen. */
    fun vigentes(context: Context): Vacaciones? =
        guardadas(context)?.takeIf { !it.hasta.isBefore(LocalDate.now()) }

    /** Si hoy es día de vacaciones. */
    fun deVacacionesHoy(context: Context): Boolean =
        vigentes(context)?.cubre(LocalDate.now()) == true

    fun guardar(context: Context, v: Vacaciones) {
        val hasta = if (v.hasta.isBefore(v.desde)) v.desde else v.hasta
        prefs(context).edit()
            .putLong(K_DESDE, v.desde.toEpochDay())
            .putLong(K_HASTA, hasta.toEpochDay())
            .putString(K_MODO, v.modo.name)
            .putInt(K_HORA, v.horaMin.coerceIn(0, 24 * 60 - 1))
            .apply()
    }

    fun borrar(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun ultimoDiario(context: Context): LocalDate? {
        val p = prefs(context)
        return if (p.contains(K_ULTIMO_DIARIO)) LocalDate.ofEpochDay(p.getLong(K_ULTIMO_DIARIO, 0))
        else null
    }

    fun setUltimoDiario(context: Context, dia: LocalDate) {
        prefs(context).edit().putLong(K_ULTIMO_DIARIO, dia.toEpochDay()).apply()
    }
}

/**
 * Activa, cambia o quita las vacaciones y reprograma las dos alarmas en el
 * acto, como el resto de Ajustes.
 */
object ModoVacaciones {

    fun guardar(context: Context, v: Vacaciones) {
        VacacionesPrefs.guardar(context, v)
        reprogramar(context)
    }

    fun terminar(context: Context) {
        VacacionesPrefs.borrar(context)
        reprogramar(context)
    }

    private fun reprogramar(context: Context) {
        MealAlarmScheduler.scheduleNext(context)
        GymAlarmScheduler.scheduleNext(context)
    }
}
