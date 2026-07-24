package com.marcm.cronicasapetito

import android.app.Application
import com.marcm.actualizador.Actualizador
import com.marcm.actualizador.ActualizadorConfig
import com.marcm.cronicasapetito.data.AppDatabase
import com.marcm.cronicasapetito.notifications.GymAlarmScheduler
import com.marcm.cronicasapetito.notifications.GymNotifier
import com.marcm.cronicasapetito.notifications.MealAlarmScheduler
import com.marcm.cronicasapetito.notifications.MealNotifier

class CronicasApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.get(this) }

    val actualizador: Actualizador by lazy {
        Actualizador(
            app = this,
            config = ActualizadorConfig(
                manifiestoUrl = "https://marcmayol.github.io/cronicas-del-apetito/updates.json",
                versionCodeActual = BuildConfig.VERSION_CODE,
                checkHorasPorDefecto = 24,
            ),
        )
    }

    override fun onCreate() {
        super.onCreate()
        MealNotifier.ensureChannel(this)
        GymNotifier.ensureChannel(this)
        MealAlarmScheduler.scheduleNext(this)
        GymAlarmScheduler.scheduleNext(this)
        // Programa la comprobación periódica de actualizaciones (WorkManager).
        actualizador.programarPeriodica()
    }
}
