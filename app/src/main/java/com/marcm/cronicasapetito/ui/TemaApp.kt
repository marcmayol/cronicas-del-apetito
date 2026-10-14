package com.marcm.cronicasapetito.ui

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StyleRes
import com.marcm.cronicasapetito.R
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

    /**
     * Los tres primeros usan la paleta neutra; [GRIMORIO] es el cuaderno marrón
     * de siempre, y de día o de noche sigue al móvil. Los nombres son los que se
     * guardan en preferencias: no renombrarlos.
     */
    enum class Modo { SISTEMA, CLARO, OSCURO, GRIMORIO }

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

    /** Si se pinta el cuaderno marrón en vez de la paleta neutra. */
    val esGrimorio: Boolean get() = modo == Modo.GRIMORIO

    /**
     * Si toca noche, para lo que se pinta fuera de Compose (los selectores de
     * fecha y hora del sistema). Dentro de Compose se mira [LocalEsNoche].
     */
    fun esNoche(context: Context): Boolean = when (modo) {
        Modo.CLARO -> false
        Modo.OSCURO -> true
        Modo.SISTEMA, Modo.GRIMORIO ->
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * Tema para los DatePickerDialog/TimePickerDialog. El de themes.xml solo
     * sabe lo que dice el móvil; con el tema elegido a mano saldrían del color
     * equivocado.
     */
    @StyleRes
    fun temaSelector(context: Context): Int {
        val noche = esNoche(context)
        return when {
            esGrimorio && noche -> R.style.Selector_Grimorio_Noche
            esGrimorio -> R.style.Selector_Grimorio
            noche -> R.style.Selector_Neutro_Noche
            else -> R.style.Selector_Neutro
        }
    }

    private fun leer(context: Context): Modo {
        val guardado = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(CLAVE, null) ?: return Modo.SISTEMA
        return runCatching { Modo.valueOf(guardado) }.getOrDefault(Modo.SISTEMA)
    }
}
