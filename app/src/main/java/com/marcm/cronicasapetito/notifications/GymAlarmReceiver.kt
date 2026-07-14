package com.marcm.cronicasapetito.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.marcm.cronicasapetito.CronicasApp
import com.marcm.cronicasapetito.data.MealRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GymAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext as CronicasApp
        val repo = MealRepository(app.database.mealDao())
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // No molestamos en fin de semana, si ya hay registro hoy,
                // o si ya se ha ido 2 veces esta semana.
                if (repo.shouldAskGym()) {
                    GymNotifier.show(context)
                }
            } finally {
                // Reprogramar para las 22:00 del día siguiente.
                GymAlarmScheduler.scheduleNext(context)
                pending.finish()
            }
        }
    }
}
