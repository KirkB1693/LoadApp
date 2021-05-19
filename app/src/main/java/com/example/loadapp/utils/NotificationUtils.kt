package com.example.loadapp.utils

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.loadapp.DetailActivity
import com.example.loadapp.MainActivity
import com.example.loadapp.R
import com.example.loadapp.constants.Constants.CHANNEL
import com.example.loadapp.constants.Constants.CHANNEL_ID
import com.example.loadapp.constants.Constants.DOWNLOAD_FILENAME
import com.example.loadapp.constants.Constants.DOWNLOAD_STATUS
import com.example.loadapp.constants.Constants.NOTIFICATION_ID
import java.util.*


fun NotificationManager.sendNotification(
    messageBody: String,
    applicationContext: Context,
    downloadStatus: String,
    downloadFilename: String
) {
    // Create the content intent for the notification, which launches
    // this activity
    val notificationId = 0
    val contentIntent = Intent(applicationContext, MainActivity::class.java)
    val contentPendingIntent = PendingIntent.getActivity(
        applicationContext,
        notificationId,
        contentIntent,
        PendingIntent.FLAG_UPDATE_CURRENT
    )


    val detailIntent = Intent(applicationContext, DetailActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    detailIntent.putExtra(DOWNLOAD_STATUS, downloadStatus)
    detailIntent.putExtra(DOWNLOAD_FILENAME, downloadFilename)
    detailIntent.putExtra(NOTIFICATION_ID,notificationId)
    val detailPendingIntent: PendingIntent = PendingIntent.getActivity(
        applicationContext,
        notificationId,
        detailIntent,
        PendingIntent.FLAG_UPDATE_CURRENT
    )

    if (!isNotificationChannelAvailable(this)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(this)
        }
    }
    // Build the notification
    val builder = NotificationCompat.Builder(
        applicationContext,
        CHANNEL_ID
    )
        .setSmallIcon(R.drawable.ic_assistant_black_24dp)
        .setLargeIcon(
            BitmapFactory.decodeResource(
                applicationContext.resources,
                R.drawable.ic_assistant_black_24dp
            )
        )
        .setContentTitle(
            applicationContext
                .getString(R.string.notification_title)
        )
        .setContentText(messageBody)
        .setContentIntent(contentPendingIntent)
        .setAutoCancel(true)
        .addAction(
            R.drawable.ic_assistant_black_24dp,
            applicationContext.getString(R.string.notification_button),
            detailPendingIntent
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
    notify(notificationId, builder.build())
}

fun generateRandom(): Int {
    val random = Random()
    return random.nextInt(9999 - 1000) + 1000
}

@TargetApi(Build.VERSION_CODES.O)
private fun isNotificationChannelAvailable(
    notificationManager: NotificationManager
) = notificationManager.getNotificationChannel(CHANNEL_ID) != null

@RequiresApi(Build.VERSION_CODES.O)
fun createNotificationChannel(
    notificationManager: NotificationManager
) {
    val channel = NotificationChannel(
        CHANNEL_ID, CHANNEL,
        NotificationManager.IMPORTANCE_HIGH
    )
    notificationManager.createNotificationChannel(channel)
}

fun NotificationManager.cancelNotifications() {
    cancelAll()
}