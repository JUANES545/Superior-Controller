package com.example.superiorcontroller.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.superiorcontroller.MainActivity
import com.example.superiorcontroller.R

class HidForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val hostName = intent?.getStringExtra(EXTRA_HOST_NAME)
                val registered = intent?.getBooleanExtra(EXTRA_REGISTERED, false) ?: false
                val notification = buildNotification(hostName, registered)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notif_channel_desc)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(hostName: String?, registered: Boolean): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val text = when {
            hostName != null -> getString(R.string.notif_connected_to, hostName)
            registered -> getString(R.string.notif_hid_ready)
            else -> getString(R.string.notif_hid_active)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "hid_service"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.example.superiorcontroller.STOP_HID"
        const val EXTRA_HOST_NAME = "host_name"
        const val EXTRA_REGISTERED = "registered"

        fun start(context: Context, hostName: String? = null, registered: Boolean = true) {
            val intent = Intent(context, HidForegroundService::class.java).apply {
                putExtra(EXTRA_HOST_NAME, hostName)
                putExtra(EXTRA_REGISTERED, registered)
            }
            context.startForegroundService(intent)
        }

        fun update(context: Context, hostName: String?, registered: Boolean) {
            start(context, hostName, registered)
        }

        fun stop(context: Context) {
            val intent = Intent(context, HidForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
