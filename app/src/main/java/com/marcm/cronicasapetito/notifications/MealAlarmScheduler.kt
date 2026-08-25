package com.marcm.cronicasapetito.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object MealAlarmScheduler {

    private const val REQUEST_CODE = 1001

    fun scheduleNext(context: Context) {
        if (!PrefsRecordatorios.comidaActiva(context)) {
            cancel(context)
            return
        }
        programar(context, nextTrigger(context))
    }

    private fun programar(context: Context, triggerAt: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    /**
     * Activa el "modo a dormir": silencia los recordatorios hasta el comienzo de
     * la próxima ventana, descarta cualquier aviso visible y reprograma la
     * alarma para que despierte ya directamente a esa hora.
     */
    fun goToSleep(context: Context) {
        SleepPrefs.setSleepUntil(context, proximoInicioVentana(context))
        MealNotifier.dismiss(context)
        scheduleNext(context)
    }

    /**
     * Cancela el "modo a dormir" antes de tiempo (p. ej. si te levantas antes de
     * que abra la ventana): borra el periodo de descanso y vuelve a programar la
     * alarma al siguiente aviso.
     */
    fun wakeUp(context: Context) {
        SleepPrefs.clear(context)
        scheduleNext(context)
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MealAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Siguiente aviso según la ventana configurada. Si estamos en «modo a
     * dormir», la próxima alarma es directamente el instante de despertar.
     */
    private fun nextTrigger(context: Context): Long {
        val ahora = System.currentTimeMillis()
        val sleepUntil = SleepPrefs.getSleepUntil(context)
        if (sleepUntil > ahora) return sleepUntil

        return RejillaAvisos.proximoDisparo(
            ahora = ahora,
            inicioMin = PrefsRecordatorios.comidaInicioMin(context),
            duracionMin = PrefsRecordatorios.comidaDuracionMin(context),
            cadaMin = PrefsRecordatorios.comidaCadaMin(context),
        )
    }

    /** Hora a la que vuelven los avisos tras dormir: el primero de la próxima ventana. */
    fun proximoInicioVentana(context: Context): Long = RejillaAvisos.proximoDisparo(
        ahora = System.currentTimeMillis(),
        inicioMin = PrefsRecordatorios.comidaInicioMin(context),
        duracionMin = 0,
        cadaMin = PrefsRecordatorios.comidaCadaMin(context),
    )
}
