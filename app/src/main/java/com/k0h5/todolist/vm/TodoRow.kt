package com.k0h5.todolist.vm

import com.k0h5.todolist.data.local.Todo

// 목록 한 줄 = 할 일 + 체크박스 선택 여부
data class TodoRow(val todo: Todo, val selected: Boolean) {
    companion object {
        fun of(list: List<Todo>, selectedIds: Set<Long>): List<TodoRow> =
            list.map { TodoRow(it, it.id in selectedIds) }
    }
}
