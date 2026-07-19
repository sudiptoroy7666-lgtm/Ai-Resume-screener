package com.example.airesumescreener

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.airesumescreener.databinding.ItemCandidateResultBinding

class CandidateAdapter(
    private val candidates: List<Candidate>,
    private val onClick: (Candidate) -> Unit
) : RecyclerView.Adapter<CandidateAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemCandidateResultBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCandidateResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val c = candidates[position]
        with(holder.binding) {
            tvName.text = c.name
            tvFileName.text = c.fileName
            tvScore.text = "${c.score}%"
            tvSummary.text = c.summary.ifBlank { "No summary" }
            tvEmail.text = c.email.ifBlank { "N/A" }

            progressScore.setProgress(c.score, true)

            val scoreColor = when {
                c.score >= 75 -> Color.parseColor("#0F766E")
                c.score >= 50 -> Color.parseColor("#D97706")
                else -> Color.parseColor("#DC2626")
            }
            tvScore.setTextColor(scoreColor)

            val badgeText = when {
                c.score >= 75 -> "Strong"
                c.score >= 50 -> "Moderate"
                else -> "Weak"
            }
            tvBadge.text = badgeText

            val badgeBg = when {
                c.score >= 75 -> Color.parseColor("#D1FAE5")
                c.score >= 50 -> Color.parseColor("#FEF3C7")
                else -> Color.parseColor("#FEE2E2")
            }
            tvBadge.setBackgroundColor(badgeBg)

            val badgeTextC = when {
                c.score >= 75 -> Color.parseColor("#065F46")
                c.score >= 50 -> Color.parseColor("#92400E")
                else -> Color.parseColor("#991B1B")
            }
            tvBadge.setTextColor(badgeTextC)

            root.setOnClickListener { onClick(c) }
        }
    }

    override fun getItemCount() = candidates.size
}