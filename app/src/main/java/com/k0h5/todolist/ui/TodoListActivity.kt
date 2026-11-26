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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TodoListActivity : AppCompatActivity() {
    companion object {
        private const val TODAY_CHECK_MS = 60_000L
    }

    private lateinit var binding: ActivityTodoListBinding
    private lateinit var adapter: TodoAdapter
    private val vm: TodoListViewModel by viewModels()

    // 필터 칩 <-> 필터 값
    private val filterButtons by lazy {
        mapOf(
            TodoFilter.ALL to binding.chipAll,
            TodoFilter.TODO to binding.chipTodo,
            TodoFilter.IN_PROGRESS to binding.chipInProgress,
            TodoFilter.DONE to binding.chipDone
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
            onClick = { todo -> if (!vm.toggleIfSelecting(todo.id)) openEdit(todo.id) },
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

        binding.cgFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val selected = filterButtons.entries.firstOrNull { it.value.id == checkedId }?.key
            if (selected != null) vm.setFilter(selected)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    vm.rows.collect { rows ->
                        // 목록 반영이 끝난 뒤, 방금 추가한 항목이 있으면 그 위치로 (회전·필터 변경 땐 스크롤 안 함)
                        adapter.submitList(rows) { scrollToJustAdded() }
                        binding.tvEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
                        // 필터 결과가 비었을 땐 "완료 항목이 없습니다"처럼 어떤 상태인지 알려줌
                        binding.tvEmpty.text = vm.filter.value.status
                            ?.let { getString(R.string.empty_filtered, it.label) }
                            ?: getString(R.string.empty_list)
                    }
                }
                launch {
                    vm.counts.collect { counts -> updateFilterLabels(counts) }
                }
                launch {
                    vm.selected.collect { ids -> updateSelectionBar(ids.size) }
                }
                launch {
                    // 목록이 id 확정보다 먼저 갱신됐을 수도 있으니 id가 정해질 때도 확인
                    vm.justAdded.collect { scrollToJustAdded() }
                }
                launch {
                    // 화면이 보이는 동안 1분마다 날짜 확인 → 자정이 지나면 "지남" 표시 갱신
                    while (true) {
                        vm.setToday(todayDue())
                        delay(TODAY_CHECK_MS)
                    }
                }
            }
        }
    }

    private fun scrollToJustAdded() {
        vm.takeJustAddedPosition(adapter.currentList)?.let { binding.rvTodos.scrollToPosition(it) }
    }

    private val backToClearSelection = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = vm.clearSelection()
    }

    private fun updateSelectionBar(selectedCount: Int) {
        val selecting = selectedCount > 0
        // 선택 중이면 상단 헤더(제목 + 추가) 자리를 선택 바(n개 선택됨 · 선택 해제 · 삭제)로 바꿈
        binding.headerDefault.visibility = if (selecting) View.GONE else View.VISIBLE
        binding.selectionBar.visibility = if (selecting) View.VISIBLE else View.GONE
        binding.tvSelectionCount.text = getString(R.string.selection_count, selectedCount)
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
        binding.chipAll.text = getString(R.string.filter_all, counts[TodoFilter.ALL] ?: 0)
        binding.chipTodo.text = getString(R.string.filter_todo, counts[TodoFilter.TODO] ?: 0)
        binding.chipInProgress.text = getString(R.string.filter_in_progress, counts[TodoFilter.IN_PROGRESS] ?: 0)
        binding.chipDone.text = getString(R.string.filter_done, counts[TodoFilter.DONE] ?: 0)
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
