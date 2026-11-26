package com.k0h5.todolist.vm

import com.k0h5.todolist.data.local.Todo

// 목록 한 줄 = 할 일 + 체크박스 선택 여부 + 마감 지남 여부
// overdue를 값으로 들고 있어야 날짜가 바뀌었을 때 DiffUtil이 그 줄을 다시 그림
data class TodoRow(val todo: Todo, val selected: Boolean, val overdue: Boolean = false) {
    companion object {
        fun of(list: List<Todo>, selectedIds: Set<Long>, today: String): List<TodoRow> =
            list.map { TodoRow(it, it.id in selectedIds, it.isOverdue(today)) }

        // 목록에서 해당 id의 위치. 없으면(아직 반영 전이거나 필터에 안 보이면) null
        fun positionOf(rows: List<TodoRow>, id: Long): Int? = rows.indexOfFirst { it.todo.id == id }.takeIf { it >= 0 }
    }
}

object TodoSelection {
    fun toggle(selected: Set<Long>, id: Long): Set<Long> = if (id in selected) selected - id else selected + id

    // 선택 중이면 항목 탭 = 선택 토글한 결과, 선택 중이 아니면 null (→ 평소처럼 편집 화면)
    fun onTap(selected: Set<Long>, id: Long): Set<Long>? = if (selected.isEmpty()) null else toggle(selected, id)
}
