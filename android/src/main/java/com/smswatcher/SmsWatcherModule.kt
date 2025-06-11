package com.smswatcher

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableArray

class SmsWatcherModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    companion object {
        var targetNumbers: MutableList<String> = mutableListOf()
    }

    override fun getName(): String = "SmsWatcherModule"

    @ReactMethod
    fun setTargetNumbers(nums: ReadableArray) {
        targetNumbers = nums.toArrayList().map { it.toString() }.toMutableList()
    }

    @ReactMethod
    fun addTargetNumber(num: String) {
        if (!targetNumbers.contains(num)) {
        targetNumbers.add(num)
        }
    }

    @ReactMethod
    fun removeTargetNumber(num: String) {
        targetNumbers.remove(num)
    }

    @ReactMethod
    fun clearTargetNumbers() {
        targetNumbers.clear()
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
