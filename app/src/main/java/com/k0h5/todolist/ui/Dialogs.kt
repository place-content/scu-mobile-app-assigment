package com.k0h5.todolist.ui

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.appbar.MaterialToolbar
import com.k0h5.todolist.R

// 삭제 확인 창: "삭제" 버튼만 빨간 계열로 표시
fun AlertDialog.Builder.showDeleteConfirm(): AlertDialog = show().apply { tintPositiveDanger() }

private fun AlertDialog.tintPositiveDanger() {
    getButton(DialogInterface.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(context, R.color.danger))
}

// "나갈까요?" 확인 창. DialogFragment라서 창이 떠 있는 채로 회전해도 다시 그려짐
class DiscardChangesDialog : DialogFragment() {
    companion object {
        const val TAG = "discard_changes"
        const val RESULT_DISCARD = "result_discard_changes"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(requireContext())
        .setMessage(R.string.confirm_discard)
        .setPositiveButton(R.string.btn_discard) { _, _ ->
            parentFragmentManager.setFragmentResult(RESULT_DISCARD, Bundle.EMPTY)
        }
        .setNegativeButton(R.string.btn_keep_editing, null)
        .create()
        .apply { setOnShowListener { tintPositiveDanger() } }
}

// 상단 < 와 기기 뒤로가기 = 취소. 입력이 바뀌었으면 실수로 날리지 않게 한 번 물어봄
// onCreate에서 부르면 회전 후 새 화면에도 "나가기" 결과를 받는 리스너가 다시 붙음
fun AppCompatActivity.cancelOnBack(toolbar: MaterialToolbar, hasChanges: () -> Boolean) {
    val leave = {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }
    supportFragmentManager.setFragmentResultListener(DiscardChangesDialog.RESULT_DISCARD, this) { _, _ -> leave() }
    onBackPressedDispatcher.addCallback(
        this,
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!hasChanges()) return leave()
                if (supportFragmentManager.findFragmentByTag(DiscardChangesDialog.TAG) == null) {
                    DiscardChangesDialog().show(supportFragmentManager, DiscardChangesDialog.TAG)
                }
            }
        }
    )
    toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
}
