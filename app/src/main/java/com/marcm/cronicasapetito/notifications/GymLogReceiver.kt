package com.marcm.cronicasapetito.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.marcm.cronicasapetito.CronicasApp
import com.marcm.cronicasapetito.data.MealRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Registra la respuesta Sí/No del gimnasio pulsada en la notificación. */
class GymLogReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_WENT = "extra_went"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val went = intent.getBooleanExtra(EXTRA_WENT, false)
        val pending = goAsync()
        val app = context.applicationContext as CronicasApp
        val repo = MealRepository(app.database.mealDao())
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repo.addGym(went)
            } finally {
                GymNotifier.dismiss(context)
                pending.finish()
            }
        }
    }
}
