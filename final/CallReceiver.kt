package com.example.ruhiassistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.provider.ContactsContract
import android.net.Uri

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("RuhiSettings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("service_calls", true)) return

        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                var callerName = "Unknown number"
                
                if (incomingNumber != null) {
                    callerName = getContactName(context, incomingNumber) ?: incomingNumber
                }

                // Notify MainActivity to announce the call
                val announceIntent = Intent("com.ruhi.ACTION_ANNOUNCE_CALL").apply {
                    putExtra("callerName", callerName)
                    setPackage(context.packageName)
                }
                context.sendBroadcast(announceIntent)
            }
        }
    }

    private fun getContactName(context: Context, phoneNumber: String): String? {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        var contactName: String? = null
        
        try {
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    contactName = cursor.getString(0)
                }
                cursor.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return contactName
    }
}
