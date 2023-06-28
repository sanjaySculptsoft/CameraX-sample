package com.example.cameraxapp.extension

import android.app.Activity
import android.content.ContentValues
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.provider.MediaStore
import com.example.cameraxapp.MainActivity
import java.text.SimpleDateFormat
import java.util.Locale


fun Activity?.saveImageToGallery(bitmap: Bitmap, resultCallback: (String) -> Unit,) {
    val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

    val contentResolver = this?.contentResolver
    val displayName = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())// The desired display name of the image

    // Create a new ContentValues object to store the image details
    val values = ContentValues()
    values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg") // Adjust the MIME type if necessary

    // Insert the image details into the MediaStore and get the content URI
    // Note: This requires the WRITE_EXTERNAL_STORAGE permission
    val imageUri = contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    try {
        // Open an OutputStream to the content URI and write the image data
        val outputStream = contentResolver?.openOutputStream(imageUri!!)
        if (outputStream != null) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.close()
            resultCallback.invoke("Saved image to gallery")
        }
    } catch (e: java.lang.Exception) {
        resultCallback.invoke("Failed to save image to gallery")
        e.printStackTrace()
    }
}

// Method to play the capture sound
 fun Activity?.playCaptureSound(cameraCaptureSound: Int) {
    val mediaPlayer: MediaPlayer = MediaPlayer.create(
        this,
        cameraCaptureSound
    )
    mediaPlayer.setOnCompletionListener { // Release the media player resources
        mediaPlayer.release()
    }
    mediaPlayer.start()
}