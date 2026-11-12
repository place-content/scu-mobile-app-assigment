package com.k0h5.todolist.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.k0h5.todolist.data.TodoRepository
import com.k0h5.todolist.data.local.Todo
import com.k0h5.todolist.data.local.TodoDatabase
import com.k0h5.todolist.data.local.TodoStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TodoListViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TodoRepository(TodoDatabase.get(app))

    private val _filter = MutableStateFlow(TodoFilter.ALL)
    val filter: StateFlow<TodoFilter> = _filter

    // 체크박스로 선택한 할 일 id (일괄 삭제용)
    private val _selected = MutableStateFlow<Set<Long>>(emptySet())
    val selected: StateFlow<Set<Long>> = _selected

    // 전체 목록 + 현재 필터 + 선택 상태 → 화면에 보여줄 목록
    val rows: Flow<List<TodoRow>> =
        combine(repo.all, _filter, _selected) { list, f, sel -> TodoRow.of(f.apply(list), sel) }

    // 필터 버튼에 표시할 개수
    val counts: Flow<Map<TodoFilter, Int>> = repo.all.map { TodoFilter.countAll(it) }

    fun setFilter(f: TodoFilter) {
        if (_filter.value == f) return
        _filter.value = f
        clearSelection() // 안 보이는 항목이 같이 삭제되지 않도록
    }

    fun toggleSelect(id: Long) = _selected.update { if (id in it) it - id else it + id }

    fun clearSelection() {
        _selected.value = emptySet()
    }

    fun deleteSelected() {
        val ids = _selected.value
        clearSelection()
        viewModelScope.launch { repo.deleteAll(ids) }
    }

    suspend fun getById(id: Long): Todo? = repo.getById(id)

    fun add(title: String, note: String, dueDate: String) = viewModelScope.launch { repo.add(title, note, dueDate) }

    fun update(id: Long, title: String, note: String, dueDate: String, status: TodoStatus) =
        viewModelScope.launch { repo.update(id, title, note, dueDate, status) }

    fun setStatus(id: Long, status: TodoStatus) = viewModelScope.launch { repo.setStatus(id, status) }

    fun delete(id: Long) {
        _selected.update { it - id }
        viewModelScope.launch { repo.delete(id) }
    }
}
