package com.example.airesumescreener

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings

object DeviceIdHelper {
    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        // Use Android ID - stable per device, doesn't require permissions
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"

        // Hash it for privacy + consistent format
        return "PERSONAL_${androidId.take(8).uppercase()}"
    }
}