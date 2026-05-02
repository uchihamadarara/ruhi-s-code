package com.example.ruhi1

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.net.Uri

class PhotoManager(private val context: Context) {

    fun getPhotoCount(): Int {
        var count = 0
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)
        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                count = cursor.count
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return count
    }

    fun openGallery() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            type = "image/*"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun deleteLatestPhoto(): Boolean {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        
        try {
            context.contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                    val deleteUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    val deletedRows = context.contentResolver.delete(deleteUri, null, null)
                    return deletedRows > 0
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}
