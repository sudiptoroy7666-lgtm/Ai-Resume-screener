package com.example.airesumescreener

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.airesumescreener.databinding.ActivityJobResultsBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JobResultsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityJobResultsBinding
    private lateinit var candidateAdapter: CandidateAdapter
    private val candidates = mutableListOf<Candidate>()

    private var jobTitle: String = ""
    private var orgName: String = ""
    private var orgId: String = ""
    private var jobId: String = ""

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri: Uri? ->
        uri?.let { exportToExcel(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJobResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        jobTitle = intent.getStringExtra("JOB_TITLE") ?: "Results"
        orgName = intent.getStringExtra("ORG_NAME") ?: ""
        orgId = intent.getStringExtra("ORG_ID") ?: ""
        jobId = intent.getStringExtra("JOB_ID") ?: ""

        binding.topAppBar.title = "$orgName — $jobTitle"
        binding.topAppBar.setNavigationOnClickListener { finish() }

        // Check if candidates were passed directly from a fresh analysis
        val receivedCandidates = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("CANDIDATES", Candidate::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Candidate>("CANDIDATES")
        }

        candidateAdapter = CandidateAdapter(candidates) { candidate ->
            val intent = Intent(this, CandidateReportActivity::class.java).apply {
                putExtra("CANDIDATE", candidate)
            }
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = candidateAdapter

        if (receivedCandidates != null && receivedCandidates.isNotEmpty()) {
            // Scenario A: Fresh analysis just finished
            candidates.addAll(receivedCandidates)
            candidates.sortByDescending { it.score }
            candidateAdapter.notifyDataSetChanged()
            updateStats()
        } else {
            // Scenario B: User clicked "View Results" -> Load from Firestore cache
            loadCachedResults()
        }

        binding.btnExport.setOnClickListener {
            if (candidates.isEmpty()) {
                Toast.makeText(this, "No candidates to export", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val date = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val safeOrg = orgName.replace(Regex("[^A-Za-z0-9]"), "_")
            val safeJob = jobTitle.replace(Regex("[^A-Za-z0-9]"), "_")
            exportLauncher.launch("${safeOrg}_${safeJob}_$date.xlsx")
        }
    }

    private fun loadCachedResults() {
        // Optional: Show a loading indicator here if you have one in your XML
        binding.recyclerView.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val fetched = FirebaseService.getCandidates(orgId, jobId)
                candidates.clear()
                candidates.addAll(fetched)
                candidates.sortByDescending { it.score }
                candidateAdapter.notifyDataSetChanged()
                updateStats()
                binding.recyclerView.visibility = View.VISIBLE

                if (candidates.isEmpty()) {
                    Toast.makeText(this@JobResultsActivity, "No analyzed candidates found yet.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@JobResultsActivity, "Failed to load results: ${e.message}", Toast.LENGTH_LONG).show()
                binding.recyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun updateStats() {
        val total = candidates.size
        val analyzed = candidates.count { it.status == "analyzed" }
        val avgScore = if (analyzed > 0) candidates.filter { it.status == "analyzed" }.sumOf { it.score } / analyzed else 0
        val strongCount = candidates.count { it.score >= 75 }
        val moderateCount = candidates.count { it.score in 50..74 }
        val weakCount = candidates.count { it.score > 0 && it.score < 50 }

        binding.tvTotal.text = total.toString()
        binding.tvAvgScore.text = if (analyzed > 0) "$avgScore%" else "—"
        binding.tvStrong.text = strongCount.toString()
        binding.tvModerate.text = moderateCount.toString()
        binding.tvWeak.text = weakCount.toString()
    }

    private fun exportToExcel(uri: Uri) {
        Toast.makeText(this, "Generating Excel...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            try {
                ExcelExporter.export(this@JobResultsActivity, uri, candidates, jobTitle)
                Toast.makeText(this@JobResultsActivity, "✅ Excel exported successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@JobResultsActivity, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}