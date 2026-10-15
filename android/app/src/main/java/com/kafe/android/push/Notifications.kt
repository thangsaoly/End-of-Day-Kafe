package com.kafe.android.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kafe.android.MainActivity
import com.kafe.android.R

object Notifications {

    const val CHANNEL_NEW_DEALS = "new_deals"
    const val EXTRA_DEAL_ID = "com.kafe.android.extra.DEAL_ID"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_NEW_DEALS,
            "New deals from favourite cafes",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when a cafe you follow posts a surplus deal"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun showNewDeal(context: Context, dealId: Long, title: String, body: String) {
        if (!canPost(context)) return

        val openDeal = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DEAL_ID, dealId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            dealId.toInt(), // one PendingIntent per deal so extras don't overwrite each other
            openDeal,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_NEW_DEALS)
            .setSmallIcon(R.drawable.ic_stat_kafe)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(dealId.toInt(), notification)
        } catch (e: SecurityException) {
            // Permission revoked between the check and notify
        }
    }
}
