package com.example.ruhiassistant

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class RuhiAccessibilityService : AccessibilityService() {

    companion object {
        var isAutomatingWhatsApp = false
    }

    // Helper receiver to trigger the process from CommandProcessor
    private val automationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.ruhi.ACTION_SEND_WHATSAPP") {
                val number = intent.getStringExtra("number") ?: return
                val text = intent.getStringExtra("message") ?: return
                
                // Format the number properly (e.g. ensure country code exists, remove spaces)
                val formattedNumber = if (number.startsWith("+")) number.replace(" ", "") else "+91" + number.replace(" ", "")
                
                // Set the flag to true so the Accessibility Service knows it should act
                isAutomatingWhatsApp = true

                // Open WhatsApp natively with the message pre-filled
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedNumber&text=${Uri.encode(text)}")
                val waIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                try {
                    context?.startActivity(waIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    isAutomatingWhatsApp = false
                }
            } else if (intent?.action == "com.ruhi.ACTION_ANALYZE_SCREEN") {
                val prefs = context?.getSharedPreferences("RuhiSettings", Context.MODE_PRIVATE)
                if (prefs?.getBoolean("service_screen", true) != true) return

                val rootNode = rootInActiveWindow
                val sb = StringBuilder()
                if (rootNode != null) {
                    extractText(rootNode, sb)
                }
                
                val resultIntent = Intent("com.ruhi.ACTION_SCREEN_TEXT_RESULT").apply {
                    putExtra("text", sb.toString())
                    setPackage(packageName)
                }
                sendBroadcast(resultIntent)
            }
        }
    }

    private fun extractText(node: AccessibilityNodeInfo, sb: StringBuilder) {
        val text = node.text
        val contentDesc = node.contentDescription
        
        if (!text.isNullOrBlank()) {
            sb.append(text).append(" ")
        } else if (!contentDesc.isNullOrBlank()) {
            sb.append(contentDesc).append(" ")
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                extractText(child, sb)
                child.recycle() // Important for memory
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val filter = IntentFilter().apply {
            addAction("com.ruhi.ACTION_SEND_WHATSAPP")
            addAction("com.ruhi.ACTION_ANALYZE_SCREEN")
        }
        registerReceiver(automationReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val prefs = getSharedPreferences("RuhiSettings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("service_screen", true)) return

        if (!isAutomatingWhatsApp) return
        if (event == null) return

        val rootNode = rootInActiveWindow ?: return

        // Check if we are inside WhatsApp
        if (event.packageName?.toString()?.contains("whatsapp") == true) {
            
            // Try to find the send button by common content description ("Send")
            val sendNodesDescr = rootNode.findAccessibilityNodeInfosByText("Send")
            if (sendNodesDescr.isNotEmpty()) {
                val clicked = sendNodesDescr[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (clicked) {
                    isAutomatingWhatsApp = false
                    return
                }
            }

            // Fallback: Recursively look for View descriptions that might match a send button
            try {
               performClick(rootNode)
            } catch (e: Exception) {
               e.printStackTrace()
            }
        }
    }

    private fun performClick(node: AccessibilityNodeInfo) {
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                // Heuristic: It's an ImageView/ImageButton and its description contains "send"
                val className = child.className?.toString()?.lowercase() ?: ""
                val contentDesc = child.contentDescription?.toString()?.lowercase() ?: ""
                
                if (className.contains("imagebutton") || className.contains("imageview")) {
                    if (contentDesc.contains("send")) {
                        child.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        isAutomatingWhatsApp = false
                        return
                    }
                }
                // Recursively search children
                performClick(child)
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(automationReceiver)
    }
}

