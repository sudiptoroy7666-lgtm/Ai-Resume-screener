package com.example.airesumescreener

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.airesumescreener.databinding.ItemHistoryBinding
import com.google.android.material.progressindicator.CircularProgressIndicator

class HistoryAdapter(private val items: List<ScanRecord>) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private val TEAL_700 = Color.parseColor("#0F766E")
    private val GREEN_700 = Color.parseColor("#065F46")
    private val GREEN_BG = Color.parseColor("#D1FAE5")
    private val AMBER_700 = Color.parseColor("#92400E")
    private val AMBER_BG = Color.parseColor("#FEF3C7")
    private val RED_700 = Color.parseColor("#991B1B")
    private val RED_BG = Color.parseColor("#FEE2E2")
    private val AMBER_500 = Color.parseColor("#D97706")
    private val RED_500 = Color.parseColor("#DC2626")

    inner class ViewHolder(val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {

            miniScoreRing.setProgress(item.score, true)
            tvScore.text = "${item.score}%"

            val ringColor = when {
                item.score >= 75 -> {
                    tvScore.setTextColor(TEAL_700)
                    tvScoreBadge.text = "Strong"
                    tvScoreBadge.setBackgroundColor(GREEN_BG)
                    tvScoreBadge.setTextColor(GREEN_700)
                    TEAL_700
                }
                item.score >= 50 -> {
                    tvScore.setTextColor(AMBER_700)
                    tvScoreBadge.text = "Moderate"
                    tvScoreBadge.setBackgroundColor(AMBER_BG)
                    tvScoreBadge.setTextColor(AMBER_700)
                    AMBER_500
                }
                else -> {
                    tvScore.setTextColor(RED_700)
                    tvScoreBadge.text = "Weak"
                    tvScoreBadge.setBackgroundColor(RED_BG)
                    tvScoreBadge.setTextColor(RED_700)
                    RED_500
                }
            }

            setIndicatorColorCompat(miniScoreRing, ringColor)

            tvFileName.text = item.fileName
            tvDate.text = item.date
            tvSummary.text = item.summary.ifBlank { "No summary available." }

            val matchedText = if (item.hardSkillsMatched.isEmpty()) "None"
            else item.hardSkillsMatched.take(3).joinToString(", ")
            tvKeywords.text = matchedText
        }
    }

    private fun setIndicatorColorCompat(indicator: CircularProgressIndicator, color: Int) {
        try {
            val drawableField = indicator.javaClass.getDeclaredField("indeterminateDrawable")
            drawableField.isAccessible = true
            val drawable = drawableField.get(indicator) as? Drawable
            drawable?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        } catch (_: Exception) {}
        try {
            val drawableField = indicator.javaClass.getDeclaredField("determinateDrawable")
            drawableField.isAccessible = true
            val drawable = drawableField.get(indicator) as? Drawable
            drawable?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        } catch (_: Exception) {}
    }

    override fun getItemCount() = items.size
}