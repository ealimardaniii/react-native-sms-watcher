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
    // Keep only digits — preserve full digit sequence so we can match by suffix.
    return number.filter { it.isDigit() }
  }

  /**
   * Returns true when the incoming sender matches one of the saved numbers.
   * Handles:
   *  - alphanumeric sender IDs (e.g. "BANK-MELLAT") — compared case-insensitively as raw strings
   *  - short codes (e.g. "5000125") — matched by exact digit equality
   *  - international vs. local formats — matched by suffix (last 7+ digits)
   */
  private fun isSenderWatched(rawSender: String, savedNumbers: Set<String>): Boolean {
    if (savedNumbers.isEmpty()) return false

    val senderDigits = normalizeNumber(rawSender)

    // Alphanumeric sender ID: no digits at all, or mostly letters.
    val senderIsAlphanumeric = rawSender.any { it.isLetter() }
    if (senderIsAlphanumeric || senderDigits.isEmpty()) {
      return savedNumbers.any { saved ->
        saved.equals(rawSender, ignoreCase = true) ||
          saved.trim().equals(rawSender.trim(), ignoreCase = true)
      }
    }

    return savedNumbers.any { saved ->
      val savedDigits = normalizeNumber(saved)
      if (savedDigits.isEmpty()) return@any false

      // Exact digit match (covers short codes and identical-length numbers).
      if (savedDigits == senderDigits) return@any true

      // Suffix match: compare the shorter digit string against the tail of the longer one.
      // Require at least 7 digits to avoid false positives (e.g. "1234" matching anything).
      val shorter = if (savedDigits.length <= senderDigits.length) savedDigits else senderDigits
      val longer = if (savedDigits.length <= senderDigits.length) senderDigits else savedDigits
      shorter.length >= 7 && longer.endsWith(shorter)
    }
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

      // Prefer originatingAddress (raw) over displayOriginatingAddress (may be reformatted).
      val sender = messages[0].originatingAddress
        ?: messages[0].displayOriginatingAddress
        ?: return

      val fullMessage = messages.joinToString("") { it.messageBody ?: "" }

      Log.d("SmsWatcher", "📩 Receiver triggered from: $sender")

      val prefs = context.getSharedPreferences("SmsWatcherPrefs", Context.MODE_PRIVATE)
      val savedNumbers = prefs.getStringSet("watchedNumbers", emptySet()) ?: emptySet()

      if (!isSenderWatched(sender, savedNumbers)) {
        Log.d("SmsWatcher", "↪︎ Sender $sender not in watched list (${savedNumbers.size} saved); ignoring.")
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
