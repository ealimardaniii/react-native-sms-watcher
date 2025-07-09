package com.smswatcher

import android.content.Intent
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class HeadlessTaskStarter(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "HeadlessTaskStarter"

    @ReactMethod
    fun start() {
        val context = reactApplicationContext
        val intent = Intent(context, SmsHeadlessService::class.java).apply {
            putExtra("message", "[warmup]")
            putExtra("address", "starter")
        }
        context.startService(intent)
    }
}
