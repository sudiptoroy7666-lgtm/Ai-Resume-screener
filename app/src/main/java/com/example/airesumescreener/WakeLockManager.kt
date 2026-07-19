package com.example.airesumescreener

import android.app.Activity
import android.os.PowerManager
import android.view.WindowManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class WakeLockManager(private val activity: Activity) : DefaultLifecycleObserver {
    private var wakeLock: PowerManager.WakeLock? = null

    fun acquire() {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val pm = activity.getSystemService(Activity.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ResumeAI:Batch")
            .apply { setReferenceCounted(false); acquire(30 * 60 * 1000L) }
    }

    fun release() {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    override fun onDestroy(owner: LifecycleOwner) = release()
}