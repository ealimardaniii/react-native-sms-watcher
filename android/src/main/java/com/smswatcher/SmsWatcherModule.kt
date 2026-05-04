package com.smswatcher

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.ReactApplication

class SmsWatcherModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

  private val prefs = reactContext.getSharedPreferences("SmsWatcherPrefs", Context.MODE_PRIVATE)

  init {
    val saved = prefs.getStringSet("watchedNumbers", null)
    if (saved != null) {
      targetNumbers = saved.toMutableList()
    }
  }

  private fun saveNumbersToPrefs() {
    prefs.edit().putStringSet("watchedNumbers", targetNumbers.toSet()).apply()
  }

  private fun triggerWarmupService(context: Context) {
    val intent = Intent(context, SmsHeadlessService::class.java).apply {
      putExtra("message", "[warmup]")
      putExtra("address", "bootstrap")
    }
    try {
      context.startService(intent)
    } catch (e: Exception) {
      Log.e("SmsWatcher", "❌ Failed to trigger warmup service", e)
    }
  }

  private fun warmUpAndTriggerFakeTask() {
    val context = reactApplicationContext
    val app = context.applicationContext as? ReactApplication

    if (app == null) {
      Log.w("SmsWatcher", "⚠ ReactApplication not found.")
      return
    }

    // ── New Arch path (reactHost) ─────────────────────────────────────────
    try {
      val reactHost = app.reactHost
      if (reactHost != null) {
        try {
          reactHost.start()
        } catch (e: Exception) {
          Log.e("SmsWatcher", "❌ reactHost.start() failed", e)
        }

        Handler(Looper.getMainLooper()).postDelayed({
          if (reactHost.currentReactContext != null) {
            Log.d("SmsWatcher", "✅ ReactContext is ready (New Arch), manually triggering JS")
            triggerWarmupService(context)
          } else {
            Log.w("SmsWatcher", "⚠ Still no ReactContext after delay (New Arch).")
          }
        }, 2000)
        return
      }
    } catch (e: Throwable) {
      // fall through to Old Arch
    }

    // ── Old Arch fallback (reactNativeHost.reactInstanceManager) ──────────
    try {
      val rim = app.reactNativeHost.reactInstanceManager
      val existing = rim.currentReactContext
      if (existing != null) {
        Log.d("SmsWatcher", "✅ ReactContext already ready (Old Arch), triggering JS")
        triggerWarmupService(context)
        return
      }

      rim.addReactInstanceEventListener(object : com.facebook.react.ReactInstanceManager.ReactInstanceEventListener {
        override fun onReactContextInitialized(reactContext: com.facebook.react.bridge.ReactContext) {
          rim.removeReactInstanceEventListener(this)
          Log.d("SmsWatcher", "✅ ReactContext initialized (Old Arch), triggering JS")
          triggerWarmupService(context)
        }
      })
      rim.createReactContextInBackground()
    } catch (e: Throwable) {
      Log.e("SmsWatcher", "❌ Failed to start ReactInstanceManager", e)
    }
  }

  companion object {
    var targetNumbers: MutableList<String> = mutableListOf()
  }

  override fun getName(): String = "SmsWatcherModule"

  @ReactMethod
  fun setTargetNumbers(nums: ReadableArray) {
    targetNumbers = nums.toArrayList().map { it.toString() }.toMutableList()
    saveNumbersToPrefs()
    warmUpAndTriggerFakeTask()
  }

  @ReactMethod
  fun addTargetNumber(num: String) {
    if (!targetNumbers.contains(num)) {
      targetNumbers.add(num)
      saveNumbersToPrefs()
      warmUpAndTriggerFakeTask()
    }
  }

  @ReactMethod
  fun removeTargetNumber(num: String) {
    if (targetNumbers.remove(num)) {
      saveNumbersToPrefs()
    }
  }

  @ReactMethod
  fun clearTargetNumbers() {
    targetNumbers.clear()
    saveNumbersToPrefs()
  }

  @ReactMethod
  fun getTargetNumbers(promise: Promise) {
    val arr: WritableArray = Arguments.createArray()
    for (num in targetNumbers) {
      arr.pushString(num)
    }
    promise.resolve(arr)
  }
}
