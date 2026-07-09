package com.example.airesumescreener


import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HistoryManager(private val context: Context) {
    private val fileName = "scan_history.json"
    private val gson = Gson()

    fun saveScan(record: ScanRecord) {
        val history = getHistory().toMutableList()
        history.add(0, record)
        if (history.size > 30) history.removeAt(history.size - 1)

        val json = gson.toJson(history)
        context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(json.toByteArray())
        }
    }

    fun getHistory(): List<ScanRecord> {
        return try {
            context.openFileInput(fileName).use { inputStream ->
                val json = inputStream.bufferedReader().use { it.readText() }
                val type = object : TypeToken<List<ScanRecord>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearHistory() {
        context.deleteFile(fileName)
    }
}