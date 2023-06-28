package com.example.cameraxapp.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import java.io.IOException



object BitmapUtils {
    @Throws(IOException::class)
    fun uriToBitmap(context: Context, uri: Uri?): Bitmap {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri!!)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        return bitmap
    }

}