package com.example.airesumescreener

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.TypedValue
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.airesumescreener.databinding.ActivityCandidateReportBinding
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip

class CandidateReportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCandidateReportBinding
    private lateinit var candidate: Candidate

    private val TEAL_700 = Color.parseColor("#0F766E")
    private val GREEN_700 = Color.parseColor("#065F46")
    private val GREEN_50 = Color.parseColor("#D1FAE5")
    private val GREEN_BORDER = Color.parseColor("#A7F3D0")
    private val RED_700 = Color.parseColor("#991B1B")
    private val RED_50 = Color.parseColor("#FEE2E2")
    private val RED_BORDER = Color.parseColor("#FECACA")
    private val AMBER_700 = Color.parseColor("#92400E")
    private val AMBER_50 = Color.parseColor("#FEF3C7")
    private val AMBER_BORDER = Color.parseColor("#FDE68A")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCandidateReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topAppBar.setNavigationOnClickListener { finish() }

        candidate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("CANDIDATE", Candidate::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("CANDIDATE")
        } ?: run { finish(); return }

        binding.topAppBar.title = candidate.name

        populateUI()

        binding.btnPrint.setOnClickListener { printReport() }
    }

    private fun populateUI() {
        binding.progressScore.setProgress(candidate.score, true)
        binding.tvScoreText.text = "${candidate.score}%"

        when {
            candidate.score >= 80 -> {
                binding.verdictStrip.setBackgroundColor(Color.parseColor("#D1FAE5"))
                binding.tvVerdict.text = "STRONG MATCH"
                binding.tvVerdict.setTextColor(GREEN_700)
                binding.verdictDot.setBackgroundColor(Color.parseColor("#059669"))
            }
            candidate.score >= 60 -> {
                binding.verdictStrip.setBackgroundColor(AMBER_50)
                binding.tvVerdict.text = "MODERATE MATCH"
                binding.tvVerdict.setTextColor(AMBER_700)
                binding.verdictDot.setBackgroundColor(Color.parseColor("#D97706"))
            }
            else -> {
                binding.verdictStrip.setBackgroundColor(RED_50)
                binding.tvVerdict.text = "WEAK MATCH"
                binding.tvVerdict.setTextColor(RED_700)
                binding.verdictDot.setBackgroundColor(Color.parseColor("#DC2626"))
            }
        }

        binding.tvName.text = candidate.name
        binding.tvEmail.text = candidate.email.ifBlank { "Not provided" }
        binding.tvPhone.text = candidate.phone.ifBlank { "Not provided" }
        binding.tvAddress.text = candidate.address.ifBlank { "Not provided" }
        binding.tvSummary.text = candidate.summary.ifBlank { "No summary provided." }
        binding.tvExperience.text = candidate.experience.ifBlank { "Not specified" }
        binding.tvEducation.text = candidate.education.ifBlank { "Not specified" }

        addChips(binding.flexHardMatched, candidate.hardSkillsMatched, ChipType.MATCHED)
        addChips(binding.flexHardMissing, candidate.hardSkillsMissing, ChipType.MISSING)
        addChips(binding.flexSoftMatched, candidate.softSkillsMatched, ChipType.MATCHED)
        addChips(binding.flexSoftMissing, candidate.softSkillsMissing, ChipType.MISSING)

        binding.formatCount.text = candidate.formattingIssues.size.toString()
        if (candidate.formattingIssues.isEmpty()) {
            binding.flexFormatting.removeAllViews()
            val tv = TextView(this).apply {
                text = "No ATS formatting issues detected."
                setTextColor(GREEN_700)
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            binding.flexFormatting.addView(tv)
        } else {
            addChips(binding.flexFormatting, candidate.formattingIssues, ChipType.ISSUE)
        }
    }

    private fun addChips(flexbox: FlexboxLayout, items: List<String>, type: ChipType) {
        flexbox.removeAllViews()
        if (items.isEmpty()) {
            val tv = TextView(this).apply {
                text = "None"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 13f
            }
            flexbox.addView(tv)
            return
        }

        for (item in items) {
            val chip = Chip(this).apply {
                text = item
                isClickable = false
                isCheckable = false
                chipMinHeight = 32f
                textSize = 13f
                chipStartPadding = 14f
                chipEndPadding = 14f

                when (type) {
                    ChipType.MATCHED -> {
                        chipBackgroundColor = android.content.res.ColorStateList.valueOf(GREEN_50)
                        setTextColor(GREEN_700)
                        chipStrokeColor = android.content.res.ColorStateList.valueOf(GREEN_BORDER)
                        chipStrokeWidth = 1f
                    }
                    ChipType.MISSING -> {
                        chipBackgroundColor = android.content.res.ColorStateList.valueOf(RED_50)
                        setTextColor(RED_700)
                        chipStrokeColor = android.content.res.ColorStateList.valueOf(RED_BORDER)
                        chipStrokeWidth = 1f
                    }
                    ChipType.ISSUE -> {
                        chipBackgroundColor = android.content.res.ColorStateList.valueOf(AMBER_50)
                        setTextColor(AMBER_700)
                        chipStrokeColor = android.content.res.ColorStateList.valueOf(AMBER_BORDER)
                        chipStrokeWidth = 1f
                    }
                }
            }
            flexbox.addView(chip)
        }
    }

    enum class ChipType { MATCHED, MISSING, ISSUE }

    private fun printReport() {
        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "Candidate Report - ${candidate.name}"

        val webView = WebView(this)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printAdapter = view!!.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        }

        val html = """
            <html><body style="font-family: Arial, sans-serif; color: #0F172A; padding: 20px;">
                <h1 style="border-bottom: 3px solid #0F766E; padding-bottom: 10px;">${candidate.name}</h1>
                <p><strong>Email:</strong> ${candidate.email}</p>
                <p><strong>Phone:</strong> ${candidate.phone}</p>
                <p><strong>Address:</strong> ${candidate.address}</p>
                
                <h2>ATS Score: ${candidate.score}%</h2>
                <p><strong>Summary:</strong> ${candidate.summary}</p>
                
                <h2>Experience</h2>
                <p>${candidate.experience}</p>
                
                <h2>Education</h2>
                <p>${candidate.education}</p>
                
                <h2>Hard Skills</h2>
                <h3>Matched (${candidate.hardSkillsMatched.size})</h3>
                <ul>${candidate.hardSkillsMatched.joinToString("") { "<li>$it</li>" }}</ul>
                <h3>Missing (${candidate.hardSkillsMissing.size})</h3>
                <ul>${candidate.hardSkillsMissing.joinToString("") { "<li>$it</li>" }}</ul>
                
                <h2>Soft Skills</h2>
                <h3>Matched (${candidate.softSkillsMatched.size})</h3>
                <ul>${candidate.softSkillsMatched.joinToString("") { "<li>$it</li>" }}</ul>
                <h3>Missing (${candidate.softSkillsMissing.size})</h3>
                <ul>${candidate.softSkillsMissing.joinToString("") { "<li>$it</li>" }}</ul>
                
                <h2>Formatting Issues (${candidate.formattingIssues.size})</h2>
                <p>${if(candidate.formattingIssues.isEmpty()) "No major formatting issues detected." else candidate.formattingIssues.joinToString(", ")}</p>
                
                <hr style="margin-top: 40px;" />
                <p style="text-align: center; font-size: 10px; color: #666;">Generated by ResumeAI Enterprise Portal</p>
            </body></html>
        """.trimIndent()

        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
    }
}