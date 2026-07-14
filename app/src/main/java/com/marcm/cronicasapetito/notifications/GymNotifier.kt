package com.marcm.cronicasapetito.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.marcm.cronicasapetito.R

object GymNotifier {

    const val CHANNEL_ID = "gym_reminder_channel"
    const val NOTIFICATION_ID = 3001

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.gym_notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.gym_notif_channel_desc)
            }
            nm.createNotificationChannel(channel)
        }
    }

    fun show(context: Context) {
        ensureChannel(context)

        // Botones que registran directamente desde la notificación, sin abrir la app.
        val yesPi = logPendingIntent(context, went = true, requestCode = 30)
        val noPi = logPendingIntent(context, went = false, requestCode = 31)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.gym_notif_title))
            .setContentText(context.getString(R.string.gym_notif_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.action_yes), yesPi)
            .addAction(0, context.getString(R.string.action_no), noPi)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Si el permiso POST_NOTIFICATIONS no está concedido, ignoramos.
        }
    }

    private fun logPendingIntent(context: Context, went: Boolean, requestCode: Int): PendingIntent {
        val intent = Intent(context, GymLogReceiver::class.java).apply {
            putExtra(GymLogReceiver.EXTRA_WENT, went)
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun dismiss(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
