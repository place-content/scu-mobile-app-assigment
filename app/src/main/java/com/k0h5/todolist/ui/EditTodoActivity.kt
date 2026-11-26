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
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.k0h5.todolist.R
import com.k0h5.todolist.data.local.TodoStatus
import com.k0h5.todolist.databinding.ActivityEditTodoBinding
import com.k0h5.todolist.vm.TodoInput
import com.k0h5.todolist.vm.TodoListViewModel
import kotlinx.coroutines.launch

class EditTodoActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_ID = "extra_id" // 편집할 Todo id (입력/출력)
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_NOTE = "extra_note"
        const val EXTRA_DUE = "extra_due"
        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_DELETE = "extra_delete" // true면 삭제 요청
        private const val STATE_ORIGINAL = "state_original"
        private const val KEY_TITLE = "title"
        private const val KEY_NOTE = "note"
        private const val KEY_DUE = "due"
        private const val KEY_STATUS = "status"

        // 회전 대비 원래 값 저장/복원 (순서에 의존하는 배열 대신 키로 저장)
        private fun TodoInput.Draft.toBundle() =
            bundleOf(KEY_TITLE to title, KEY_NOTE to note, KEY_DUE to due, KEY_STATUS to status.name)

        private fun Bundle.toDraft() = TodoInput.Draft(
            title = getString(KEY_TITLE).orEmpty(),
            note = getString(KEY_NOTE).orEmpty(),
            due = getString(KEY_DUE).orEmpty(),
            status = TodoStatus.valueOf(getString(KEY_STATUS) ?: TodoStatus.TODO.name)
        )
    }

    private lateinit var binding: ActivityEditTodoBinding
    private val vm: TodoListViewModel by viewModels()
    private var todoId: Long = -1L

    // DB에서 불러온 원래 값 (불러오기 전엔 null)
    // 변경 여부 확인 + 이미 지난 원래 마감일을 그대로 두는 걸 허용하는 데 씀
    private var original: TodoInput.Draft? = null

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
        // 단, 불러오기가 끝나기 전에 회전했다면 저장된 원래 값이 없으니 다시 불러옴
        original = savedInstanceState?.getBundle(STATE_ORIGINAL)?.toDraft()
        if (original == null) loadTodo()

        binding.btnSave.setOnClickListener { save() }
        // 불러오는 중(original == null)엔 사용자가 바꾼 게 없으므로 바로 나감. 불러오면 그 값과 비교
        cancelOnBack(binding.toolbar) { original?.let { TodoInput.hasChanges(it, currentDraft()) } ?: false }
        binding.btnDelete.setOnClickListener { confirmDelete() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        original?.let { outState.putBundle(STATE_ORIGINAL, it.toBundle()) }
    }

    private fun earliestDue(): String = TodoInput.earliestDue(todayDue(), original?.due.orEmpty())

    private fun currentDraft() = TodoInput.Draft(
        title = binding.etTitle.text.toString(),
        note = binding.etNote.text.toString(),
        due = binding.etDue.text.toString(),
        status = TodoStatus.entries[binding.spStatus.selectedItemPosition]
    ).trimmed()

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
            original = TodoInput.Draft(todo.title, todo.note, todo.dueDate, todo.status)
            binding.etDue.setText(todo.dueDate)
            binding.spStatus.setSelection(todo.status.ordinal)
        }
    }

    private fun save() {
        // 아직 불러오는 중이면 저장하지 않음 (빈 값으로 덮어쓰는 것 방지)
        if (original == null) return
        val draft = currentDraft()

        val error = TodoInput.validate(draft.title, draft.due, earliestDue = earliestDue())
        if (error != null) {
            Toast.makeText(this, getString(error.messageRes()), Toast.LENGTH_SHORT).show()
            return
        }

        val data = Intent()
            .putExtra(EXTRA_ID, todoId)
            .putExtra(EXTRA_TITLE, draft.title)
            .putExtra(EXTRA_NOTE, draft.note)
            .putExtra(EXTRA_DUE, draft.due)
            .putExtra(EXTRA_STATUS, draft.status.name)
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
