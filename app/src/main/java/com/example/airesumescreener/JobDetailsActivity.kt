package com.example.airesumescreener

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.airesumescreener.databinding.ActivityJobDetailsBinding
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class JobDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityJobDetailsBinding
    private lateinit var jobAdapter: JobOpeningAdapter
    private val jobs = mutableListOf<JobOpening>()
    private var orgId = ""
    private var orgName = ""
    private lateinit var wakeLockManager: WakeLockManager
    private var progressDialog: AlertDialog? = null

    private val cerebrasApi: CerebrasApi by lazy {
        Retrofit.Builder().baseUrl(BASE_URL)
            .client(OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(CerebrasApi::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJobDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        wakeLockManager = WakeLockManager(this)
        lifecycle.addObserver(wakeLockManager)

        orgId = intent.getStringExtra("ORG_ID") ?: run { finish(); return }
        orgName = intent.getStringExtra("ORG_NAME") ?: ""
        binding.topAppBar.title = orgName
        binding.topAppBar.setNavigationOnClickListener { finish() }



        // ... inside onCreate() ...

        jobAdapter = JobOpeningAdapter(jobs,
            onJobClick = { },
            onAnalyzeClick = { startBatchAnalysis(it) },
            onResultsClick = { job ->
                // Launch Results activity WITHOUT passing the CANDIDATES extra.
                // This tells JobResultsActivity to load from Firestore cache.
                val intent = Intent(this, JobResultsActivity::class.java).apply {
                    putExtra("JOB_TITLE", job.title)
                    putExtra("ORG_NAME", orgName)
                    putExtra("ORG_ID", orgId)
                    putExtra("JOB_ID", job.id)
                }
                startActivity(intent)
            },
            onCloseClick = { closeJob(it) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = jobAdapter

        binding.btnAddJob.setOnClickListener { showCreateJobDialog() }
        loadJobs()
    }

    private fun loadJobs() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val list = FirebaseService.getJobOpenings(orgId)
            jobs.clear(); jobs.addAll(list); jobAdapter.notifyDataSetChanged()
            binding.progressBar.visibility = View.GONE
            binding.tvEmpty.visibility = if (jobs.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (jobs.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun showCreateJobDialog() {
        val title = EditText(this).apply {
            hint = "Job Title / ID"
            setPadding(40, 20, 40, 20)
        }
        val desc = EditText(this).apply {
            hint = "Brief description (for display)"
            setPadding(40, 20, 40, 20)
            minLines = 2
        }
        val requirements = EditText(this).apply {  // ← NEW FIELD
            hint = "Full job requirements (for AI matching)\nPaste skills, qualifications, responsibilities..."
            setPadding(40, 20, 40, 20)
            minLines = 6
            maxLines = 10
        }

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
            addView(title)
            addView(desc)
            addView(requirements)
        }

        AlertDialog.Builder(this)
            .setTitle("Create Job Opening")
            .setView(container)
            .setPositiveButton("Create") { _, _ ->
                val t = title.text.toString().trim()
                val d = desc.text.toString().trim()
                val r = requirements.text.toString().trim()  // ← NEW

                if (t.length < 3) {
                    Toast.makeText(this, "Title: min 3 chars", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (r.length < 50) {  // ← NEW VALIDATION
                    Toast.makeText(this, "Requirements: min 50 chars for accurate AI matching", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    FirebaseService.createJobOpening(orgId, t, d, r).fold(  // ← PASS REQUIREMENTS
                        onSuccess = {
                            Toast.makeText(this@JobDetailsActivity, "Created: $it", Toast.LENGTH_SHORT).show()
                            loadJobs()
                        },
                        onFailure = {
                            Toast.makeText(this@JobDetailsActivity, it.message, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startBatchAnalysis(job: JobOpening) {
        // Validate job has requirements
        if (job.requirements.isBlank()) {
            Toast.makeText(this, "Job has no requirements. Edit job to add requirements first.", Toast.LENGTH_LONG).show()
            return
        }

        showProgressDialog()
        wakeLockManager.acquire()

        lifecycleScope.launch {
            try {
                updateProgress("Loading candidates…", 0, 0, true)
                val candidates = FirebaseService.getCandidates(orgId, job.id)

                if (candidates.isEmpty()) {
                    dismissProgress()
                    Toast.makeText(this@JobDetailsActivity, "No CVs found.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val total = candidates.size
                val done = mutableListOf<Candidate>()
                var lastUiUpdate = 0L

                // Truncate job requirements to prevent token overflow (15K chars ≈ 4K tokens)
                val jobRequirements = job.requirements.take(15000)

                for (i in candidates.indices) {
                    yield()
                    val c = candidates[i]
                    val now = System.currentTimeMillis()

                    if (now - lastUiUpdate > 150L || i == 0 || i == total - 1) {
                        updateProgress("Analyzing: ${c.name}", i, total, false)
                        lastUiUpdate = now
                    }

                    try {
                        if (c.resumeText.isBlank()) {
                            c.status = "Error"
                            done += c
                            continue
                        }

                        // ✅ NEW PROMPT WITH JOB REQUIREMENTS
                        val prompt = """
                        You are an expert ATS (Applicant Tracking System) analyzer.
                        
                        JOB REQUIREMENTS:
                        ${jobRequirements}
                        
                        CANDIDATE RESUME:
                        ${c.resumeText.take(20000)}
                        
                        Compare the resume against the job requirements and output ONLY JSON:
                        {
                          "score": <int 0-100 based on how well resume matches job requirements>,
                          "name": "<extracted from resume>",
                          "email": "<extracted>",
                          "phone": "<extracted>",
                          "address": "<extracted>",
                          "education": "<brief summary>",
                          "experience": "<brief summary>",
                          "hard_skills_matched": [<skills in resume that match job requirements>],
                          "hard_skills_missing": [<skills in job requirements not found in resume>],
                          "soft_skills_matched": [<soft skills in resume that match>],
                          "soft_skills_missing": [<soft skills in requirements not found>],
                          "formatting_issues": [<ATS formatting problems>],
                          "summary": "<1-2 sentence assessment of fit>"
                        }
                    """.trimIndent()

                        val req = ChatRequest(MODEL_ID, listOf(
                            Message("system", "You are an expert ATS analyzer. Output only valid JSON."),
                            Message("user", prompt)
                        ))

                        var res: retrofit2.Response<ChatResponse>? = null
                        var retries = 0

                        while (retries < 3) {
                            res = withContext(Dispatchers.IO) {
                                cerebrasApi.analyze("Bearer ${BuildConfig.CEREBRAS_API_KEY}", req)
                            }
                            if (res.isSuccessful) break
                            if (res.code() == 429) {
                                delay(10000)
                                retries++
                            } else break
                        }

                        if (res?.isSuccessful == true) {
                            val raw = res.body()?.choices?.firstOrNull()?.message?.content.orEmpty()
                            val json = extractJson(raw)?.let { JsonParser.parseString(it).asJsonObject }

                            if (json != null) {
                                c.score = json.optI("score")
                                c.name = json.optS("name", c.name)
                                c.email = json.optS("email", c.email)
                                c.phone = json.optS("phone")
                                c.address = json.optS("address")
                                c.education = json.optS("education")
                                c.experience = json.optS("experience")
                                c.summary = json.optS("summary")
                                c.hardSkillsMatched = json.optL("hard_skills_matched")
                                c.hardSkillsMissing = json.optL("hard_skills_missing")
                                c.softSkillsMatched = json.optL("soft_skills_matched")
                                c.softSkillsMissing = json.optL("soft_skills_missing")
                                c.formattingIssues = json.optL("formatting_issues")
                                c.status = "analyzed"

                                withContext(Dispatchers.IO) {
                                    FirebaseService.updateCandidate(c)
                                }
                            } else {
                                c.status = "Error"
                            }
                        } else {
                            c.status = "Error"
                        }
                    } catch (e: Exception) {
                        c.status = "Error"
                        e.printStackTrace()
                    }

                    done += c
                    delay(3000)
                }

                withContext(Dispatchers.Main) {
                    dismissProgress()
                    startActivity(Intent(this@JobDetailsActivity, JobResultsActivity::class.java).apply {
                        putParcelableArrayListExtra("CANDIDATES", ArrayList(done))
                        putExtra("JOB_TITLE", job.title)
                        putExtra("ORG_NAME", orgName)
                        putExtra("ORG_ID", orgId)
                        putExtra("JOB_ID", job.id)
                    })
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    dismissProgress()
                    Toast.makeText(this@JobDetailsActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun closeJob(job: JobOpening) {
        AlertDialog.Builder(this).setTitle("Close & Delete Job")
            .setMessage("This will permanently delete '${job.title}' and ALL candidate data. This cannot be undone.")
            .setPositiveButton("Delete Everything") { _, _ ->
                lifecycleScope.launch {
                    val result = FirebaseService.deleteJobOpening(orgId, job.id)
                    result.fold(
                        onSuccess = {
                            Toast.makeText(this@JobDetailsActivity, "Job and all data deleted.", Toast.LENGTH_SHORT).show()
                            loadJobs()
                        },
                        onFailure = {
                            Toast.makeText(this@JobDetailsActivity, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun showProgressDialog() {
        val v = layoutInflater.inflate(R.layout.dialog_batch_progress, null)
        progressDialog = AlertDialog.Builder(this).setView(v).setCancelable(false).create()
        progressDialog?.show()
    }

    private fun dismissProgress() {
        progressDialog?.dismiss(); progressDialog = null
        wakeLockManager.release()
    }

    private fun updateProgress(msg: String, cur: Int, total: Int, indeterminate: Boolean) {
        lifecycleScope.launch(Dispatchers.Main) {
            progressDialog?.window?.decorView?.let { dv ->
                dv.findViewById<TextView>(R.id.tvProgressMessage)?.text = msg
                dv.findViewById<ProgressBar>(R.id.progressBarDialog)?.apply {
                    isIndeterminate = indeterminate
                    if (!indeterminate && total > 0) { max = total; progress = cur + 1 }
                }
                dv.findViewById<TextView>(R.id.tvProgressCount)?.text =
                    if (total > 0 && !indeterminate) "${cur + 1} / $total" else ""
            }
        }
    }

    private fun extractJson(s: String): String? {
        val start = s.indexOf('{'); if (start < 0) return null
        var depth = 0; var inStr = false; var esc = false
        for (i in start until s.length) {
            val c = s[i]
            if (esc) { esc = false; continue }
            if (c == '\\') { esc = true; continue }
            if (c == '"') { inStr = !inStr; continue }
            if (!inStr) { if (c == '{') depth++ else if (c == '}') { depth--; if (depth == 0) return s.substring(start, i + 1) } }
        }
        return null
    }

    private fun com.google.gson.JsonObject.optS(k: String, d: String = "") =
        if (has(k) && get(k).isJsonPrimitive) get(k).asString else d
    private fun com.google.gson.JsonObject.optI(k: String, d: Int = 0) =
        if (has(k) && get(k).isJsonPrimitive) get(k).asInt else d
    private fun com.google.gson.JsonObject.optL(k: String): List<String> =
        if (has(k) && get(k).isJsonArray)
            get(k).asJsonArray.mapNotNull { if (it.isJsonPrimitive) it.asString else null }
        else emptyList()

    override fun onDestroy() { super.onDestroy(); dismissProgress() }
    override fun onResume() { super.onResume(); loadJobs() }
}