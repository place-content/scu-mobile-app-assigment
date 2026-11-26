package com.k0h5.todolist.ui

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.k0h5.todolist.R
import com.k0h5.todolist.databinding.ActivityAddTodoBinding
import com.k0h5.todolist.vm.TodoInput

class AddTodoActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_NOTE = "extra_note"
        const val EXTRA_DUE = "extra_due"
    }

    private lateinit var binding: ActivityAddTodoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddTodoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.addTodoView.applySystemBarPadding()
        binding.etDue.setupDuePicker { todayDue() }

        binding.btnSave.setOnClickListener { save() }
        // 비활성 저장 버튼을 누르면 터치가 감싼 영역으로 넘어옴 → 빈 필수 칸 표시
        binding.saveButtonArea.setOnClickListener { showTitleError(true) }
        binding.etTitle.doAfterTextChanged { updateSaveEnabled() }
        updateSaveEnabled()

        // 새 할 일은 빈 입력이 원래 값
        cancelOnBack(binding.toolbar) { TodoInput.hasChanges(TodoInput.Draft(), currentDraft()) }
    }

    // 제목이 비었으면 저장 버튼 비활성. 제목을 채우면 빨간 표시도 지움
    private fun updateSaveEnabled() {
        val canSave = TodoInput.canSave(binding.etTitle.text.toString())
        binding.btnSave.isEnabled = canSave
        // 비활성 버튼도 clickable이면 터치를 삼켜 버리므로, 감싼 영역이 받을 수 있게 꺼 둠
        binding.btnSave.isClickable = canSave
        if (canSave) showTitleError(false)
    }

    private fun showTitleError(show: Boolean) {
        binding.etTitle.backgroundTintList =
            if (show) ColorStateList.valueOf(ContextCompat.getColor(this, R.color.danger)) else null
        binding.tvTitleError.visibility = if (show) View.VISIBLE else View.GONE
        if (show) binding.etTitle.requestFocus()
    }

    private fun currentDraft() = TodoInput.Draft(
        title = binding.etTitle.text.toString(),
        note = binding.etNote.text.toString(),
        due = binding.etDue.text.toString()
    ).trimmed()

    private fun save() {
        val (title, note, due) = currentDraft()

        val error = TodoInput.validate(title, due, earliestDue = todayDue())
        if (error != null) {
            Toast.makeText(this, getString(error.messageRes()), Toast.LENGTH_SHORT).show()
            return
        }

        val data = Intent()
            .putExtra(EXTRA_TITLE, title)
            .putExtra(EXTRA_NOTE, note)
            .putExtra(EXTRA_DUE, due)
        setResult(Activity.RESULT_OK, data)
        finish()
    }
}
