package com.smswatcher

import android.content.Intent
import android.util.Log
import com.facebook.react.HeadlessJsTaskService
import com.facebook.react.bridge.Arguments
import com.facebook.react.jstasks.HeadlessJsTaskConfig
import com.facebook.react.ReactApplication

class SmsHeadlessService : HeadlessJsTaskService() {
    override fun getTaskConfig(intent: Intent?): HeadlessJsTaskConfig? {
        val application = application as ReactApplication
        val reactInstanceManager = application.reactNativeHost.reactInstanceManager

        if (!reactInstanceManager.hasStartedCreatingInitialContext()) {
            reactInstanceManager.createReactContextInBackground()
            return null 
        }

        val reactContext = reactInstanceManager.currentReactContext
        if (reactContext == null) {
            return null
        }

        val message = intent?.getStringExtra("message") ?: return null
        val address = intent.getStringExtra("address") ?: "unknown"

        val extras = Arguments.createMap().apply {
            putString("message", message)
            putString("address", address)
        }


        return HeadlessJsTaskConfig(
            "SmsBackgroundTask",
            extras,
            5000, // Timeout
            true   // Allow in foreground & background
        )
    }
}
