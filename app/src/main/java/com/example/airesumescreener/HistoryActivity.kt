package com.example.airesumescreener

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.airesumescreener.databinding.ActivityHistoryBinding

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
                Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show()
                true
            } else false
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        loadData()
    }

    private fun loadData() {
        val history = historyManager.getHistory()
        if (history.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.tvEmpty.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.tvEmpty.visibility = View.GONE
            binding.recyclerView.adapter = HistoryAdapter(history)
        }
    }
}