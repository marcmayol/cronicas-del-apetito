package com.marcm.cronicasapetito.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Qué tema usa la app: el del móvil, o el que elija quien la usa.
 *
 * Se guarda en preferencias, pero vive además en un estado observable de
 * Compose para que el cambio se vea en el acto y no al reiniciar. Es un objeto
 * global a propósito: el tema lo leen cinco Activities distintas, y pasarlo por
 * parámetros hasta cada una sería mucho cable para un solo interruptor.
 */
object TemaApp {

    enum class Modo { SISTEMA, CLARO, OSCURO }

    private const val PREFS = "apariencia"
    private const val CLAVE = "tema"

    /** Lo lee [CronicasTheme]; escribirlo repinta todas las pantallas vivas. */
    var modo by mutableStateOf(Modo.SISTEMA)
        private set

    fun init(context: Context) {
        modo = leer(context)
    }

    fun cambiar(context: Context, nuevo: Modo) {
        modo = nuevo
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(CLAVE, nuevo.name)
            .apply()
    }

    private fun leer(context: Context): Modo {
        val guardado = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(CLAVE, null) ?: return Modo.SISTEMA
        return runCatching { Modo.valueOf(guardado) }.getOrDefault(Modo.SISTEMA)
    }
}
