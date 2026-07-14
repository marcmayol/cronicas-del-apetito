package com.marcm.cronicasapetito

import android.app.Application
import com.marcm.cronicasapetito.data.AppDatabase
import com.marcm.cronicasapetito.notifications.GymAlarmScheduler
import com.marcm.cronicasapetito.notifications.GymNotifier
import com.marcm.cronicasapetito.notifications.MealAlarmScheduler
import com.marcm.cronicasapetito.notifications.MealNotifier

class CronicasApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.get(this) }

    override fun onCreate() {
        super.onCreate()
        MealNotifier.ensureChannel(this)
        GymNotifier.ensureChannel(this)
        MealAlarmScheduler.scheduleNext(this)
        GymAlarmScheduler.scheduleNext(this)
    }
}
