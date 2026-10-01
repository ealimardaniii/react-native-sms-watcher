package com.smswatcher

import android.content.Intent
import android.util.Log
import com.facebook.react.HeadlessJsTaskService
import com.facebook.react.bridge.Arguments
import com.facebook.react.jstasks.HeadlessJsTaskConfig

class SmsHeadlessService : HeadlessJsTaskService() {

  /**
   * Always hand the task to RN. If the React instance isn't running yet (e.g. the process was
   * cold-started by the SMS broadcast), HeadlessJsTaskService.startTask() starts it — on both the
   * New Arch (reactHost) and Old Arch (ReactInstanceManager) — and runs the task once the context
   * is initialized, no matter how long that takes.
   */
  override fun getTaskConfig(intent: Intent?): HeadlessJsTaskConfig? {
    val message = intent?.getStringExtra("message") ?: return null
    val address = intent.getStringExtra("address") ?: ""

    Log.d("SmsWatcher", "✅ HeadlessJsTaskConfig created with message: $message")
    val data = Arguments.createMap().apply {
      putString("message", message)
      putString("address", address)
    }
    return HeadlessJsTaskConfig(
      "SmsBackgroundTask",
      data,
      30000,
      true
    )
  }
}
