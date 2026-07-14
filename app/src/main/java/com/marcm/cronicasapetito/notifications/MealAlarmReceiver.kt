package com.marcm.cronicasapetito.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MealAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        MealNotifier.show(context)
        // Reprogramar la siguiente alarma horaria
        MealAlarmScheduler.scheduleNext(context)
    }
}
