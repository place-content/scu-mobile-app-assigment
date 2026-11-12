package com.k0h5.todolist.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.k0h5.todolist.R
import com.k0h5.todolist.data.local.Todo
import com.k0h5.todolist.data.local.TodoStatus
import com.k0h5.todolist.databinding.ActivityTodoListBinding
import com.k0h5.todolist.vm.TodoFilter
import com.k0h5.todolist.vm.TodoListViewModel
import kotlinx.coroutines.launch

class TodoListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTodoListBinding
    private lateinit var adapter: TodoAdapter
    private val vm: TodoListViewModel by viewModels()

    // 필터 라디오 버튼 id <-> 필터 값
    private val filterButtons by lazy {
        mapOf(
            TodoFilter.ALL to binding.rbAll,
            TodoFilter.TODO to binding.rbTodo,
            TodoFilter.IN_PROGRESS to binding.rbInProgress,
            TodoFilter.DONE to binding.rbDone
        )
    }

    // AddTodoActivity 결과 수신
    private val addTodoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode != Activity.RESULT_OK || data == null) return@registerForActivityResult
        vm.add(
            title = data.getStringExtra(AddTodoActivity.EXTRA_TITLE).orEmpty(),
            note = data.getStringExtra(AddTodoActivity.EXTRA_NOTE).orEmpty(),
            dueDate = data.getStringExtra(AddTodoActivity.EXTRA_DUE).orEmpty()
        )
        Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show()
    }

    // EditTodoActivity 결과 수신 (수정 또는 삭제)
    private val editTodoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode != Activity.RESULT_OK || data == null) return@registerForActivityResult
        val id = data.getLongExtra(EditTodoActivity.EXTRA_ID, -1L)
        if (id <= 0) return@registerForActivityResult

        if (data.getBooleanExtra(EditTodoActivity.EXTRA_DELETE, false)) {
            vm.delete(id)
            Toast.makeText(this, R.string.msg_deleted, Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        val status = data.getStringExtra(EditTodoActivity.EXTRA_STATUS)
            ?.let { name -> TodoStatus.entries.firstOrNull { it.name == name } }
            ?: TodoStatus.TODO
        vm.update(
            id = id,
            title = data.getStringExtra(EditTodoActivity.EXTRA_TITLE).orEmpty(),
            note = data.getStringExtra(EditTodoActivity.EXTRA_NOTE).orEmpty(),
            dueDate = data.getStringExtra(EditTodoActivity.EXTRA_DUE).orEmpty(),
            status = status
        )
        Toast.makeText(this, R.string.msg_updated, Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTodoListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.todoListView.applySystemBarPadding()

        adapter = TodoAdapter(
            onSelect = { todo -> vm.toggleSelect(todo.id) },
            onMove = { todo -> todo.status.next()?.let { vm.setStatus(todo.id, it) } },
            onClick = { todo -> openEdit(todo.id) },
            onLongPress = { todo -> showItemMenu(todo) }
        )
        binding.rvTodos.layoutManager = LinearLayoutManager(this)
        binding.rvTodos.adapter = adapter

        binding.btnAddTodo.setOnClickListener {
            addTodoLauncher.launch(Intent(this, AddTodoActivity::class.java))
        }

        binding.btnClearSelection.setOnClickListener { vm.clearSelection() }
        binding.btnDeleteSelected.setOnClickListener { confirmDeleteSelected() }

        // 선택 중일 때 뒤로가기 = 선택 해제
        onBackPressedDispatcher.addCallback(this, backToClearSelection)

        binding.rgFilter.setOnCheckedChangeListener { _, checkedId ->
            val selected = filterButtons.entries.firstOrNull { it.value.id == checkedId }?.key
            if (selected != null) vm.setFilter(selected)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    vm.rows.collect { rows ->
                        adapter.submitList(rows)
                        binding.tvEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    vm.counts.collect { counts -> updateFilterLabels(counts) }
                }
                launch {
                    vm.selected.collect { ids -> updateBottomBar(ids.size) }
                }
            }
        }
    }

    private val backToClearSelection = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = vm.clearSelection()
    }

    private fun updateBottomBar(selectedCount: Int) {
        val selecting = selectedCount > 0
        binding.btnAddTodo.visibility = if (selecting) View.GONE else View.VISIBLE
        binding.btnClearSelection.visibility = if (selecting) View.VISIBLE else View.GONE
        binding.btnDeleteSelected.visibility = if (selecting) View.VISIBLE else View.GONE
        binding.btnDeleteSelected.text = getString(R.string.btn_delete_selected, selectedCount)
        backToClearSelection.isEnabled = selecting
    }

    private fun confirmDeleteSelected() {
        val count = vm.selected.value.size
        if (count == 0) return
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.confirm_delete_selected, count))
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                vm.deleteSelected()
                Toast.makeText(this, getString(R.string.msg_deleted_count, count), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .showDeleteConfirm()
    }

    private fun updateFilterLabels(counts: Map<TodoFilter, Int>) {
        binding.rbAll.text = getString(R.string.filter_all, counts[TodoFilter.ALL] ?: 0)
        binding.rbTodo.text = getString(R.string.filter_todo, counts[TodoFilter.TODO] ?: 0)
        binding.rbInProgress.text = getString(R.string.filter_in_progress, counts[TodoFilter.IN_PROGRESS] ?: 0)
        binding.rbDone.text = getString(R.string.filter_done, counts[TodoFilter.DONE] ?: 0)
    }

    private fun openEdit(id: Long) {
        val intent = Intent(this, EditTodoActivity::class.java)
            .putExtra(EditTodoActivity.EXTRA_ID, id)
        editTodoLauncher.launch(intent)
    }

    private fun showItemMenu(todo: Todo) {
        val items = arrayOf(
            getString(R.string.menu_status),
            getString(R.string.menu_edit),
            getString(R.string.menu_delete)
        )
        AlertDialog.Builder(this)
            .setTitle(todo.title)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showStatusDialog(todo)
                    1 -> openEdit(todo.id)
                    2 -> confirmDelete(todo)
                }
            }
            .show()
    }

    private fun showStatusDialog(todo: Todo) {
        val labels = TodoStatus.entries.map { it.label }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.menu_status)
            .setSingleChoiceItems(labels, todo.status.ordinal) { dialog, which ->
                vm.setStatus(todo.id, TodoStatus.entries[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun confirmDelete(todo: Todo) {
        AlertDialog.Builder(this)
            .setMessage(R.string.confirm_delete)
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                vm.delete(todo.id)
                Toast.makeText(this, R.string.msg_deleted, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .showDeleteConfirm()
    }
}
