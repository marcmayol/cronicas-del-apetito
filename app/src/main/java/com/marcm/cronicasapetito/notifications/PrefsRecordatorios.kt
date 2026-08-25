package com.marcm.cronicasapetito.notifications

import android.content.Context
import java.util.Calendar

/**
 * Todo lo que antes estaba clavado en el código: a qué hora empiezan y acaban
 * los recordatorios, cada cuánto llegan, si se pregunta por el gimnasio y con
 * qué objetivo semanal, y si la caminata asoma en el aviso de comida.
 *
 * Los valores por defecto son exactamente el comportamiento de la v2.0.1, para
 * que quien ya venía usando la app no note ningún cambio al actualizar.
 */
object PrefsRecordatorios {

    private const val PREFS = "recordatorios_prefs"

    private const val K_COMIDA_ACTIVA = "comida_activa"
    private const val K_COMIDA_INICIO = "comida_inicio_min"
    private const val K_COMIDA_FIN = "comida_fin_min"
    private const val K_COMIDA_CADA = "comida_cada_min"
    private const val K_CAMINATA_EN_AVISO = "caminata_en_aviso"
    private const val K_GYM_ACTIVO = "gym_activo"
    private const val K_GYM_HORA = "gym_hora_min"
    private const val K_GYM_DIAS = "gym_dias"
    private const val K_GYM_OBJETIVO = "gym_objetivo_semanal"
    private const val K_ANIMO_ACTIVO = "animo_activo"
    private const val K_BIENVENIDA_VISTA = "bienvenida_vista"

    /** 8:00, en minutos desde medianoche. */
    const val COMIDA_INICIO_POR_DEFECTO = 8 * 60
    /** 0:00 del día siguiente: la ventana cierra a medianoche. */
    const val COMIDA_FIN_POR_DEFECTO = 0
    const val COMIDA_CADA_POR_DEFECTO = 60
    /** 22:00. */
    const val GYM_HORA_POR_DEFECTO = 22 * 60
    const val GYM_OBJETIVO_POR_DEFECTO = 2
    /** Sin tope: pregunta todos los días elegidos, hayas ido las veces que hayas ido. */
    const val GYM_SIN_OBJETIVO = 0

    /** Frecuencias ofrecidas en Ajustes, en minutos. */
    val FRECUENCIAS = listOf(30, 60, 90, 120, 180, 240, 360)

    /** Lunes a viernes, en las constantes de [Calendar.DAY_OF_WEEK]. */
    val GYM_DIAS_POR_DEFECTO: Set<Int> = setOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY,
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // -----------------------------------------------------------------------
    // Comida
    // -----------------------------------------------------------------------

    fun comidaActiva(context: Context): Boolean =
        prefs(context).getBoolean(K_COMIDA_ACTIVA, true)

    fun setComidaActiva(context: Context, activa: Boolean) {
        prefs(context).edit().putBoolean(K_COMIDA_ACTIVA, activa).apply()
    }

    fun comidaInicioMin(context: Context): Int =
        prefs(context).getInt(K_COMIDA_INICIO, COMIDA_INICIO_POR_DEFECTO)

    fun comidaFinMin(context: Context): Int =
        prefs(context).getInt(K_COMIDA_FIN, COMIDA_FIN_POR_DEFECTO)

    fun setComidaVentana(context: Context, inicioMin: Int, finMin: Int) {
        prefs(context).edit()
            .putInt(K_COMIDA_INICIO, inicioMin.coerceIn(0, 24 * 60 - 1))
            .putInt(K_COMIDA_FIN, finMin.coerceIn(0, 24 * 60 - 1))
            .apply()
    }

    fun comidaCadaMin(context: Context): Int =
        prefs(context).getInt(K_COMIDA_CADA, COMIDA_CADA_POR_DEFECTO)

    fun setComidaCadaMin(context: Context, cadaMin: Int) {
        prefs(context).edit().putInt(K_COMIDA_CADA, cadaMin.coerceAtLeast(15)).apply()
    }

    /**
     * Cuánto dura la ventana de avisos, en minutos. Si el fin cae en la misma
     * hora que el inicio o antes, se entiende que cruza la medianoche; si son
     * exactamente iguales, es el día entero.
     */
    fun comidaDuracionMin(context: Context): Int =
        duracion(comidaInicioMin(context), comidaFinMin(context))

    fun duracion(inicioMin: Int, finMin: Int): Int {
        val bruto = (finMin - inicioMin + 24 * 60) % (24 * 60)
        return if (bruto == 0) 24 * 60 else bruto
    }

    // -----------------------------------------------------------------------
    // Caminata
    // -----------------------------------------------------------------------

    /** Si el botón «Caminar» sale dentro del recordatorio de comida. */
    fun caminataEnAviso(context: Context): Boolean =
        prefs(context).getBoolean(K_CAMINATA_EN_AVISO, true)

    fun setCaminataEnAviso(context: Context, activa: Boolean) {
        prefs(context).edit().putBoolean(K_CAMINATA_EN_AVISO, activa).apply()
    }

    // -----------------------------------------------------------------------
    // Estado de ánimo
    // -----------------------------------------------------------------------

    /**
     * Si el ánimo se ofrece: en el botón Anotar y como cierre de la caminata.
     * No tiene recordatorio propio, solo aparece o no aparece.
     */
    fun animoActivo(context: Context): Boolean =
        prefs(context).getBoolean(K_ANIMO_ACTIVO, true)

    fun setAnimoActivo(context: Context, activo: Boolean) {
        prefs(context).edit().putBoolean(K_ANIMO_ACTIVO, activo).apply()
    }

    // -----------------------------------------------------------------------
    // Primera vez
    // -----------------------------------------------------------------------

    /**
     * Si ya se pasó por la bienvenida. Quien venga actualizando desde una
     * versión anterior no la ve: ya tiene la app montada a su manera y sería
     * un trámite en medio, así que se marca como vista al detectar registros.
     */
    fun bienvenidaVista(context: Context): Boolean =
        prefs(context).getBoolean(K_BIENVENIDA_VISTA, false)

    fun setBienvenidaVista(context: Context, vista: Boolean) {
        prefs(context).edit().putBoolean(K_BIENVENIDA_VISTA, vista).apply()
    }

    // -----------------------------------------------------------------------
    // Gimnasio
    // -----------------------------------------------------------------------

    fun gymActivo(context: Context): Boolean =
        prefs(context).getBoolean(K_GYM_ACTIVO, true)

    fun setGymActivo(context: Context, activo: Boolean) {
        prefs(context).edit().putBoolean(K_GYM_ACTIVO, activo).apply()
    }

    fun gymHoraMin(context: Context): Int =
        prefs(context).getInt(K_GYM_HORA, GYM_HORA_POR_DEFECTO)

    fun setGymHoraMin(context: Context, horaMin: Int) {
        prefs(context).edit().putInt(K_GYM_HORA, horaMin.coerceIn(0, 24 * 60 - 1)).apply()
    }

    fun gymDias(context: Context): Set<Int> {
        val guardado = prefs(context).getStringSet(K_GYM_DIAS, null)
            ?: return GYM_DIAS_POR_DEFECTO
        return guardado.mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun setGymDias(context: Context, dias: Set<Int>) {
        prefs(context).edit()
            .putStringSet(K_GYM_DIAS, dias.map { it.toString() }.toSet())
            .apply()
    }

    /** Veces por semana tras las que deja de preguntar. [GYM_SIN_OBJETIVO] = nunca calla. */
    fun gymObjetivoSemanal(context: Context): Int =
        prefs(context).getInt(K_GYM_OBJETIVO, GYM_OBJETIVO_POR_DEFECTO)

    fun setGymObjetivoSemanal(context: Context, veces: Int) {
        prefs(context).edit().putInt(K_GYM_OBJETIVO, veces.coerceIn(0, 7)).apply()
    }
}
