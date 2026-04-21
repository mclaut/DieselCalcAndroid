package com.mclaut.dieselcalc

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LogStore(context: Context) {
    private val prefs = context.getSharedPreferences("calc_log", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getEntries(): List<LogEntry> {
        val json = prefs.getString("entries", "[]") ?: "[]"
        return try {
            val type = object : TypeToken<List<LogEntry>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addEntry(entry: LogEntry): List<LogEntry> {
        val entries = mutableListOf(entry) + getEntries()
        saveEntries(entries)
        return entries
    }

    fun deleteEntry(id: String): List<LogEntry> {
        val entries = getEntries().filter { it.id != id }
        saveEntries(entries)
        return entries
    }

    fun clearAll(): List<LogEntry> {
        saveEntries(emptyList())
        return emptyList()
    }

    private fun saveEntries(entries: List<LogEntry>) {
        prefs.edit().putString("entries", gson.toJson(entries)).apply()
    }
}
