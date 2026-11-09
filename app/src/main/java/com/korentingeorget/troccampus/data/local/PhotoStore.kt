package com.korentingeorget.troccampus.data.local

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PhotoStore(private val context: Context) {
    private val photosDir: File
        get() = File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }

    suspend fun saveBytes(bytes: ByteArray, prefix: String = "ad"): String = withContext(Dispatchers.IO) {
        val file = File(photosDir, "${prefix}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { it.write(bytes) }
        file.absolutePath
    }

    suspend fun copyUri(sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(photosDir, "shared_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun uriFor(path: String): Uri {
        val file = File(path)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    suspend fun delete(path: String?) = withContext(Dispatchers.IO) {
        if (path.isNullOrBlank()) return@withContext
        try {
            val file = File(path)
            if (file.exists()) file.delete()
        } catch (_: Exception) {}
    }
}
