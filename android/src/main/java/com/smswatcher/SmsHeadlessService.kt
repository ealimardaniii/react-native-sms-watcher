package com.smswatcher

import android.content.Context
import android.content.Intent
import android.util.Log
import com.facebook.react.HeadlessJsTaskService
import com.facebook.react.ReactApplication
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.jstasks.HeadlessJsTaskConfig
import org.json.JSONObject

class SmsHeadlessService : HeadlessJsTaskService() {
  private val prefsKey = "pending_sms"

  override fun getTaskConfig(intent: Intent?): HeadlessJsTaskConfig? {
    val message = intent?.getStringExtra("message") ?: return null
    val address = intent.getStringExtra("address") ?: ""

    // ✅ New Arch path: use reactHost instead of reactNativeHost/reactInstanceManager
    val app = application as? ReactApplication
    val reactHost = app?.reactHost
    val reactContext: ReactContext? = reactHost?.currentReactContext

    if (reactContext == null || !reactContext.hasActiveCatalystInstance()) {
      Log.d("SmsWatcher", "⚠️ React context not ready, saving message to prefs")
      saveMessageToPrefs(applicationContext, message, address)

      // ✅ New Arch: ask ReactHost to start (instead of createReactContextInBackground)
      if (reactHost != null) {
        try {
          reactHost.start()
        } catch (e: Exception) {
          Log.e("SmsWatcher", "❌ reactHost.start() failed", e)
        }
      } else {
        Log.w("SmsWatcher", "⚠ reactHost is null (New Arch may be disabled or not available).")
      }

      return null
    }

    Log.d("SmsWatcher", "✅ HeadlessJsTaskConfig created with message: $message")
    val data = Arguments.createMap().apply {
      putString("message", message)
      putString("address", address)
    }
    return HeadlessJsTaskConfig(
      "SmsBackgroundTask",
      data,
      5000,
      true
    )
  }

  private fun saveMessageToPrefs(context: Context, message: String, address: String) {
    val prefs = context.getSharedPreferences("SmsWatcherPrefs", Context.MODE_PRIVATE)
    val json = JSONObject().apply {
      put("message", message)
      put("address", address)
    }
    prefs.edit().putString(prefsKey, json.toString()).apply()
  }

  private fun restorePendingMessages(reactContext: ReactContext) {
    val prefs = applicationContext.getSharedPreferences("SmsWatcherPrefs", Context.MODE_PRIVATE)
    val jsonStr = prefs.getString(prefsKey, null) ?: return
    prefs.edit().remove(prefsKey).apply()

    try {
      val json = JSONObject(jsonStr)
      val message = json.getString("message")
      val address = json.getString("address")

      val retryIntent = Intent(applicationContext, SmsHeadlessService::class.java).apply {
        putExtra("message", message)
        putExtra("address", address)
      }
      applicationContext.startService(retryIntent)
      
    } catch (e: Exception) {
      Log.e("SmsWatcher", "❌ Failed to parse pending message", e)
    }
  }
}
