package com.crashhandlerapp

import android.app.Application
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeApplicationEntryPoint.loadReactNative
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
// Adding NativeCrashHandler
import com.nativecrashhandler.CrashHandlerPackage
import com.nativecrashhandler.CrashHandler

class MainApplication : Application(), ReactApplication {

  override val reactHost: ReactHost by lazy {
    getDefaultReactHost(
      context = applicationContext,
      packageList =
        PackageList(this).packages.apply {
          // Packages that cannot be autolinked yet can be added manually here, for example:
          // add(MyReactNativePackage())
          add(CrashHandlerPackage())
        },
    )
  }

  private var currentActivity: android.app.Activity? = null
  private val crashHandler = CrashHandler()

  override fun onCreate() {
    super.onCreate()
    loadReactNative(this)

    // Initialize global crash handler once
    Thread.setDefaultUncaughtExceptionHandler(crashHandler)
    
    registerActivityLifecycleCallbacks(object : android.app.Application.ActivityLifecycleCallbacks {
      override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {
        currentActivity = activity
        crashHandler.updateActivity(activity)
      }
      override fun onActivityStarted(activity: android.app.Activity) { 
        currentActivity = activity
        crashHandler.updateActivity(activity)
      }
      override fun onActivityResumed(activity: android.app.Activity) { 
        currentActivity = activity
        crashHandler.updateActivity(activity)
      }
      override fun onActivityPaused(activity: android.app.Activity) {}
      override fun onActivityStopped(activity: android.app.Activity) {}
      override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
      override fun onActivityDestroyed(activity: android.app.Activity) {
        if (currentActivity == activity) {
            currentActivity = null
            crashHandler.updateActivity(null)
        }
      }
    })
  }
}
