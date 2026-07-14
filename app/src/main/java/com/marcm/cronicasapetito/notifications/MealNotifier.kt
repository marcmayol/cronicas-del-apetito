package com.marcm.cronicasapetito.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.marcm.cronicasapetito.R
import com.marcm.cronicasapetito.ui.EntryActivity
import com.marcm.cronicasapetito.ui.WalkMoodActivity

object MealNotifier {

    const val CHANNEL_ID = "meal_reminder_channel"
    const val NOTIFICATION_ID = 2001

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_desc)
            }
            nm.createNotificationChannel(channel)
        }
    }

    fun show(context: Context) {
        ensureChannel(context)

        // Acción "Sí" → abre la pantalla de entrada
        val yesIntent = Intent(context, EntryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val yesPi = PendingIntent.getActivity(
            context, 10, yesIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Acción "No" → abre el flujo de caminata + estado de ánimo
        val noIntent = Intent(context, WalkMoodActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val noPi = PendingIntent.getActivity(
            context, 11, noIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Acción "Nada" → registra directamente que no se ha hecho nada (sin abrir pantalla)
        val nothingIntent = Intent(context, NothingLogReceiver::class.java)
        val nothingPi = PendingIntent.getBroadcast(
            context, 12, nothingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notif_title))
            .setContentText(context.getString(R.string.notif_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(yesPi)
            .addAction(0, context.getString(R.string.action_yes), yesPi)
            .addAction(0, context.getString(R.string.action_no), noPi)
            .addAction(0, context.getString(R.string.action_nothing), nothingPi)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Si el permiso POST_NOTIFICATIONS no está concedido, ignoramos.
        }
    }

    fun dismiss(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
