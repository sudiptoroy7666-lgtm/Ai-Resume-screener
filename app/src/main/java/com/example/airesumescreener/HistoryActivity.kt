package com.example.airesumescreener

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.airesumescreener.databinding.ActivityHistoryBinding
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var historyManager: HistoryManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        historyManager = HistoryManager(this)

        binding.topAppBar.setNavigationOnClickListener { finish() }

        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.action_clear) {
                val current = historyManager.getHistory()
                if (current.isEmpty()) {
                    Toast.makeText(this, "History is already empty", Toast.LENGTH_SHORT).show()
                    return@setOnMenuItemClickListener true
                }
                historyManager.clearHistory()
                loadData()
                Toast.makeText(this, "Personal scan history cleared", Toast.LENGTH_SHORT).show()
                true
            } else false
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        loadData()
    }

    private fun loadData() {
        binding.recyclerView.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE

        // Show a simple progress indicator while loading
        binding.recyclerView.alpha = 0.5f

        lifecycleScope.launch {
            val allItems = mutableListOf<HistoryItem>()

            // 1. Load local personal scans
            val localScans = historyManager.getHistory()
            localScans.forEach { scan ->
                allItems.add(HistoryItem(
                    id = "local_${scan.id}",
                    source = "personal",
                    fileName = scan.fileName,
                    date = scan.date,
                    score = scan.score,
                    summary = scan.summary,
                    hardSkillsMatched = scan.hardSkillsMatched,
                    hardSkillsMissing = scan.hardSkillsMissing
                ))
            }

            // 2. Load batch-analyzed candidates from Firestore
            try {
                val allCandidates = FirebaseService.getAllAnalyzedCandidates()
                val personalOrgId = DeviceIdHelper.getDeviceId(this@HistoryActivity)

                // Only show: HR batch scans + this device's personal scans
                val filteredCandidates = allCandidates.filter { candidate ->
                    val isHrScan = !candidate.orgId.startsWith("PERSONAL_")
                    val isMyPersonalScan = candidate.orgId == personalOrgId
                    isHrScan || isMyPersonalScan
                }

                // Cache org and job names
                val orgNameCache = mutableMapOf<String, String>()
                val jobTitleCache = mutableMapOf<String, String>()

                filteredCandidates.forEach { candidate ->
                    val orgName = orgNameCache.getOrPut(candidate.orgId) {
                        if (candidate.orgId == personalOrgId) "My Personal Scans"
                        else FirebaseService.getOrganizationName(candidate.orgId)
                    }
                    val jobKey = "${candidate.orgId}_${candidate.jobId}"
                    val jobTitle = jobTitleCache.getOrPut(jobKey) {
                        FirebaseService.getJobTitle(candidate.orgId, candidate.jobId)
                    }

                    val dateStr = try {
                        if (candidate.uploadedAt.isNotBlank()) {
                            val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                .parse(candidate.uploadedAt)
                            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(parsed ?: Date())
                        } else "Unknown date"
                    } catch (e: Exception) { "Unknown date" }

                    allItems.add(HistoryItem(
                        id = "batch_${candidate.id}",
                        source = if (candidate.orgId.startsWith("PERSONAL_")) "personal" else "batch",
                        fileName = candidate.fileName.ifBlank { candidate.name },
                        date = dateStr,
                        score = candidate.score,
                        summary = candidate.summary,
                        hardSkillsMatched = candidate.hardSkillsMatched,
                        hardSkillsMissing = candidate.hardSkillsMissing,
                        orgName = orgName,
                        jobTitle = jobTitle,
                        candidate = candidate
                    ))
                }
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                Toast.makeText(this@HistoryActivity, "Could not load batch history: ${e.message}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@HistoryActivity, "Could not load batch history: ${e.message}", Toast.LENGTH_LONG).show()
            }

            // 3. Sort by score (highest first), then by date
            allItems.sortWith(compareByDescending<HistoryItem> { it.score }.thenByDescending { it.date })

            binding.recyclerView.alpha = 1f

            if (allItems.isEmpty()) {
                binding.recyclerView.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
            } else {
                binding.recyclerView.visibility = View.VISIBLE
                binding.tvEmpty.visibility = View.GONE
                binding.recyclerView.adapter = HistoryAdapter(allItems) { item ->
                    // If it's a batch result with full candidate data, open CandidateReportActivity
                    if (item.source == "batch" && item.candidate != null) {
                        val intent = Intent(this@HistoryActivity, CandidateReportActivity::class.java).apply {
                            putExtra("CANDIDATE", item.candidate)
                        }
                        startActivity(intent)
                    } else {
                        // For personal scans, just show a toast with the summary
                        Toast.makeText(this@HistoryActivity, item.summary, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }
}