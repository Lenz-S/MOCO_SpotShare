package com.example.moco.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.moco.MainActivity
import com.example.moco.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * MocoMessagingService: Verarbeitet eingehende Push-Benachrichtigungen von Firebase.
 * Diese Klasse wird im Hintergrund ausgeführt, auch wenn die App geschlossen ist.
 */
class MocoMessagingService : FirebaseMessagingService() {

    companion object {
        /**
         * Statische Hilfsfunktion, um überall in der App eine lokale Benachrichtigung auszulösen.
         */
        fun showLocalNotification(context: Context, title: String, messageBody: String) {
            val intent = Intent(context, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE
            )

            val channelId = "moco_notifications"
            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Parkplatz-Updates",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Benachrichtigungen über Buchungen und Statusänderungen"
                }
                notificationManager.createNotificationChannel(channel)
            }

            notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
        }
    }

    /**
     * Wird aufgerufen, wenn eine neue Push-Nachricht empfangen wird.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: "MOCO SpotShare"
        val message = remoteMessage.notification?.body ?: "Neuigkeiten zu deinem Parkplatz!"

        showLocalNotification(this, title, message)
    }
}