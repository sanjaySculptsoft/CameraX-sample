package com.example.cameraxapp.util

import android.view.View
import android.view.ViewTreeObserver
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo


// utility function
inline fun View.afterMeasured(crossinline block: () -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (measuredWidth > 0 && measuredHeight > 0) {
                viewTreeObserver.removeOnGlobalLayoutListener(this) //  Remove a previously installed global layout callback
                block() // call block
            }
        }
    })
}

// Method to set zoom level
// fun Camera?.setZoomLevel(zoomValue: Float) {
//    this?.cameraControl?.setZoomRatio(zoomValue)
//}
// Method to set zoom level
// Method to set zoom level
 fun Camera?.setZoomLevel(zoomValue: Float) {
    val maxZoom = this?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1.0f
    val clampedZoom = zoomValue.coerceIn(1.0f, maxZoom)
    this?.cameraControl?.setZoomRatio(clampedZoom)
}