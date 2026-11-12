package com.k0h5.todolist.ui

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun View.applySystemBarPadding() {
    val base = intArrayOf(paddingLeft, paddingTop, paddingRight, paddingBottom)
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
        v.setPadding(base[0] + bars.left, base[1] + bars.top, base[2] + bars.right, base[3] + bars.bottom)
        insets
    }
}
