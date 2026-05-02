package com.example.ruhi1

import android.content.Context
import android.content.SharedPreferences

class MemoryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("RuhiMemory", Context.MODE_PRIVATE)

    fun saveFact(fact: String) {
        val currentFacts = getFacts().toMutableSet()
        currentFacts.add(fact)
        prefs.edit().putStringSet("facts", currentFacts).apply()
    }

    fun getFacts(): Set<String> {
        return prefs.getStringSet("facts", emptySet()) ?: emptySet()
    }
    
    fun saveContact(name: String, number: String) {
        prefs.edit().putString("contact_${name.lowercase()}", number).apply()
    }
    
    fun getContactNumber(name: String): String? {
        return prefs.getString("contact_${name.lowercase()}", null)
    }
}
