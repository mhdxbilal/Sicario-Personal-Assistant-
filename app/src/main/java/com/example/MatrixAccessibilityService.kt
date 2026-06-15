package com.example

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.MatrixRepository

class MatrixAccessibilityService : AccessibilityService() {

    companion object {
        const val TAG = "MatrixAccessibility"
        const val ACTION_COMMAND = "com.example.MATRIX_COMMAND"
        const val EXTRA_COMMAND_TEXT = "extra_command_text"
        const val EXTRA_RPA_PHONE = "extra_rpa_phone"
        const val EXTRA_RPA_MSG = "extra_rpa_msg"

        var isServiceConnected = false
            private set
    }

    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_COMMAND) {
                val command = intent.getStringExtra(EXTRA_COMMAND_TEXT)?.lowercase() ?: return
                Log.d(TAG, "Received offline system control command: $command")
                MatrixRepository.addLog("Accessibility received command: '$command'")

                when {
                    command.contains("next") || command.contains("scroll") || command.contains("താഴേക്ക്") || command.contains("പോകുക") -> {
                        performVerticalSwipeUp()
                    }
                    command.contains("back") || command.contains("മടങ്ങുക") -> {
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        MatrixRepository.addLog("Executed: GLOBAL_ACTION_BACK")
                    }
                    command.contains("home") || command.contains("ഹോം") -> {
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        MatrixRepository.addLog("Executed: GLOBAL_ACTION_HOME")
                    }
                    command.contains("whatsapp") || command.contains("മെസ്സേജ്") || command.contains("വാട്സ്") -> {
                        val phone = intent.getStringExtra(EXTRA_RPA_PHONE) ?: "+919876543210"
                        val msg = intent.getStringExtra(EXTRA_RPA_MSG) ?: "Hello from Matrix Assistant!"
                        executeWhatsAppRPA(phone, msg)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(ACTION_COMMAND)
        registerReceiver(commandReceiver, filter)
        Log.d(TAG, "Matrix Accessibility Service Created and Broadcast Receiver registered.")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceConnected = true
        MatrixRepository.addLog("Matrix Accessibility Service Connected.")
        Log.d(TAG, "Matrix Accessibility Service connected successfully.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intercept events if needed
    }

    override fun onInterrupt() {
        Log.d(TAG, "Matrix Accessibility Service interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceConnected = false
        try {
            unregisterReceiver(commandReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
        MatrixRepository.addLog("Matrix Accessibility Service Disconnected.")
    }

    // Programmable Scroll Gesture for short-form video feeds
    private fun performVerticalSwipeUp() {
        MatrixRepository.addLog("Triggered 'Matrix Next' page swipe.")
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val screenWidth = displayMetrics.widthPixels

        val startX = screenWidth / 2f
        val startY = screenHeight * 0.8f
        val endX = screenWidth / 2f
        val endY = screenHeight * 0.2f

        val path = Path()
        path.moveTo(startX, startY)
        path.lineTo(endX, endY)

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 50, 350))

        dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.d(TAG, "Scroll gesture completed successfully.")
                MatrixRepository.addLog("Programmatic vertical scroll executed successfully.")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.e(TAG, "Scroll gesture cancelled.")
                MatrixRepository.addLog("Programmatic vertical scroll cancelled/failed.")
            }
        }, null)
    }

    // Automation Engine: RPA messaging sequence
    private fun executeWhatsAppRPA(phoneNumber: String, messageText: String) {
        MatrixRepository.addLog("Launching WhatsApp RPA sequence for target: $phoneNumber")

        val cleanPhone = phoneNumber.replace("+", "").replace(" ", "").trim()
        val uriString = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${android.net.Uri.encode(messageText)}"
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uriString)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            `setPackage`("com.whatsapp")
        }

        try {
            startActivity(intent)
            MatrixRepository.addLog("WhatsApp intent dispatched. Traversing view nodes in chat window...")
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    findAndClickSendButton()
                } catch (e: Exception) {
                    Log.e(TAG, "Error traversing search nodes", e)
                    MatrixRepository.addLog("Traversal warning: ${e.localizedMessage}")
                }
            }, 2500)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch WhatsApp", e)
            MatrixRepository.addLog("RPA error: Is WhatsApp installed?")
        }
    }

    // WhatsApp View Node traversal seeking the Send button
    private fun findAndClickSendButton() {
        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            Log.e(TAG, "Active window node is null. Cannot automate clicks.")
            MatrixRepository.addLog("RPA traversal failed: Active window root is null.")
            return
        }

        val sendByIdList = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
        if (!sendByIdList.isNullOrEmpty()) {
            val sendNode = sendByIdList[0]
            if (sendNode.isEnabled && sendNode.isClickable) {
                sendNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                MatrixRepository.addLog("RPA click dispatched successfully on send ID node.")
                return
            }
        }

        val success = traverseAndClickSendFallback(rootNode)
        if (success) {
            MatrixRepository.addLog("RPA automation clicked send button via fallback scanner.")
        } else {
            MatrixRepository.addLog("RPA Warning: Send button found, but user interaction is required.")
        }
    }

    private fun traverseAndClickSendFallback(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        val contentDesc = node.contentDescription?.toString()?.lowercase()
        val textStr = node.text?.toString()?.lowercase()

        val isSendResource = node.viewIdResourceName?.contains("send", ignoreCase = true) == true
        val isSendDesc = contentDesc?.contains("send") == true || contentDesc?.contains("അയയ്ക്കുക") == true

        if (isSendResource || isSendDesc) {
            if (node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            } else {
                var parent = node.parent
                while (parent != null) {
                    if (parent.isClickable) {
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        return true
                    }
                    parent = parent.parent
                }
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                if (traverseAndClickSendFallback(child)) {
                    return true
                }
            }
        }
        return false
    }
}
