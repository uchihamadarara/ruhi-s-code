package com.example.ruhiassistant

import android.content.Context
import java.io.File
import java.io.IOException

class FileManager(private val context: Context) {

    fun saveFile(filename: String, content: String): Boolean {
        return try {
            val file = File(context.filesDir, filename)
            file.writeText(content)
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun readFile(filename: String): String? {
        val file = File(context.filesDir, filename)
        if (!file.exists()) {
            return null
        }
        return try {
            file.readText()
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun deleteFile(filename: String): Boolean {
        val file = File(context.filesDir, filename)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
    
    fun listFiles(): List<String> {
        val files = context.filesDir.listFiles()
        return files?.map { it.name } ?: emptyList()
    }
}
