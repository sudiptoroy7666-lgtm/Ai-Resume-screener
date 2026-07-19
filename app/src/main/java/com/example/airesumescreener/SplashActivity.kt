package com.example.airesumescreener

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.airesumescreener.databinding.ActivitySplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // ❌ DO NOT CALL installSplashScreen() – it is removed completely
        super.onCreate(savedInstanceState)

        // Inflate custom branded layout
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Fade-in animations for your UI elements
        binding.ivLogo.alpha = 0f
        binding.tvAppName.alpha = 0f
        binding.tvTagline.alpha = 0f

        binding.ivLogo.animate().alpha(1f).setDuration(600).start()
        binding.tvAppName.animate().alpha(1f).setDuration(600).setStartDelay(200).start()
        binding.tvTagline.animate().alpha(0.8f).setDuration(600).setStartDelay(400).start()

        // Hold for a moment, then navigate to MainActivity
        lifecycleScope.launch {
            delay(1500)
            // Navigate to role selection (not MainActivity)
            startActivity(Intent(this@SplashActivity, RoleSelectionActivity::class.java))
            finish()
        }
    }
}