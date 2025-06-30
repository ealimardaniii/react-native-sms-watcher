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

class SmsWatcherReceiver : BroadcastReceiver() {
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

      if (!SmsWatcherModule.targetNumbers.any { sender.contains(it) }) {
        return
      }

      val fullMessage = messages.joinToString("") { it.messageBody }

      val serviceIntent = Intent(context, SmsHeadlessService::class.java).apply {
          putExtra("message", fullMessage)
          putExtra("address", sender)
      }
      context.startService(serviceIntent)
      HeadlessJsTaskService.acquireWakeLockNow(context)
    }
  }
}
