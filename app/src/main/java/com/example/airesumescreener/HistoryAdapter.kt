package com.example.airesumescreener


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.airesumescreener.databinding.ItemHistoryBinding

class HistoryAdapter(private val items: List<ScanRecord>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            tvScore.text = "${item.score}%"
            tvFileName.text = item.fileName
            tvDate.text = item.date
            tvSummary.text = item.summary

            val matchedText = if (item.hardSkillsMatched.isEmpty()) "None" else item.hardSkillsMatched.take(3).joinToString(", ")
            tvKeywords.text = "Top Matched: $matchedText"

            val scoreColorAttr = when {
                item.score >= 75 -> com.google.android.material.R.attr.colorOnPrimary
                item.score >= 50 -> com.google.android.material.R.attr.colorTertiary
                else -> com.google.android.material.R.attr.colorOnError
            }
            val typedValue = android.util.TypedValue()
            root.context.theme.resolveAttribute(scoreColorAttr, typedValue, true)
            tvScore.setTextColor(typedValue.data)
        }
    }

    override fun getItemCount() = items.size
}