package com.marcm.cronicasapetito.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object GymAlarmScheduler {

    private const val REQUEST_CODE = 1002

    fun scheduleNext(context: Context) {
        val dias = PrefsRecordatorios.gymDias(context)
        if (!PrefsRecordatorios.gymActivo(context) || dias.isEmpty()) {
            cancel(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context)
        val triggerAt = nextTrigger(PrefsRecordatorios.gymHoraMin(context), dias)

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

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GymAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Próxima vez que sean las [horaMin] en uno de los [dias] elegidos. Saltar
     * directamente al día bueno evita despertar la app los días que no toca.
     */
    private fun nextTrigger(horaMin: Int, dias: Set<Int>): Long {
        val ahora = System.currentTimeMillis()
        val base = Calendar.getInstance().apply {
            timeInMillis = ahora
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        for (salto in 0..7) {
            val dia = (base.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, salto) }
            if (dia.get(Calendar.DAY_OF_WEEK) !in dias) continue
            val instante = dia.apply { add(Calendar.MINUTE, horaMin) }.timeInMillis
            if (instante > ahora) return instante
        }
        // Con [dias] no vacío siempre hay uno en la semana que viene.
        return ahora + 24L * 60 * 60 * 1000
    }
}
