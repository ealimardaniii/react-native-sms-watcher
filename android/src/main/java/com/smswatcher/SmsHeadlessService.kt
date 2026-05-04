package com.smswatcher

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.facebook.react.HeadlessJsTaskService
import com.facebook.react.ReactApplication
import com.facebook.react.ReactInstanceManager
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.jstasks.HeadlessJsTaskConfig
import org.json.JSONObject

class SmsHeadlessService : HeadlessJsTaskService() {
  private val prefsKey = "pending_sms"

  /**
   * Returns the current ReactContext if one exists, regardless of whether the host
   * app is running the New Architecture (reactHost) or the Old Architecture
   * (reactNativeHost.reactInstanceManager). Returns null if neither path is ready.
   */
  private fun currentReactContext(): ReactContext? {
    val app = application as? ReactApplication ?: return null

    // New Arch path
    try {
      val host = app.reactHost
      if (host != null) {
        host.currentReactContext?.let { return it }
      }
    } catch (e: Throwable) {
      // reactHost may not exist on older RN versions — fall through to Old Arch.
    }

    // Old Arch path
    return try {
      app.reactNativeHost.reactInstanceManager.currentReactContext
    } catch (e: Throwable) {
      null
    }
  }

  /**
   * Asks RN to spin up the React context, on whichever architecture is in use,
   * and runs [onReady] when (or shortly after) the context becomes available.
   */
  private fun startReactContextAndThen(onReady: (ReactContext) -> Unit) {
    val app = application as? ReactApplication ?: return

    // Try New Arch first
    try {
      val host = app.reactHost
      if (host != null) {
        host.start()
        Handler(Looper.getMainLooper()).postDelayed({
          host.currentReactContext?.let(onReady)
        }, 1500)
        return
      }
    } catch (e: Throwable) {
      // fall through to Old Arch
    }

    // Old Arch fallback
    try {
      val rim = app.reactNativeHost.reactInstanceManager
      val existing = rim.currentReactContext
      if (existing != null) {
        onReady(existing)
        return
      }
      rim.addReactInstanceEventListener(object : ReactInstanceManager.ReactInstanceEventListener {
        override fun onReactContextInitialized(context: ReactContext) {
          rim.removeReactInstanceEventListener(this)
          onReady(context)
        }
      })
      rim.createReactContextInBackground()
    } catch (e: Throwable) {
      Log.e("SmsWatcher", "❌ Failed to start ReactInstanceManager", e)
    }
  }

  override fun getTaskConfig(intent: Intent?): HeadlessJsTaskConfig? {
    val message = intent?.getStringExtra("message") ?: return null
    val address = intent.getStringExtra("address") ?: ""

    val reactContext = currentReactContext()

    if (reactContext == null || !reactContext.hasActiveCatalystInstance()) {
      Log.d("SmsWatcher", "⚠️ React context not ready, saving message to prefs")
      saveMessageToPrefs(applicationContext, message, address)

      startReactContextAndThen { ctx ->
        Log.d("SmsWatcher", "✅ ReactContext ready, restoring pending messages")
        restorePendingMessages(ctx)
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
