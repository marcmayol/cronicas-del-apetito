package com.marcm.cronicasapetito.notifications

import android.content.Context
import com.marcm.cronicasapetito.R
import java.util.Calendar
import java.util.Locale

/** «08:00» a partir de los minutos desde medianoche. */
fun horaTexto(minutosDelDia: Int): String =
    String.format(Locale.getDefault(), "%02d:%02d", minutosDelDia / 60, minutosDelDia % 60)

/** «cada 1 h 30 min» a partir de los minutos de separación entre avisos. */
fun frecuenciaTexto(context: Context, cadaMin: Int): String = when {
    cadaMin < 60 -> context.getString(R.string.freq_minutes, cadaMin)
    cadaMin % 60 == 0 -> context.getString(R.string.freq_hours, cadaMin / 60)
    else -> context.getString(R.string.freq_hours_minutes, cadaMin / 60, cadaMin % 60)
}

/** Lo mismo sin el «cada»: es lo que va dentro de los chips de Ajustes. */
fun frecuenciaCorta(context: Context, cadaMin: Int): String = when {
    cadaMin < 60 -> context.getString(R.string.freq_minutes_short, cadaMin)
    cadaMin % 60 == 0 -> context.getString(R.string.freq_hours_short, cadaMin / 60)
    else -> context.getString(R.string.freq_hours_minutes_short, cadaMin / 60, cadaMin % 60)
}

/**
 * Cuándo toca el siguiente aviso. Vive aparte de [MealAlarmScheduler] porque no
 * depende de Android: así se puede comprobar con tests normales, que es lo único
 * que sostiene una ventana horaria configurable sin tener que esperar a mañana
 * para ver si falla.
 */
object RejillaAvisos {

    /**
     * Primer instante posterior a [ahora] que caiga en la rejilla de avisos: los
     * que salen de [inicioMin] sumando [cadaMin] hasta agotar [duracionMin].
     *
     * Se recorren ayer, hoy y mañana porque la ventana puede cruzar la
     * medianoche, y los saltos se hacen con [Calendar.add] para que los cambios
     * de hora de marzo y octubre no descuadren la rejilla.
     */
    fun proximoDisparo(ahora: Long, inicioMin: Int, duracionMin: Int, cadaMin: Int): Long {
        val paso = cadaMin.coerceAtLeast(1)
        for (dia in -1..1) {
            val base = medianoche(ahora).apply { add(Calendar.DAY_OF_YEAR, dia) }
            var offset = 0
            while (offset <= duracionMin) {
                val instante = (base.clone() as Calendar)
                    .apply { add(Calendar.MINUTE, inicioMin + offset) }
                    .timeInMillis
                if (instante > ahora) return instante
                offset += paso
            }
        }
        // Inalcanzable con una rejilla válida, pero mejor una hora de más que un crash.
        return ahora + 60L * 60 * 1000
    }

    private fun medianoche(millis: Long): Calendar = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
