package com.marcm.cronicasapetito

import android.app.Application
import com.marcm.actualizador.Actualizador
import com.marcm.actualizador.ActualizadorConfig
import com.marcm.cronicasapetito.data.AppDatabase
import com.marcm.cronicasapetito.data.MealRepository
import com.marcm.cronicasapetito.notifications.GymAlarmScheduler
import com.marcm.cronicasapetito.notifications.GymNotifier
import com.marcm.cronicasapetito.notifications.MealAlarmScheduler
import com.marcm.cronicasapetito.notifications.MealNotifier
import com.marcm.cronicasapetito.ui.Fechas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CronicasApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.get(this) }

    val actualizador: Actualizador by lazy {
        Actualizador(
            app = this,
            config = ActualizadorConfig(
                manifiestoUrl = "https://marcmayol.com/cronicas-del-apetito/updates.json",
                versionCodeActual = BuildConfig.VERSION_CODE,
                checkHorasPorDefecto = 24,
            ),
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Los formatos de fecha salen de recursos: hay que darles el contexto
        // antes de que cualquier pantalla los pida.
        Fechas.init(this)
        MealNotifier.ensureChannel(this)
        GymNotifier.ensureChannel(this)
        MealAlarmScheduler.scheduleNext(this)
        GymAlarmScheduler.scheduleNext(this)
        // Programa la comprobación periódica de actualizaciones (WorkManager).
        actualizador.programarPeriodica()

        // Las fotos de los registros borrados se quedan en el disco a propósito
        // —mientras se pueda deshacer, el archivo tiene que existir—, así que
        // las que ya no apunta nadie se recogen aquí, en el siguiente arranque.
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                MealRepository(database.mealDao())
                    .limpiarFotosHuerfanas(java.io.File(filesDir, "photos"))
            }
        }
    }
}
