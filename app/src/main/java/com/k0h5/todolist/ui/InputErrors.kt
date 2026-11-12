package com.k0h5.todolist.ui

import androidx.annotation.StringRes
import com.k0h5.todolist.R
import com.k0h5.todolist.vm.TodoInput

@StringRes
fun TodoInput.Error.messageRes(): Int = when (this) {
    TodoInput.Error.TITLE_REQUIRED -> R.string.msg_title_required
    TodoInput.Error.DUE_INVALID -> R.string.msg_due_invalid
    TodoInput.Error.DUE_PAST -> R.string.msg_due_past
}
