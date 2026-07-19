package com.example.airesumescreener

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.airesumescreener.databinding.ItemJobOpeningBinding
import java.text.SimpleDateFormat
import java.util.Locale

class JobOpeningAdapter(
    private val jobs: List<JobOpening>,
    private val onJobClick: (JobOpening) -> Unit,
    private val onAnalyzeClick: (JobOpening) -> Unit,
    private val onResultsClick: (JobOpening) -> Unit, // NEW
    private val onCloseClick: (JobOpening) -> Unit
) : RecyclerView.Adapter<JobOpeningAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemJobOpeningBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemJobOpeningBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val job = jobs[position]
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        with(holder.binding) {
            tvJobTitle.text = job.title
            tvJobId.text = "ID: ${job.id}"
            tvCvCount.text = "${job.cvCount} CVs"
            tvPostedDate.text = job.postedDate?.let { dateFormat.format(it) } ?: "Just posted"
            tvDescription.text = job.description.ifBlank { "No description" }

            if (job.status == "closed") {
                tvStatusBadge.text = "CLOSED"
                tvStatusBadge.setBackgroundColor(android.graphics.Color.parseColor("#FEE2E2"))
                tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#991B1B"))

                btnAnalyze.isEnabled = false; btnAnalyze.alpha = 0.5f
                btnResults.isEnabled = false; btnResults.alpha = 0.5f
                btnClose.isEnabled = false; btnClose.alpha = 0.5f
            } else {
                tvStatusBadge.text = "OPEN"
                tvStatusBadge.setBackgroundColor(android.graphics.Color.parseColor("#D1FAE5"))
                tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#065F46"))

                btnAnalyze.isEnabled = true; btnAnalyze.alpha = 1f
                btnResults.isEnabled = true; btnResults.alpha = 1f
                btnClose.isEnabled = true; btnClose.alpha = 1f
            }

            root.setOnClickListener { onJobClick(job) }
            btnAnalyze.setOnClickListener { onAnalyzeClick(job) }
            btnResults.setOnClickListener { onResultsClick(job) } // NEW
            btnClose.setOnClickListener { onCloseClick(job) }
        }
    }

    override fun getItemCount() = jobs.size
}