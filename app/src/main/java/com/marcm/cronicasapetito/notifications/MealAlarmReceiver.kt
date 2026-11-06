package com.marcm.cronicasapetito.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.ZoneId

class MealAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Una alarma puede sobrevivir a que apagues el carril desde Ajustes.
        if (PrefsRecordatorios.comidaActiva(context) && !SleepPrefs.isSleeping(context)) {
            val aviso = PlanVacaciones.alSonar(
                ahora = System.currentTimeMillis(),
                v = VacacionesPrefs.vigentes(context),
                inicioMin = PrefsRecordatorios.comidaInicioMin(context),
                ultimoDiario = VacacionesPrefs.ultimoDiario(context),
                zona = ZoneId.systemDefault(),
            )
            when (aviso) {
                AvisoComida.NORMAL -> MealNotifier.show(context)
                AvisoComida.VACACIONES -> {
                    VacacionesPrefs.setUltimoDiario(context, LocalDate.now())
                    MealNotifier.show(context, deVacaciones = true)
                }
                AvisoComida.NADA -> Unit
            }
        }
        // Reprogramar el siguiente aviso de la ventana
        MealAlarmScheduler.scheduleNext(context)
    }
}
