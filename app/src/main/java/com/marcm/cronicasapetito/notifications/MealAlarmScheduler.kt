package com.marcm.cronicasapetito.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object MealAlarmScheduler {

    private const val REQUEST_CODE = 1001
    const val START_HOUR = 8
    const val END_HOUR_INCLUSIVE = 24 // 00:00 del día siguiente

    fun scheduleNext(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MealAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAt = nextHourlyTrigger(context)

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
     * Activa el "modo a dormir": silencia los recordatorios hasta las
     * [START_HOUR]:00 de la próxima mañana, descarta cualquier aviso visible
     * y reprograma la alarma para que despierte ya directamente a esa hora.
     */
    fun goToSleep(context: Context) {
        SleepPrefs.setSleepUntil(context, nextStartHourMillis())
        MealNotifier.dismiss(context)
        scheduleNext(context)
    }

    /**
     * Cancela el "modo a dormir" antes de tiempo (p. ej. si te levantas antes
     * de las 8:00): borra el periodo de descanso y vuelve a programar la alarma
     * a la siguiente hora en punto dentro de la franja.
     */
    fun wakeUp(context: Context) {
        SleepPrefs.clear(context)
        scheduleNext(context)
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MealAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
    }

    /**
     * Devuelve el siguiente cambio de hora en punto dentro de la franja
     * [START_HOUR, END_HOUR_INCLUSIVE]. Si estamos fuera de ventana, salta a las 8:00 del próximo día válido.
     */
    private fun nextHourlyTrigger(context: Context): Long {
        val now = Calendar.getInstance()

        // Modo "me voy a dormir": si seguimos dentro del periodo de descanso,
        // la próxima alarma es directamente el instante de despertar.
        val sleepUntil = SleepPrefs.getSleepUntil(context)
        if (sleepUntil > now.timeInMillis) {
            return sleepUntil
        }

        val target = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.HOUR_OF_DAY, 1)
        }
        val hour = target.get(Calendar.HOUR_OF_DAY)
        // Ventana: 8..23 + 0 (medianoche del día siguiente)
        val inWindow = hour in START_HOUR..23 || hour == 0
        if (!inWindow) {
            // Saltar a las 8:00 del próximo día válido (si es antes de las 8, mismo día)
            if (hour < START_HOUR) {
                target.set(Calendar.HOUR_OF_DAY, START_HOUR)
            } else {
                target.add(Calendar.DAY_OF_YEAR, 1)
                target.set(Calendar.HOUR_OF_DAY, START_HOUR)
            }
        }
        return target.timeInMillis
    }

    /** Próxima ocurrencia de las [START_HOUR]:00 (mañana, salvo que aún no hayan sido hoy). */
    private fun nextStartHourMillis(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, START_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }
}
