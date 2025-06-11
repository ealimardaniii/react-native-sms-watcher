package com.smswatcher

import android.content.Intent
import com.facebook.react.HeadlessJsTaskService
import com.facebook.react.bridge.Arguments
import com.facebook.react.jstasks.HeadlessJsTaskConfig

class SmsHeadlessService : HeadlessJsTaskService() {
  override fun getTaskConfig(intent: Intent?): HeadlessJsTaskConfig? {
    val data = intent?.getStringExtra("message") ?: return null

    val extras = Arguments.createMap().apply {
      putString("message", data)
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
