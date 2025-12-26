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

  private fun warmUpAndTriggerFakeTask() {
    val context = reactApplicationContext
    val app = context.applicationContext as? ReactApplication

    if (app == null) {
      Log.w("SmsWatcher", "⚠ ReactApplication not found.")
      return
    }

    // New Arch: reactHost ممکنه nullable باشه
    val reactHost = app.reactHost
    if (reactHost == null) {
      Log.w("SmsWatcher", "⚠ reactHost is null (New Arch may be disabled or not available).")
      return
    }

    // ✅ درخواست آماده‌سازی RN (بدون ReactInstanceManager)
    try {
      reactHost.start()
    } catch (e: Exception) {
      Log.e("SmsWatcher", "❌ reactHost.start() failed", e)
      return
    }

    // مثل قبل: بعد از ۲ ثانیه اگر ReactContext آماده بود، سرویس رو trigger کن
    Handler(Looper.getMainLooper()).postDelayed({
      val currentReactContext = reactHost.currentReactContext
      if (currentReactContext != null) {
        Log.d("SmsWatcher", "✅ ReactContext is ready, manually triggering JS")
        val intent = Intent(context, SmsHeadlessService::class.java).apply {
          putExtra("message", "[warmup]")
          putExtra("address", "bootstrap")
        }
        try {
          context.startService(intent)
        } catch (e: Exception) {
          Log.e("SmsWatcher", "❌ Failed to trigger JS after ReactContext ready", e)
        }
      } else {
        Log.w("SmsWatcher", "⚠ Still no ReactContext after delay.")
      }
    }, 2000)
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
