package com.marcm.cronicasapetito.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MealAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Una alarma puede sobrevivir a que apagues el carril desde Ajustes.
        if (PrefsRecordatorios.comidaActiva(context) && !SleepPrefs.isSleeping(context)) {
            MealNotifier.show(context)
        }
        // Reprogramar el siguiente aviso de la ventana
        MealAlarmScheduler.scheduleNext(context)
    }
}
