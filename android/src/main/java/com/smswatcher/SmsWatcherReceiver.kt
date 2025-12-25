package com.smswatcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsMessage
import com.facebook.react.HeadlessJsTaskService
import com.smswatcher.SmsWatcherModule
import android.util.Log

class SmsWatcherReceiver : BroadcastReceiver() {

  private fun normalizeNumber(number: String): String {
    val digits = number.filter { it.isDigit() }
    return if (digits.length > 10) digits.takeLast(10) else digits
  }

  override fun onReceive(context: Context, intent: Intent) {

    if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
      val pdus = intent.extras?.get("pdus") as? Array<*>
      if (pdus.isNullOrEmpty()) return

      val format = intent.extras?.getString("format")
      val messages = pdus.mapNotNull { pdu ->
        (pdu as? ByteArray)?.let { SmsMessage.createFromPdu(it, format) }
      }
      if (messages.isEmpty()) return

      val sender = messages[0].displayOriginatingAddress ?: return

      val fullMessage = messages.joinToString("") { it.messageBody }

      Log.d("SmsWatcher", "📩 Receiver triggered from: $sender")

      val prefs = context.getSharedPreferences("SmsWatcherPrefs", Context.MODE_PRIVATE)
      val savedNumbers = prefs.getStringSet("watchedNumbers", emptySet()) ?: emptySet()

      val normalizedSender = normalizeNumber(sender)
      val normalizedSaved = savedNumbers.map { normalizeNumber(it) }

      if (!normalizedSaved.any { it == normalizedSender }) {
        return
      }

      val serviceIntent = Intent(context, SmsHeadlessService::class.java).apply {
          putExtra("message", fullMessage)
          putExtra("address", sender)
      }

        try {
            context.startService(serviceIntent)
            HeadlessJsTaskService.acquireWakeLockNow(context)
            Log.d("SmsWatcher", "🚀 Headless service started with message: $fullMessage")
        } catch (e: Exception) {
            Log.e("SmsWatcher", "❌ Failed to start Headless JS task", e)
        }
    }
  }
}
