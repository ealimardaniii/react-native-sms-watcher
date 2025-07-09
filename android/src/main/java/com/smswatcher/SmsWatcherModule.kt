package com.smswatcher

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableArray
import android.content.Context
import android.util.Log
import com.facebook.react.ReactApplication
import com.facebook.react.ReactInstanceManager

class SmsWatcherModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private val prefs = reactContext.getSharedPreferences("SmsWatcherPrefs", Context.MODE_PRIVATE)

    companion object {
        var targetNumbers: MutableList<String> = mutableListOf()
    }

    init {
        val saved = prefs.getStringSet("watchedNumbers", null)
        if (saved != null) {
            targetNumbers = saved.toMutableList()
        }
    }

    private fun warmUpReactContext() {
    val application = reactApplicationContext.applicationContext as ReactApplication
    val reactInstanceManager = application.reactNativeHost.reactInstanceManager

    if (!reactInstanceManager.hasStartedCreatingInitialContext()) {
        Log.d("SmsWatcher", "🚀 Warming up React Context...")
        reactInstanceManager.createReactContextInBackground()
    }
}

    private fun saveNumbersToPrefs() {
        prefs.edit().putStringSet("watchedNumbers", targetNumbers.toSet()).apply()
    }

    override fun getName(): String = "SmsWatcherModule"

    @ReactMethod
    fun setTargetNumbers(nums: ReadableArray) {
        targetNumbers = nums.toArrayList().map { it.toString() }.toMutableList()
        saveNumbersToPrefs()
        warmUpReactContext()
    }

    @ReactMethod
    fun addTargetNumber(num: String) {
        if (!targetNumbers.contains(num)) {
            targetNumbers.add(num)
            saveNumbersToPrefs()
            warmUpReactContext()
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