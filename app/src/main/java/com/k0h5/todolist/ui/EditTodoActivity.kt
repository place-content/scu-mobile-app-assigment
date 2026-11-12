package com.k0h5.todolist.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.k0h5.todolist.R
import com.k0h5.todolist.data.local.TodoStatus
import com.k0h5.todolist.databinding.ActivityEditTodoBinding
import com.k0h5.todolist.vm.TodoInput
import com.k0h5.todolist.vm.TodoListViewModel
import kotlinx.coroutines.launch

class EditTodoActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_ID = "extra_id"          // 편집할 Todo id (입력/출력)
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_NOTE = "extra_note"
        const val EXTRA_DUE = "extra_due"
        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_DELETE = "extra_delete"  // true면 삭제 요청
        private const val STATE_ORIGINAL_DUE = "state_original_due"
    }

    private lateinit var binding: ActivityEditTodoBinding
    private val vm: TodoListViewModel by viewModels()
    private var todoId: Long = -1L
    // 불러온 원래 마감일 (이미 지난 날짜라도 그대로 두는 건 허용하기 위해 보관)
    private var originalDue = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditTodoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.editTodoView.applySystemBarPadding()
        binding.etDue.setupDuePicker { earliestDue() }

        binding.spStatus.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            TodoStatus.entries.map { it.label }
        )

        todoId = intent.getLongExtra(EXTRA_ID, -1L)
        // 화면 회전 등으로 다시 만들어질 땐 입력 중이던 값을 덮어쓰지 않음
        if (savedInstanceState == null) {
            loadTodo()
        } else {
            originalDue = savedInstanceState.getString(STATE_ORIGINAL_DUE).orEmpty()
        }

        binding.btnSave.setOnClickListener { save() }
        binding.toolbar.setNavigationOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
        binding.btnDelete.setOnClickListener { confirmDelete() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_ORIGINAL_DUE, originalDue)
    }

    private fun earliestDue(): String = TodoInput.earliestDue(todayDue(), originalDue)

    private fun loadTodo() {
        lifecycleScope.launch {
            val todo = vm.getById(todoId)
            if (todo == null) {
                Toast.makeText(this@EditTodoActivity, R.string.msg_not_found, Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            binding.etTitle.setText(todo.title)
            binding.etNote.setText(todo.note)
            originalDue = todo.dueDate
            binding.etDue.setText(todo.dueDate)
            binding.spStatus.setSelection(todo.status.ordinal)
        }
    }

    private fun save() {
        val title = binding.etTitle.text.toString().trim()
        val note = binding.etNote.text.toString().trim()
        val due = binding.etDue.text.toString().trim()
        val status = TodoStatus.entries[binding.spStatus.selectedItemPosition]

        val error = TodoInput.validate(title, due, earliestDue = earliestDue())
        if (error != null) {
            Toast.makeText(this, getString(error.messageRes()), Toast.LENGTH_SHORT).show()
            return
        }

        val data = Intent()
            .putExtra(EXTRA_ID, todoId)
            .putExtra(EXTRA_TITLE, title)
            .putExtra(EXTRA_NOTE, note)
            .putExtra(EXTRA_DUE, due)
            .putExtra(EXTRA_STATUS, status.name)
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setMessage(R.string.confirm_delete)
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                val data = Intent()
                    .putExtra(EXTRA_ID, todoId)
                    .putExtra(EXTRA_DELETE, true)
                setResult(Activity.RESULT_OK, data)
                finish()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .showDeleteConfirm()
    }
}
