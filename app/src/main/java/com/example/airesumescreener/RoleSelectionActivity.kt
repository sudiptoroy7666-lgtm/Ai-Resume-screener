package com.example.airesumescreener

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.airesumescreener.databinding.ActivityRoleSelectionBinding

class RoleSelectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRoleSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnApplicant.setOnClickListener {
            startActivity(Intent(this, ApplicantPortalActivity::class.java))
        }

        binding.btnHr.setOnClickListener {
            startActivity(Intent(this, AdminActivity::class.java))
        }
    }
}