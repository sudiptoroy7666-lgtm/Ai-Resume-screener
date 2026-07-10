package com.example.airesumescreener

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.airesumescreener.databinding.ActivitySplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Install the core splash screen API to handle the initial window draw
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // 2. Inflate our custom branded layout
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 3. Keep the native splash screen on until our UI is fully drawn
        var keepSplashOnScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        // 4. Add smooth fade-in animations for the UI elements
        binding.ivLogo.alpha = 0f
        binding.tvAppName.alpha = 0f
        binding.tvTagline.alpha = 0f

        binding.ivLogo.animate().alpha(1f).setDuration(600).start()
        binding.tvAppName.animate().alpha(1f).setDuration(600).setStartDelay(200).start()
        binding.tvTagline.animate().alpha(0.8f).setDuration(600).setStartDelay(400).start()

        // 5. Hold the screen for a moment, then navigate to Main
        lifecycleScope.launch {
            delay(1500) // 1.5 seconds delay so the user sees the branding

            keepSplashOnScreen = false // Fades out the native window splash

            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }
    }
}