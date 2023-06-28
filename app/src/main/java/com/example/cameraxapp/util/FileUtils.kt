package com.example.cameraxapp.util

import android.content.Context
import com.example.cameraxapp.R
import java.io.File


// when saved uri->  file:///storage/emulated/0/Android/media/com.example.cameraxapp/CameraXApp/2023-06-27-18-34-41-713.jpg

// get output directory
fun Context.getOutputDirectory(): File {
    val mediaDir = externalMediaDirs.firstOrNull()?.let {
        File(it, resources.getString(R.string.app_name)).apply {
            mkdir() } }
    return if(mediaDir != null && mediaDir.exists())
        mediaDir else filesDir
}