package com.k0h5.todolist.ui

import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.k0h5.todolist.R

// 삭제 확인 창: "삭제" 버튼만 빨간 계열로 표시
fun AlertDialog.Builder.showDeleteConfirm(): AlertDialog {
    val dialog = show()
    dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        ?.setTextColor(ContextCompat.getColor(context, R.color.danger))
    return dialog
}
