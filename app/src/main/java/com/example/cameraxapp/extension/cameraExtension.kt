package com.example.cameraxapp.util

import android.view.View
import android.view.ViewTreeObserver



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

