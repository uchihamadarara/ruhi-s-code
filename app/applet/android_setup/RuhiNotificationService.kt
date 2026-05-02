package com.example.ruhiassistant

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification

class RuhiNotificationService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val prefs = getSharedPreferences("RuhiSettings", android.content.Context.MODE_PRIVATE)
        if (!prefs.getBoolean("service_whatsapp", true)) return

        val packageName = sbn.packageName
        
        if (packageName == "com.whatsapp") {
            val extras = sbn.notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: return
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return

            // Avoid reading group summaries like "2 messages from 2 chats"
            if (text.contains("messages from") && text.contains("chats")) return
            
            // Broadcast the message content to MainActivity to read out loud
            val intent = Intent("com.ruhi.ACTION_READ_NOTIFICATION").apply {
                putExtra("sender", title)
                putExtra("message", text)
            }
            sendBroadcast(intent)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Handle if needed
    }
}
