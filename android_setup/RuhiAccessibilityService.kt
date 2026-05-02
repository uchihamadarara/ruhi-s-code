package com.example.ruhiassistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class RuhiAccessibilityService : AccessibilityService() {
    private val commandReceiver = object : BroadcastReceiver() { override fun onReceive(context: Context?, intent: Intent?) { if (intent?.action == "com.ruhi.ACTION_SEND_WHATSAPP") { targetContact = intent.getStringExtra("contact") ?: ""; messageToSend = intent.getStringExtra("message") ?: ""; isSendingWhatsApp = true; /* Launch WhatsApp Intent implicitly */ } } }

    private var isSendingWhatsApp = false
    private var targetContact = ""
    private var messageToSend = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        registerReceiver(commandReceiver, IntentFilter("com.ruhi.ACTION_SEND_WHATSAPP"), Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isSendingWhatsApp) return
        
        val rootNode = rootInActiveWindow ?: return

        // 1. WhatsApp Search Logic
        val searchBox = findNodeById(rootNode, "com.whatsapp:id/search_input")
        if (searchBox != null && targetContact.isNotEmpty()) {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, targetContact)
            searchBox.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            targetContact = "" // clears to move to next step
            return
        }

        // 2. Select Contact
        // Requires clicking the correct contact in list after searching

        // 3. Type Message Logic
        val messageBox = findNodeById(rootNode, "com.whatsapp:id/entry")
        if (messageBox != null && messageToSend.isNotEmpty()) {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, messageToSend)
            messageBox.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            
            // Logically wait, then press send
            val sendButton = findNodeById(rootNode, "com.whatsapp:id/send")
            sendButton?.performAction(AccessibilityNodeInfo.ACTION_CLICK)

            // Reset state
            isSendingWhatsApp = false
            messageToSend = ""
        }
    }

    override fun onDestroy() {
        unregisterReceiver(commandReceiver)
        super.onDestroy()
    }

    override fun onInterrupt() {
        // Handle interrupt
    }

    // Helper to start the WhatsApp sequence from other activities
    fun triggerWhatsAppMessage(contact: String, message: String) {
        this.targetContact = contact
        this.messageToSend = message
        this.isSendingWhatsApp = true
        // Code to launch WhatsApp Intent here
    }

    private fun findNodeById(node: AccessibilityNodeInfo, id: String): AccessibilityNodeInfo? {
        val list = node.findAccessibilityNodeInfosByViewId(id)
        return if (list.isNotEmpty()) list[0] else null
    }
}
