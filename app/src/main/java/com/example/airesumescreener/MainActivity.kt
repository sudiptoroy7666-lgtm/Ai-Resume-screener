package com.example.airesumescreener

import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.airesumescreener.databinding.ActivityMainBinding
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.pow

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai/"
const val MODEL_ID = "gemini-3.5-flash"

// Add these instead:
const val PERSONAL_JOB_ID = "QUICK_SCAN"
// PERSONAL_ORG_ID is now dynamic per device

// Unified response structure matching Admin Portal
data class CandidateAnalysisResult(
    val score: Int = 0,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val education: String = "",
    val experience: String = "",
    val hardSkillsMatched: List<String> = emptyList(),
    val hardSkillsMissing: List<String> = emptyList(),
    val softSkillsMatched: List<String> = emptyList(),
    val softSkillsMissing: List<String> = emptyList(),
    val formattingIssues: List<String> = emptyList(),
    val summary: String = ""
)

interface CerebrasApi {
    @POST("chat/completions")
    suspend fun analyze(
        @Header("Authorization") auth: String,
        @Body request: ChatRequest
    ): retrofit2.Response<ChatResponse>
}

class RetryInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var tryCount = 0
        val maxRetries = 3
        while (tryCount < maxRetries) {
            try {
                response?.close()
                response = chain.proceed(request)
                val statusCode = response.code
                if (response.isSuccessful || (statusCode != 429 && statusCode < 500)) return response
            } catch (e: IOException) {
                if (tryCount == maxRetries - 1) throw e
            }
            tryCount++
            val backoff = (2.0.pow(tryCount.toDouble()) * 1000).toLong()
            try { Thread.sleep(backoff) } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException("Interrupted during retry backoff", e)
            }
        }
        return response ?: throw IOException("Failed to get response after retries")
    }
}

class MainActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_APPLICANT_MODE = "applicant_mode"
    }
    private lateinit var binding: ActivityMainBinding
    private var resumeUri: Uri? = null
    private var fileName: String = ""
    private var fileSize: Long = 0
    private var currentResult: CandidateAnalysisResult? = null

    private val TEAL_700 = Color.parseColor("#0F766E")
    private val TEAL_500 = Color.parseColor("#0D9488")
    private val SLATE_300 = Color.parseColor("#CBD5E1")
    private val SLATE_400 = Color.parseColor("#94A3B8")
    private val SLATE_500 = Color.parseColor("#64748B")
    private val SLATE_800 = Color.parseColor("#1E293B")
    private val GREEN_700 = Color.parseColor("#065F46")
    private val GREEN_50 = Color.parseColor("#D1FAE5")
    private val GREEN_BORDER = Color.parseColor("#A7F3D0")
    private val RED_700 = Color.parseColor("#991B1B")
    private val RED_50 = Color.parseColor("#FEE2E2")
    private val RED_BORDER = Color.parseColor("#FECACA")
    private val AMBER_700 = Color.parseColor("#92400E")
    private val AMBER_500 = Color.parseColor("#D97706")
    private val AMBER_50 = Color.parseColor("#FEF3C7")
    private val AMBER_BORDER = Color.parseColor("#FDE68A")
    private val GREEN_BG = Color.parseColor("#D1FAE5")
    private val RED_50_BG = Color.parseColor("#FEE2E2")
    private val GREEN_CHECK = Color.parseColor("#059669")
    private val RED_CROSS = Color.parseColor("#DC2626")

    private val excelExportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri: Uri? ->
        uri?.let { exportToExcel(it) }
    }

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            resumeUri = it
            fileName = getFileName(it)
            fileSize = getFileSize(it)

            showFileUploadedState()
            activateStep(2)
            validateForm()

            Toast.makeText(this, "✅ File uploaded successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private val cerebrasApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .callTimeout(180, TimeUnit.SECONDS)
                .addInterceptor(RetryInterceptor())
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(CerebrasApi::class.java)

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        PDFBoxResourceLoader.init(applicationContext)

        val isApplicantMode = intent.getBooleanExtra(EXTRA_APPLICANT_MODE, false)

// Inflate menu manually to control visibility
        binding.topAppBar.menu.clear()
        menuInflater.inflate(R.menu.main_menu, binding.topAppBar.menu)

        if (isApplicantMode) {
            // Hide HR-specific items for applicants
            binding.topAppBar.menu.findItem(R.id.action_submit_to_hr)?.isVisible = false
            binding.topAppBar.menu.findItem(R.id.action_admin)?.isVisible = false
            binding.topAppBar.title = "ATS Resume Scanner"
            binding.topAppBar.subtitle = "Check your resume"
        }

        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    true
                }
                R.id.action_submit_to_hr -> {
                    startActivity(Intent(this, ApplicantPortalActivity::class.java))
                    true
                }
                R.id.action_admin -> {
                    startActivity(Intent(this, AdminActivity::class.java))
                    true
                }
                else -> false
            }
        }

        binding.btnUploadResume.setOnClickListener {
            if (resumeUri != null) {
                AlertDialog.Builder(this)
                    .setTitle("Change File")
                    .setMessage("Do you want to upload a different resume?")
                    .setPositiveButton("Change File") { _, _ ->
                        resetFileUploadState()
                        filePickerLauncher.launch(arrayOf(
                            "application/pdf",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        ))
                    }
                    .setNegativeButton("Keep Current", null)
                    .show()
            } else {
                filePickerLauncher.launch(arrayOf(
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                ))
            }
        }

        binding.btnRemoveFile.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Remove File")
                .setMessage("Remove the uploaded resume?")
                .setPositiveButton("Remove") { _, _ ->
                    resetFileUploadState()
                    deactivateStep(2)
                    deactivateStep(3)
                    validateForm()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.etJobDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val len = s?.trim()?.length ?: 0
                if (len > 20 && resumeUri != null) activateStep(3) else deactivateStep(3)
                if (len > 20 && resumeUri != null) activateStep(2) else if (resumeUri == null) deactivateStep(2)
                validateForm()
            }
        })

        binding.btnAnalyze.setOnClickListener {
            val jobDesc = binding.etJobDescription.text?.trim().toString()
            if (resumeUri == null || jobDesc.length < 20) {
                showError("Please upload a resume and enter a job description (at least 20 characters).")
                return@setOnClickListener
            }
            analyzeResume(jobDesc)
        }

        binding.btnExport.setOnClickListener {
            if (currentResult == null) {
                showError("No results to export yet.")
                return@setOnClickListener
            }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            excelExportLauncher.launch("ATS_Report_${safeName}_$timestamp.xlsx")
        }

        binding.btnReset.setOnClickListener { resetForm() }
    }

    private fun showFileUploadedState() {
        binding.btnUploadResume.apply {
            text = "✓ File uploaded successfully"
            setTextColor(GREEN_700)
            backgroundTintList = ColorStateList.valueOf(GREEN_50)
            strokeColor = ColorStateList.valueOf(GREEN_BORDER)
            icon = resources.getDrawable(android.R.drawable.checkbox_on_background, theme)
            iconTint = ColorStateList.valueOf(GREEN_700)
            alpha = 0.9f
        }

        binding.fileInfoContainer.visibility = View.VISIBLE
        binding.tvFileName.text = fileName

        val sizeStr = when {
            fileSize < 1024 -> "$fileSize B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
            else -> "${fileSize / (1024 * 1024)} MB"
        }

        val extension = getExtension(resumeUri!!, fileName).uppercase()
        binding.tvFileInfo.text = "$extension • $sizeStr"
    }
    private fun getPersonalOrgId(): String {
        return DeviceIdHelper.getDeviceId(this)
    }
    private fun resetFileUploadState() {
        resumeUri = null
        fileName = ""
        fileSize = 0

        binding.btnUploadResume.apply {
            text = "Tap to upload or drag a file here"
            setTextColor(TEAL_700)
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F0FDFA"))
            strokeColor = ColorStateList.valueOf(Color.parseColor("#99F6E4"))
            icon = resources.getDrawable(R.drawable.baseline_cloud_upload_24, theme)
            iconTint = ColorStateList.valueOf(TEAL_500)
            alpha = 1f
        }

        binding.fileInfoContainer.visibility = View.GONE
        binding.tvFileName.text = ""
        binding.tvFileInfo.text = ""
    }

    private fun getFileSize(uri: Uri): Long {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndexOrThrow(OpenableColumns.SIZE)
                    cursor.getLong(sizeIndex)
                } else 0L
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun activateStep(step: Int) {
        when (step) {
            2 -> {
                binding.stepBadge2.backgroundTintList = ColorStateList.valueOf(TEAL_700)
                binding.stepBadge2.setTextColor(Color.WHITE)
                binding.stepBadge2.alpha = 1f
                binding.stepLabel2.alpha = 1f
                binding.stepLabel2.setTextColor(SLATE_800)
                binding.stepLine1.setBackgroundColor(TEAL_500)
            }
            3 -> {
                binding.stepBadge3.backgroundTintList = ColorStateList.valueOf(TEAL_700)
                binding.stepBadge3.setTextColor(Color.WHITE)
                binding.stepBadge3.alpha = 1f
                binding.stepLabel3.alpha = 1f
                binding.stepLabel3.setTextColor(SLATE_800)
                binding.stepLine2.setBackgroundColor(TEAL_500)
            }
        }
    }

    private fun deactivateStep(step: Int) {
        when (step) {
            2 -> {
                binding.stepBadge2.backgroundTintList = ColorStateList.valueOf(SLATE_300)
                binding.stepBadge2.setTextColor(SLATE_500)
                binding.stepBadge2.alpha = 0.45f
                binding.stepLabel2.alpha = 0.45f
                binding.stepLabel2.setTextColor(SLATE_500)
                binding.stepLine1.setBackgroundColor(SLATE_300)
            }
            3 -> {
                binding.stepBadge3.backgroundTintList = ColorStateList.valueOf(SLATE_300)
                binding.stepBadge3.setTextColor(SLATE_500)
                binding.stepBadge3.alpha = 0.45f
                binding.stepLabel3.alpha = 0.45f
                binding.stepLabel3.setTextColor(SLATE_500)
                binding.stepLine2.setBackgroundColor(SLATE_300)
            }
        }
    }

    private fun validateForm() {
        val hasFile = resumeUri != null
        val hasJD = binding.etJobDescription.text?.trim()?.length ?: 0 > 20
        binding.btnAnalyze.isEnabled = hasFile && hasJD
        binding.btnAnalyze.alpha = if (hasFile && hasJD) 1f else 0.5f
    }

    private fun analyzeResume(jobDescription: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.cardResults.visibility = View.GONE
        binding.btnAnalyze.isEnabled = false
        binding.btnAnalyze.alpha = 0.5f

        lifecycleScope.launch {
            try {
                val rawResumeText = withContext(Dispatchers.IO) { extractText(resumeUri!!) }

                if (rawResumeText.isBlank()) {
                    showError("Failed to extract text. Ensure the file is a valid PDF/DOC/DOCX.")
                    return@launch
                }

                val cleanedResumeText = cleanAndTruncateText(rawResumeText)

                // ✅ QUALITY CHECK: Detect poor extraction
                val wordCount = cleanedResumeText.split(Regex("\\s+")).filter { it.isNotEmpty() }.size
                val hasGoodContent = wordCount >= 50 && cleanedResumeText.length >= 200

                if (!hasGoodContent) {
                    withContext(Dispatchers.Main) {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("⚠️ Extraction Quality Warning")
                            .setMessage(
                                "The extracted text seems incomplete (only $wordCount words). " +
                                        "This can happen with:\n\n" +
                                        "• Scanned/image-based PDFs\n" +
                                        "• Complex multi-column layouts\n" +
                                        "• Password-protected files\n\n" +
                                        "The AI analysis may be inaccurate. Do you want to continue?"
                            )
                            .setPositiveButton("Continue Anyway") { _, _ ->
                                proceedWithAnalysis(cleanedResumeText)
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                    return@launch
                }

                proceedWithAnalysis(cleanedResumeText)

            } catch (e: JsonSyntaxException) {
                showError("AI returned malformed data. Please try again.")
            } catch (e: Exception) {
                e.printStackTrace()
                FirebaseCrashlytics.getInstance().recordException(e)
                showError("System Error: ${e.message ?: "Unknown"}")
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnAnalyze.isEnabled = true
                binding.btnAnalyze.alpha = 1f
            }
        }
    }

    private fun updateUI(result: CandidateAnalysisResult) {
        binding.cardResults.visibility = View.VISIBLE

        when {
            result.score >= 80 -> {
                binding.verdictStrip.setBackgroundColor(GREEN_BG)
                binding.tvVerdict.text = "STRONG MATCH"
                binding.tvVerdict.setTextColor(GREEN_700)
                binding.verdictDot.setBackgroundColor(GREEN_CHECK)
            }
            result.score >= 60 -> {
                binding.verdictStrip.setBackgroundColor(AMBER_50)
                binding.tvVerdict.text = "MODERATE MATCH"
                binding.tvVerdict.setTextColor(AMBER_700)
                binding.verdictDot.setBackgroundColor(AMBER_500)
            }
            else -> {
                binding.verdictStrip.setBackgroundColor(RED_50_BG)
                binding.tvVerdict.text = "WEAK MATCH"
                binding.tvVerdict.setTextColor(RED_700)
                binding.verdictDot.setBackgroundColor(RED_CROSS)
            }
        }

        binding.progressScore.setProgress(result.score, true)
        binding.tvScoreText.text = "${result.score}%"
        binding.tvSummary.text = result.summary.ifBlank { "No summary provided." }

        val totalMatched = result.hardSkillsMatched.size + result.softSkillsMatched.size
        val totalMissing = result.hardSkillsMissing.size + result.softSkillsMissing.size
        val totalIssues = result.formattingIssues.size

        binding.statMatched.text = totalMatched.toString()
        binding.statMissing.text = totalMissing.toString()
        binding.statIssues.text = totalIssues.toString()

        binding.hardMatchCount.text = "${result.hardSkillsMatched.size} found"
        binding.hardMissCount.text = "${result.hardSkillsMissing.size} missing"
        binding.softMatchCount.text = "${result.softSkillsMatched.size} found"
        binding.softMissCount.text = "${result.softSkillsMissing.size} missing"

        addChips(binding.flexHardMatched, result.hardSkillsMatched, ChipType.MATCHED)
        addChips(binding.flexHardMissing, result.hardSkillsMissing, ChipType.MISSING)
        addChips(binding.flexSoftMatched, result.softSkillsMatched, ChipType.MATCHED)
        addChips(binding.flexSoftMissing, result.softSkillsMissing, ChipType.MISSING)

        binding.formatCount.text = totalIssues.toString()
        if (result.formattingIssues.isEmpty()) {
            binding.flexFormatting.removeAllViews()
            val tv = TextView(this).apply {
                text = "No ATS formatting issues detected."
                setTextColor(GREEN_700)
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            binding.flexFormatting.addView(tv)
        } else {
            addChips(binding.flexFormatting, result.formattingIssues, ChipType.ISSUE)
        }

        // ✅ Save to local history as backup (still works offline)
        val record = ScanRecord(
            fileName = fileName, score = result.score, summary = result.summary,
            hardSkillsMatched = result.hardSkillsMatched, hardSkillsMissing = result.hardSkillsMissing
        )
        HistoryManager(this).saveScan(record)
    }
    private fun proceedWithAnalysis(cleanedResumeText: String) {
        val jobDescription = binding.etJobDescription.text?.trim().toString()
        val cleanedJobDesc = cleanAndTruncateText(jobDescription)

        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.cardResults.visibility = View.GONE
                binding.btnAnalyze.isEnabled = false
                binding.btnAnalyze.alpha = 0.5f

                val prompt = """
                You are an expert ATS (Applicant Tracking System) analyzer.
                
                JOB REQUIREMENTS:
                ${cleanedJobDesc}
                
                CANDIDATE RESUME:
                ${cleanedResumeText}
                
                Compare the resume against the job requirements and output ONLY JSON:
                {
                  "score": <int 0-100>,
                  "name": "<extracted>",
                  "email": "<extracted>",
                  "phone": "<extracted>",
                  "address": "<extracted>",
                  "education": "<brief summary>",
                  "experience": "<brief summary>",
                  "hard_skills_matched": [],
                  "hard_skills_missing": [],
                  "soft_skills_matched": [],
                  "soft_skills_missing": [],
                  "formatting_issues": [],
                  "summary": "<1-2 sentence assessment>"
                }
            """.trimIndent()

                val request = ChatRequest(
                    model = MODEL_ID,
                    messages = listOf(
                        Message("system", "You are an expert ATS analyzer. Output only valid JSON."),
                        Message("user", prompt)
                    )
                )

                val response = withContext(Dispatchers.IO) {
                    cerebrasApi.analyze("Bearer ${BuildConfig.CEREBRAS_API_KEY}", request)
                }

                if (!response.isSuccessful) {
                    showError("API Error ${response.code()}: ${response.errorBody()?.string() ?: "Unknown"}")
                    return@launch
                }

                val rawContent = response.body()?.choices?.firstOrNull()?.message?.content ?: ""
                val jsonString = extractJsonObject(rawContent) ?: throw JsonSyntaxException("No valid JSON found.")
                val jsonObject = JsonParser.parseString(jsonString).asJsonObject

                val result = CandidateAnalysisResult(
                    score = jsonObject.optI("score"),
                    name = jsonObject.optS("name"),
                    email = jsonObject.optS("email"),
                    phone = jsonObject.optS("phone"),
                    address = jsonObject.optS("address"),
                    education = jsonObject.optS("education"),
                    experience = jsonObject.optS("experience"),
                    hardSkillsMatched = jsonObject.optL("hard_skills_matched"),
                    hardSkillsMissing = jsonObject.optL("hard_skills_missing"),
                    softSkillsMatched = jsonObject.optL("soft_skills_matched"),
                    softSkillsMissing = jsonObject.optL("soft_skills_missing"),
                    formattingIssues = jsonObject.optL("formatting_issues"),
                    summary = jsonObject.optS("summary")
                )

                currentResult = result
                withContext(Dispatchers.Main) { updateUI(result) }
                withContext(Dispatchers.IO) { savePersonalScanToFirestore(result) }

            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                withContext(Dispatchers.Main) {
                    showError("Analysis failed: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnAnalyze.isEnabled = true
                    binding.btnAnalyze.alpha = 1f
                }
            }
        }
    }
    private fun addChips(flexbox: FlexboxLayout, items: List<String>, type: ChipType) {
        flexbox.removeAllViews()
        if (items.isEmpty()) {
            flexbox.addView(TextView(this).apply { text = "None"; setTextColor(SLATE_400); textSize = 13f })
            return
        }
        for ((index, item) in items.withIndex()) {
            val chip = Chip(this).apply {
                text = item; isClickable = false; isCheckable = false
                chipMinHeight = 32f; textSize = 13f; chipStartPadding = 14f; chipEndPadding = 14f
                when (type) {
                    ChipType.MATCHED -> {
                        chipBackgroundColor = ColorStateList.valueOf(GREEN_50)
                        setTextColor(GREEN_700)
                        chipStrokeColor = ColorStateList.valueOf(GREEN_BORDER)
                        chipStrokeWidth = 1f
                    }
                    ChipType.MISSING -> {
                        chipBackgroundColor = ColorStateList.valueOf(RED_50)
                        setTextColor(RED_700)
                        chipStrokeColor = ColorStateList.valueOf(RED_BORDER)
                        chipStrokeWidth = 1f
                    }
                    ChipType.ISSUE -> {
                        chipBackgroundColor = ColorStateList.valueOf(AMBER_50)
                        setTextColor(AMBER_700)
                        chipStrokeColor = ColorStateList.valueOf(AMBER_BORDER)
                        chipStrokeWidth = 1f
                    }
                }
                alpha = 0f
                postDelayed({ animate().alpha(1f).setDuration(200L).start() }, index * 40L)
            }
            flexbox.addView(chip)
        }
    }

    enum class ChipType { MATCHED, MISSING, ISSUE }

    private fun exportToExcel(uri: Uri) {
        val result = currentResult ?: return

        Toast.makeText(this, "Generating Excel...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                // ✅ Convert to Candidate with all fields for consistent Excel export
                val candidateForExport = Candidate(
                    id = System.currentTimeMillis().toString(),
                    fileName = fileName,
                    jobId = PERSONAL_JOB_ID,
                    resumeText = "", // Not needed for export
                    name = result.name.ifBlank { fileName.substringBeforeLast('.') },
                    email = result.email,
                    phone = result.phone,
                    address = result.address,
                    education = result.education,
                    experience = result.experience,
                    score = result.score,
                    summary = result.summary,
                    hardSkillsMatched = result.hardSkillsMatched,
                    hardSkillsMissing = result.hardSkillsMissing,
                    softSkillsMatched = result.softSkillsMatched,
                    softSkillsMissing = result.softSkillsMissing,
                    formattingIssues = result.formattingIssues,
                    status = "analyzed",
                    uploadedAt = Date().toString()
                )

                ExcelExporter.export(this@MainActivity, uri, listOf(candidateForExport), "Quick Scan")

                Toast.makeText(this@MainActivity, "✅ Excel exported successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                showError("Export failed: ${e.message}")
            }
        }
    }

    private fun resetForm() {
        resetFileUploadState()
        currentResult = null
        binding.etJobDescription.text?.clear()
        binding.cardResults.visibility = View.GONE
        deactivateStep(2)
        deactivateStep(3)
        validateForm()
        binding.nestedScrollView.scrollTo(0, 0)
        Toast.makeText(this, "Form reset — ready for next screening", Toast.LENGTH_SHORT).show()
    }

    private fun cleanAndTruncateText(text: String, maxChars: Int = 40000): String {
        var cleaned = text.replace(Regex("\\r\\n|\\r"), "\n").replace(Regex("[ \\t]+"), " ").replace(Regex("\\n{3,}"), "\n\n")
        cleaned = cleaned.replace(Regex("(?m)^\\s*Page\\s+\\d+\\s*$"), "").replace(Regex("\\b\\d+\\s*/\\s*\\d+\\b"), "")
        return (if (cleaned.length > maxChars) cleaned.substring(0, maxChars) + "\n[TRUNCATED]" else cleaned).trim()
    }

    private fun extractJsonObject(text: String): String? {
        val startIndex = text.indexOf('{'); if (startIndex == -1) return null
        var braceCount = 0; var inString = false; var escape = false
        for (i in startIndex until text.length) {
            val char = text[i]
            if (escape) { escape = false; continue }
            if (char == '\\') { escape = true; continue }
            if (char == '"') { inString = !inString; continue }
            if (!inString) { if (char == '{') braceCount++ else if (char == '}') { braceCount--; if (braceCount == 0) return text.substring(startIndex, i + 1) } }
        }
        return null
    }

    private fun extractText(uri: Uri): String {
        val extension = getExtension(uri, fileName)
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                when (extension) {
                    "pdf" -> PDDocument.load(inputStream).use { doc -> PDFTextStripper().getText(doc) }
                    "docx" -> XWPFDocument(inputStream).use { doc -> XWPFWordExtractor(doc).use { it.text } }
                    "doc" -> HWPFDocument(inputStream).use { doc -> WordExtractor(doc).use { it.text } }
                    else -> ""
                }
            } ?: ""
        } catch (e: Exception) { e.printStackTrace(); "" }
    }

    private fun showError(msg: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
            binding.progressBar.visibility = View.GONE
            binding.btnAnalyze.isEnabled = true
            binding.btnAnalyze.alpha = 1f
        }
    }

    private fun getFileName(uri: Uri): String {
        var result = ""
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
        }
        return result.ifBlank { uri.path?.substringAfterLast('/') ?: "Unknown File" }
    }

    private fun getExtension(uri: Uri, name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        if (ext.isNotEmpty()) return ext
        val mimeType = contentResolver.getType(uri) ?: return ""
        return when {
            mimeType.contains("pdf") -> "pdf"
            mimeType.contains("wordprocessingml") || mimeType.contains("docx") -> "docx"
            mimeType.contains("msword") || mimeType.contains("doc") -> "doc"
            else -> ""
        }
    }

    // ✅ NEW: Save personal scan to Firestore (unified with Admin Portal)
    private suspend fun savePersonalScanToFirestore(result: CandidateAnalysisResult) {
        val personalOrgId = getPersonalOrgId()

        try {
            // Ensure authenticated
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }

            val candidateId = "personal_${System.currentTimeMillis()}"
            val candidate = Candidate(
                id = candidateId,
                fileName = fileName,
                orgId = personalOrgId,
                jobId = PERSONAL_JOB_ID,
                resumeText = "",
                name = result.name.ifBlank { fileName.substringBeforeLast('.') },
                email = result.email,
                phone = result.phone,
                address = result.address,
                education = result.education,
                experience = result.experience,
                score = result.score,
                summary = result.summary,
                hardSkillsMatched = result.hardSkillsMatched,
                hardSkillsMissing = result.hardSkillsMissing,
                softSkillsMatched = result.softSkillsMatched,
                softSkillsMissing = result.softSkillsMissing,
                formattingIssues = result.formattingIssues,
                status = "analyzed",
                uploadedAt = Date().toString()
            )

            // Save to Firestore under device-specific org
            db.collection("organizations").document(personalOrgId)
                .collection("job_openings").document(PERSONAL_JOB_ID)
                .collection("candidates").document(candidateId)
                .set(candidate)
                .await()

            // Try to increment cvCount, create job doc if first scan
            try {
                db.collection("organizations").document(personalOrgId)
                    .collection("job_openings").document(PERSONAL_JOB_ID)
                    .update("cvCount", com.google.firebase.firestore.FieldValue.increment(1))
                    .await()
            } catch (_: Exception) {
                // First scan - create the job doc
                db.collection("organizations").document(personalOrgId)
                    .collection("job_openings").document(PERSONAL_JOB_ID)
                    .set(JobOpening(
                        id = PERSONAL_JOB_ID,
                        orgId = personalOrgId,
                        title = "Quick Scan",
                        description = "Personal resume screening",
                        requirements = "",
                        cvCount = 1,
                        status = "open"
                    ))
                    .await()
            }

            // ✅ Show success feedback
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivity,
                    "✅ Saved to cloud history",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } catch (e: Exception) {
            Log.e("MainActivity", "Firestore save failed", e)
            FirebaseCrashlytics.getInstance().recordException(e)

            // ✅ Show failure feedback
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivity,
                    "⚠️ Saved locally only (offline mode)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

// Helper extensions for JSON parsing (matching Admin Portal style)
private fun com.google.gson.JsonObject.optS(key: String, default: String = ""): String {
    return if (has(key) && get(key).isJsonPrimitive) get(key).asString else default
}

private fun com.google.gson.JsonObject.optI(key: String, default: Int = 0): Int {
    return if (has(key) && get(key).isJsonPrimitive) get(key).asInt else default
}

private fun com.google.gson.JsonObject.optL(key: String): List<String> {
    return if (has(key) && get(key).isJsonArray) {
        get(key).asJsonArray.mapNotNull { if (it.isJsonPrimitive) it.asString else null }
    } else emptyList()
}