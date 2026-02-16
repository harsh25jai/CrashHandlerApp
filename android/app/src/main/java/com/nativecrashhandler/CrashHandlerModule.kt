package com.nativecrashhandler

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.annotations.ReactModule
import android.util.Log

@ReactModule(name = CrashHandlerModule.NAME)
class CrashHandlerModule(reactContext: ReactApplicationContext) : NativeCrashHandlerSpec(reactContext) {

    override fun getName(): String = NAME

    override fun captureJSException(stack: String) {
        val activity = reactApplicationContext.currentActivity
        val crashHandler = CrashHandler().apply { updateActivity(activity) }
        
        // Handle the crash on a background thread to avoid blocking JS
        Thread {
            val exception = RuntimeException("JavaScript Exception: $stack")
            crashHandler.handleCrash(exception, "JavaScript")
        }.start()
    }

    // For testing purposes
    override fun triggerNativeCrash() {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Log.d("CrashHandlerModule", "Manual Native Crash triggered from JS (Main Thread)")
            throw RuntimeException("Manual Native Crash triggered from JS (Main Thread)")
        }
    }

    companion object {
        const val NAME = "NativeCrashHandler"
    }
}