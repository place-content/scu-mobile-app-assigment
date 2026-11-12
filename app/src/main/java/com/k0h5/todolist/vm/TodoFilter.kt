package com.k0h5.todolist.vm

import com.k0h5.todolist.data.local.Todo
import com.k0h5.todolist.data.local.TodoStatus

enum class TodoFilter(val status: TodoStatus?) {
    ALL(null),
    TODO(TodoStatus.TODO),
    IN_PROGRESS(TodoStatus.IN_PROGRESS),
    DONE(TodoStatus.DONE);

    fun apply(list: List<Todo>): List<Todo> =
        if (status == null) list else list.filter { it.status == status }

    companion object {
        fun countAll(list: List<Todo>): Map<TodoFilter, Int> =
            entries.associateWith { it.apply(list).size }
    }
}
