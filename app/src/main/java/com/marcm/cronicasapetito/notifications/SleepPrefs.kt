package com.marcm.cronicasapetito.notifications

import android.content.Context

/**
 * Guarda el estado del "modo a dormir": el instante (en millis) hasta el cual
 * NO se deben enviar recordatorios de comida. Cuando ese instante queda en el
 * pasado, el modo deja de tener efecto y las notificaciones se reanudan solas.
 */
object SleepPrefs {

    private const val PREFS = "sleep_prefs"
    private const val KEY_SLEEP_UNTIL = "sleep_until_millis"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Instante hasta el que se silencian los avisos (0 si no hay modo dormir activo). */
    fun getSleepUntil(context: Context): Long =
        prefs(context).getLong(KEY_SLEEP_UNTIL, 0L)

    fun setSleepUntil(context: Context, millis: Long) {
        prefs(context).edit().putLong(KEY_SLEEP_UNTIL, millis).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_SLEEP_UNTIL).apply()
    }

    /** True si ahora mismo estamos dentro del periodo de "me voy a dormir". */
    fun isSleeping(context: Context): Boolean =
        getSleepUntil(context) > System.currentTimeMillis()
}
