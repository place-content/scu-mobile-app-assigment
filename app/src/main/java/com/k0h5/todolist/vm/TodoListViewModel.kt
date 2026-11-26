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

    // 오늘 날짜 (YYYY-MM-DD). 화면이 주기적으로 넣어 줌 → 자정이 지나면 "지남" 표시가 갱신됨
    private val today = MutableStateFlow("")

    // 방금 추가한 할 일 id. 목록에 나타나면 화면이 그 위치로 한 번 스크롤하고 비움
    // DB 목록 갱신과 id 확정 중 어느 쪽이 먼저 올지 모르므로 화면이 양쪽에서 확인할 수 있게 Flow로 둠
    private val _justAdded = MutableStateFlow<Long?>(null)
    val justAdded: StateFlow<Long?> = _justAdded

    // 전체 목록 + 현재 필터 + 선택 상태 + 오늘 날짜 → 화면에 보여줄 목록
    val rows: Flow<List<TodoRow>> =
        combine(repo.all, _filter, _selected, today) { list, f, sel, day -> TodoRow.of(f.apply(list), sel, day) }

    // 필터 버튼에 표시할 개수
    val counts: Flow<Map<TodoFilter, Int>> = repo.all.map { TodoFilter.countAll(it) }

    fun setToday(day: String) {
        today.value = day // 같은 값이면 StateFlow가 무시하므로 자주 불러도 됨
    }

    fun setFilter(f: TodoFilter) {
        if (_filter.value == f) return
        _filter.value = f
        clearSelection() // 안 보이는 항목이 같이 삭제되지 않도록
        _justAdded.value = null // 필터를 바꾼 뒤엔 예전에 추가한 항목으로 튀지 않게
    }

    fun toggleSelect(id: Long) = _selected.update { TodoSelection.toggle(it, id) }

    // 선택 중이면 항목 탭 = 선택 토글하고 true, 아니면 false → 화면이 평소처럼 편집을 엶
    fun toggleIfSelecting(id: Long): Boolean {
        _selected.value = TodoSelection.onTap(_selected.value, id) ?: return false
        return true
    }

    fun clearSelection() {
        _selected.value = emptySet()
    }

    fun deleteSelected() {
        val ids = _selected.value
        clearSelection()
        viewModelScope.launch { repo.deleteAll(ids) }
    }

    // 방금 추가한 항목이 rows에 있으면 그 위치를 돌려주고 비움. 추가 직후가 아니면 항상 null
    fun takeJustAddedPosition(rows: List<TodoRow>): Int? {
        val id = _justAdded.value ?: return null
        return TodoRow.positionOf(rows, id)?.also { _justAdded.value = null }
    }

    suspend fun getById(id: Long): Todo? = repo.getById(id)

    fun add(title: String, note: String, dueDate: String) =
        viewModelScope.launch { _justAdded.value = repo.add(title, note, dueDate) }

    fun update(id: Long, title: String, note: String, dueDate: String, status: TodoStatus) =
        viewModelScope.launch { repo.update(id, title, note, dueDate, status) }

    fun setStatus(id: Long, status: TodoStatus) = viewModelScope.launch { repo.setStatus(id, status) }

    fun delete(id: Long) {
        _selected.update { it - id }
        viewModelScope.launch { repo.delete(id) }
    }
}
