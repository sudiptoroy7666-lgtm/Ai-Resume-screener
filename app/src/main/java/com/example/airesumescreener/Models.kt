package com.example.airesumescreener

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Parcelize
data class Candidate(
    val id: String = "",  // ← Remove @DocumentId annotation
    val fileName: String = "",
    val orgId: String = "",
    val jobId: String = "",
    val resumeText: String = "",
    var status: String = "pending",
    var score: Int = 0,
    var name: String = "Unknown",
    var email: String = "",
    var phone: String = "",
    var address: String = "",
    var education: String = "",
    var experience: String = "",
    var summary: String = "",
    var hardSkillsMatched: List<String> = emptyList(),
    var hardSkillsMissing: List<String> = emptyList(),
    var softSkillsMatched: List<String> = emptyList(),
    var softSkillsMissing: List<String> = emptyList(),
    var formattingIssues: List<String> = emptyList(),
    val uploadedAt: String = ""
) : Parcelable

data class Organization(
    @DocumentId val id: String = "",
    val name: String = "",
    @ServerTimestamp val createdAt: Date? = null
)

data class JobOpening(
    @DocumentId val id: String = "",
    val orgId: String = "",
    val title: String = "",
    val description: String = "",
    val requirements: String = "",       // Full JD (for AI matching) ← NEW
    @ServerTimestamp val postedDate: Date? = null,
    var cvCount: Int = 0,
    var status: String = "open"
)

data class ScanRecord(
    val id: Long = System.currentTimeMillis(),
    val date: String = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date()),
    val fileName: String,
    val score: Int,
    val summary: String,
    val hardSkillsMatched: List<String>,
    val hardSkillsMissing: List<String>
)

// Shared AI API data classes
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val response_format: ResponseFormat = ResponseFormat("json_object"),
    val temperature: Double = 0.1,
    val max_tokens: Int = 2048
)

// Add this at the bottom of Models.kt
data class HistoryItem(
    val id: String,
    val source: String,           // "personal" or "batch"
    val fileName: String,
    val date: String,
    val score: Int,
    val summary: String,
    val hardSkillsMatched: List<String>,
    val hardSkillsMissing: List<String>,
    val orgName: String? = null,   // Only for batch results
    val jobTitle: String? = null,  // Only for batch results
    val candidate: Candidate? = null // Full candidate data for batch results
)
data class Message(val role: String, val content: String)
data class ResponseFormat(val type: String)
data class ChatResponse(val choices: List<Choice>)
data class Choice(val message: Message)