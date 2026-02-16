package com.nativecrashhandler

import android.app.Activity
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.Window
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CrashHandler : Thread.UncaughtExceptionHandler {
    private val TAG = "CrashHandler"
    private var activity: Activity? = null
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    fun updateActivity(activity: Activity?) {
        this.activity = activity
    }

    companion object {
        private const val SCREENSHOT_DIR = "crash_screenshots"
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        Log.d(TAG, "!!! uncaughtException triggered on thread: ${thread.name} !!!")
        try {
            handleCrash(throwable, "Native")
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in CrashHandler logic: ${e.message}")
            e.printStackTrace()
        } finally {
            Log.d(TAG, "CrashHandler work finished, passing to default handler.")
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun handleCrash(throwable: Throwable, type: String) {
        Log.d(TAG, "==== CRASH DETECTED [$type] ====")
        Log.d(TAG, "Throwable: ${throwable.message}")

        val bitmap = captureScreenshot()
        if (bitmap != null) {
            Log.d(TAG, "Screenshot captured successfully, now saving...")
            val file = saveScreenshotToFile(bitmap, throwable)
            if (file != null) {
                Log.d(TAG, "SUCCESS: Screenshot saved at: ${file.absolutePath}")
                Log.d(TAG, "File Size: ${file.length()} bytes")
            } else {
                Log.e(TAG, "ERROR: Failed to save bitmap to file")
            }
        } else {
            Log.e(TAG, "ERROR: Screenshot capture returned null bitmap")
        }
    }

    private fun captureScreenshot(): Bitmap? {
        Log.d(TAG, "captureScreenshot called on thread: ${Thread.currentThread().name}")
        val window: Window = activity?.window ?: run {
            Log.e(TAG, "Capture failed: activity or window is null")
            return null
        }
        
        val decorView = window.decorView
        val width = decorView.width
        val height = decorView.height
        
        if (width <= 0 || height <= 0) {
            Log.e(TAG, "Capture failed: window dimensions are 0 (width: $width, height: $height)")
            return null
        }
        
        Log.d(TAG, "Creating bitmap of size ${width}x$height")
        val bitmap = try {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Capture failed: OutOfMemoryError")
            return null
        }
        
        val handlerThread = android.os.HandlerThread("PixelCopyThread")
        handlerThread.start()
        val handler = Handler(handlerThread.looper)
        
        val latch = java.util.concurrent.CountDownLatch(1)
        var pixelCopyResult = -1
        
        try {
            Log.d(TAG, "Requesting PixelCopy on background thread...")
            PixelCopy.request(window, bitmap, { copyResult ->
                pixelCopyResult = copyResult
                Log.d(TAG, "PixelCopy callback received with result code: $copyResult")
                latch.countDown()
            }, handler)
            
            // Wait for completion (up to 5 seconds)
            val success = latch.await(5000, java.util.concurrent.TimeUnit.MILLISECONDS)
            if (!success) {
                Log.e(TAG, "Capture failed: PixelCopy timed out after 5s")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Capture failed: PixelCopy exception: ${e.message}")
        } finally {
            handlerThread.quitSafely()
        }
        
        return if (pixelCopyResult == PixelCopy.SUCCESS) bitmap else {
            Log.e(TAG, "Capture failed: PixelCopy was not successful (result: $pixelCopyResult)")
            null
        }
    }

    private fun saveScreenshotToFile(bitmap: Bitmap, throwable: Throwable): File? {
        val context = activity?.applicationContext ?: return null
        return try {
            val screenshotsDir = File(context.getExternalFilesDir(null), SCREENSHOT_DIR).apply {
                if (!exists()) {
                    Log.d(TAG, "Creating directory: ${absolutePath}")
                    mkdirs()
                }
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                .format(Date())
            val exceptionName = throwable.javaClass.simpleName
            val filename = "${timestamp}_${exceptionName}.jpg"
            val screenshotFile = File(screenshotsDir, filename)

            Log.d(TAG, "Attempting to write JPEG to: ${screenshotFile.absolutePath}")
            FileOutputStream(screenshotFile).use { out ->
                val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
                Log.d(TAG, "Compression result: $compressed")
            }
            
            Log.d(TAG, "File write complete. Size: ${screenshotFile.length()} bytes")
            
            // Log directory contents for absolute verification
            Log.d(TAG, "--- Directory Listing: $SCREENSHOT_DIR ---")
            screenshotsDir.listFiles()?.forEach { f ->
                Log.d(TAG, "File: ${f.name} (${f.length()} bytes)")
            }
            Log.d(TAG, "------------------------------------------")
            
            screenshotFile
        } catch (e: Exception) {
            Log.e(TAG, "Error saving file: ${e.message}")
            null
        }
    }
}