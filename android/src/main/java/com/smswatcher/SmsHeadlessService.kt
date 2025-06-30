package com.smswatcher

import android.content.Intent
import com.facebook.react.HeadlessJsTaskService
import com.facebook.react.bridge.Arguments
import com.facebook.react.jstasks.HeadlessJsTaskConfig

class SmsHeadlessService : HeadlessJsTaskService() {
  override fun getTaskConfig(intent: Intent?): HeadlessJsTaskConfig? {
  val message = intent?.getStringExtra("message") ?: return null
  val address = intent.getStringExtra("address") ?: "unknown"

  val extras = Arguments.createMap().apply {
    putString("message", message)
    putString("address", address)
  }

    // Return the Headless task configuration
    return HeadlessJsTaskConfig(
      "SmsBackgroundTask", 
      extras,
      5000,               
      true                
    )
  }
}
