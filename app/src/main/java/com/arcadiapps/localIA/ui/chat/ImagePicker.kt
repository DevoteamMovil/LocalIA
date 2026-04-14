package com.arcadiapps.localIA.ui.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class PickedImage(val uri: Uri, val localPath: String, val bitmap: Bitmap)

@Composable
fun rememberImagePicker(onImagePicked: (PickedImage) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val picked = saveImageLocally(context, it)
            if (picked != null) onImagePicked(picked)
        }
    }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

private fun saveImageLocally(context: Context, uri: Uri): PickedImage? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        // Escalar si es muy grande (max 1024px)
        val scaled = scaleBitmap(bitmap, 1024)

        val imagesDir = File(context.filesDir, "chat_images")
        imagesDir.mkdirs()
        val file = File(imagesDir, "${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        PickedImage(uri = uri, localPath = file.absolutePath, bitmap = scaled)
    } catch (e: Exception) {
        null
    }
}

private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= maxSize && h <= maxSize) return bitmap
    val ratio = minOf(maxSize.toFloat() / w, maxSize.toFloat() / h)
    return Bitmap.createScaledBitmap(bitmap, (w * ratio).toInt(), (h * ratio).toInt(), true)
}
