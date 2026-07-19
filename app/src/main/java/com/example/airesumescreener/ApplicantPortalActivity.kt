package com.example.airesumescreener

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.airesumescreener.databinding.ActivityApplicantPortalBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.util.Date

class ApplicantPortalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityApplicantPortalBinding

    private var resumeUri: Uri? = null
    private var fileName: String = ""
    private var fileSize: Long = 0
    private var extractedText: String? = null

    private val GREEN_700 = Color.parseColor("#065F46")
    private val GREEN_50 = Color.parseColor("#D1FAE5")
    private val GREEN_BORDER = Color.parseColor("#A7F3D0")
    private val TEAL_700 = Color.parseColor("#0F766E")
    private val TEAL_500 = Color.parseColor("#0D9488")
    private val RED_50 = Color.parseColor("#FEE2E2")
    private val RED_BORDER = Color.parseColor("#FECACA")

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val MAX_SIZE_BYTES = 10 * 1024 * 1024L // 10MB
    private val MAX_TEXT_CHARS = 15000

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { handleFilePicked(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApplicantPortalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        PDFBoxResourceLoader.init(applicationContext)

        binding.topAppBar.setNavigationOnClickListener { finish() }

        binding.btnPickResume.setOnClickListener {
            filePickerLauncher.launch(arrayOf(
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            ))
        }

        binding.btnRemoveFile.setOnClickListener {
            resetFileState()
        }

        binding.btnSubmit.setOnClickListener {
            submitApplication()
        }
// ===== QUICK ACTION NAVIGATION =====
// In onCreate(), after binding.topAppBar.setNavigationOnClickListener:
        binding.topAppBar.title = "Applicant Portal"
        binding.topAppBar.subtitle = "Check, submit & track"
        binding.cardCheckResume.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_APPLICANT_MODE, true)
            })
        }
        binding.cardCheckResume.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        binding.cardViewHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        validateForm()
    }

    private fun handleFilePicked(uri: Uri) {
        val pickedFileName = getFileName(uri)
        val extension = getExtension(uri, pickedFileName).lowercase()

        // Reject .doc (legacy Word)
        if (extension == "doc") {
            showError("We can't read old .doc files. Please re-save as PDF or .docx and upload again.")
            return
        }

        // Validate extension
        if (extension != "pdf" && extension != "docx") {
            showError("Unsupported file type. Please upload a PDF or .docx file.")
            return
        }

        // Validate size
        val size = getFileSize(uri)
        if (size > MAX_SIZE_BYTES) {
            showError("File is too large. Please keep your resume under 10MB.")
            return
        }

        resumeUri = uri
        fileName = pickedFileName
        fileSize = size

        showFileUploadedState()
        extractTextAsync(uri, extension)
    }

    private fun showFileUploadedState() {
        binding.btnPickResume.apply {
            text = "✓ File selected successfully"
            setTextColor(GREEN_700)
            backgroundTintList = ColorStateList.valueOf(GREEN_50)
            strokeColor = ColorStateList.valueOf(GREEN_BORDER)
            icon = resources.getDrawable(android.R.drawable.checkbox_on_background, theme)
            iconTint = ColorStateList.valueOf(GREEN_700)
            alpha = 0.95f
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

    private fun resetFileState() {
        resumeUri = null
        fileName = ""
        fileSize = 0
        extractedText = null

        binding.btnPickResume.apply {
            text = "Tap to select your resume"
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
        binding.tvExtractionStatus.text = "PDF or .docx only, max 10MB"
        binding.tvExtractionStatus.setTextColor(Color.parseColor("#94A3B8"))
        binding.extractionProgress.visibility = View.GONE

        validateForm()
    }

    private fun extractTextAsync(uri: Uri, extension: String) {
        binding.extractionProgress.visibility = View.VISIBLE
        binding.tvExtractionStatus.text = "Extracting text from file..."
        binding.tvExtractionStatus.setTextColor(Color.parseColor("#0F766E"))

        lifecycleScope.launch {
            try {
                val text = withContext(Dispatchers.IO) { extractText(uri, extension) }

                val cleaned = cleanTextForAts(text)

                if (cleaned.length < 40) {
                    binding.extractionProgress.visibility = View.GONE
                    binding.tvExtractionStatus.text = "⚠ Could not find readable text. Try a text-based PDF or Word file."
                    binding.tvExtractionStatus.setTextColor(Color.parseColor("#DC2626"))
                    extractedText = null
                    validateForm()
                    return@launch
                }

                // Truncate to match website's 15K limit
                extractedText = if (cleaned.length > MAX_TEXT_CHARS) {
                    cleaned.substring(0, MAX_TEXT_CHARS)
                } else {
                    cleaned
                }

                binding.extractionProgress.visibility = View.GONE
                val charCount = extractedText?.length ?: 0
                binding.tvExtractionStatus.text = "✅ Text extracted: $charCount characters ready to submit"
                binding.tvExtractionStatus.setTextColor(Color.parseColor("#059669"))

                validateForm()

            } catch (e: Exception) {
                Log.e("ApplicantPortal", "Extraction error", e)
                binding.extractionProgress.visibility = View.GONE
                binding.tvExtractionStatus.text = "❌ Error reading file. Please try another file."
                binding.tvExtractionStatus.setTextColor(Color.parseColor("#DC2626"))
                extractedText = null
                validateForm()
            }
        }
    }

    private fun extractText(uri: Uri, extension: String): String {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                when (extension) {
                    "pdf" -> PDDocument.load(inputStream).use { doc -> PDFTextStripper().getText(doc) }
                    "docx" -> XWPFDocument(inputStream).use { doc -> XWPFWordExtractor(doc).use { it.text } }
                    else -> ""
                }
            } ?: ""
        } catch (e: Exception) {
            Log.e("ApplicantPortal", "extractText error", e)
            ""
        }
    }

    private fun cleanTextForAts(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace('\u00A0', ' ')
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\n{3,}"), "\n\n")
            .split('\n')
            .joinToString("\n") { it.trim() }
            .trim()
    }

    private fun validateForm() {
        val orgCode = binding.etOrgCode.text?.toString()?.trim().orEmpty()
        val jobId = binding.etJobId.text?.toString()?.trim().orEmpty()
        val fullName = binding.etFullName.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()

        val isOrgValid = orgCode.length >= 3
        val isJobValid = jobId.length >= 2
        val isNameValid = fullName.isNotEmpty()
        val isEmailValid = isValidEmail(email)
        val hasExtractedText = !extractedText.isNullOrBlank()

        val allValid = isOrgValid && isJobValid && isNameValid && isEmailValid && hasExtractedText

        binding.btnSubmit.isEnabled = allValid
        binding.btnSubmit.alpha = if (allValid) 1f else 0.5f
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun submitApplication() {
        val orgCode = binding.etOrgCode.text.toString().trim()
            .uppercase()
            .replace(Regex("[^A-Z0-9]"), "_")
        val jobId = binding.etJobId.text.toString().trim()
            .uppercase()
            .replace(Regex("[^A-Z0-9]"), "_")
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val text = extractedText ?: return

        // Lock UI
        binding.btnSubmit.isEnabled = false
        binding.btnSubmit.alpha = 0.5f
        binding.btnSubmit.text = "Submitting..."

        lifecycleScope.launch {
            try {
                // 1. Authenticate
                withContext(Dispatchers.IO) {
                    if (auth.currentUser == null) {
                        auth.signInAnonymously().await()
                    }
                }

                // 2. Validate org/job exists
                val jobRef = db.collection("organizations")
                    .document(orgCode)
                    .collection("job_openings")
                    .document(jobId)

                val jobSnap = withContext(Dispatchers.IO) {
                    jobRef.get().await()
                }

                if (!jobSnap.exists()) {
                    showValidationError(
                        "Job \"$jobId\" not found in organization \"$orgCode\". Please verify with HR."
                    )
                    return@launch
                }

                // 3. Save candidate - use Candidate object directly for type safety
                val candidateId = "portal_${System.currentTimeMillis()}"
                val candidate = Candidate(
                    id = candidateId,
                    fileName = fileName,
                    orgId = orgCode,
                    jobId = jobId,
                    resumeText = text,
                    name = fullName,
                    email = email,
                    status = "pending",
                    score = 0,
                    uploadedAt = Date().toString()
                )

                withContext(Dispatchers.IO) {
                    db.collection("organizations").document(orgCode)
                        .collection("job_openings").document(jobId)
                        .collection("candidates").document(candidateId)
                        .set(candidate).await()

                    // Increment cvCount
                    jobRef.update("cvCount", FieldValue.increment(1)).await()
                }


                // 4. Success!
                Toast.makeText(
                    this@ApplicantPortalActivity,
                    "✅ Application submitted successfully! Good luck!",
                    Toast.LENGTH_LONG
                ).show()

                binding.btnSubmit.text = "✓ Submitted"
                binding.btnSubmit.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#059669"))

                // Reset form after short delay
                binding.btnSubmit.postDelayed({
                    resetForm()
                }, 2500)

            } catch (e: Exception) {
                Log.e("ApplicantPortal", "Submit error", e)
                Toast.makeText(
                    this@ApplicantPortalActivity,
                    "❌ ${e.message ?: "Submission failed. Please try again."}",
                    Toast.LENGTH_LONG
                ).show()
                binding.btnSubmit.isEnabled = true
                binding.btnSubmit.alpha = 1f
                binding.btnSubmit.text = "Submit Application"
            }
        }
    }

    private fun showValidationError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        binding.btnSubmit.isEnabled = true
        binding.btnSubmit.alpha = 1f
        binding.btnSubmit.text = "Submit Application"
    }

    private fun resetForm() {
        binding.etOrgCode.text?.clear()
        binding.etJobId.text?.clear()
        binding.etFullName.text?.clear()
        binding.etEmail.text?.clear()
        resetFileState()

        binding.btnSubmit.text = "Submit Application"
        binding.btnSubmit.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#0F766E"))

        Toast.makeText(this, "Form reset — ready for another application", Toast.LENGTH_SHORT).show()
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun getFileName(uri: Uri): String {
        var result = ""
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        return result.ifBlank { uri.path?.substringAfterLast('/') ?: "Unknown File" }
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
}