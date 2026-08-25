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
                // No molestamos si el carril está apagado, si hoy no es uno de los
                // días elegidos, si ya hay registro hoy, o si ya se ha alcanzado
                // el objetivo semanal.
                val activo = PrefsRecordatorios.gymActivo(context)
                val toca = repo.shouldAskGym(
                    dias = PrefsRecordatorios.gymDias(context),
                    objetivoSemanal = PrefsRecordatorios.gymObjetivoSemanal(context),
                )
                if (activo && toca) {
                    GymNotifier.show(context)
                }
            } finally {
                // Reprogramar para el próximo día elegido.
                GymAlarmScheduler.scheduleNext(context)
                pending.finish()
            }
        }
    }
}
