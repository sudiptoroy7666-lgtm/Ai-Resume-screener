package com.example.airesumescreener

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ScanRecord(
    val id: Long = System.currentTimeMillis(),
    val date: String = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date()),
    val fileName: String,
    val score: Int,
    val summary: String,
    val hardSkillsMatched: List<String>,
    val hardSkillsMissing: List<String>
)