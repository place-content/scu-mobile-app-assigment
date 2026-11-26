package com.k0h5.todolist.vm

import com.k0h5.todolist.data.local.Todo
import com.k0h5.todolist.data.local.TodoStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverdueTest {
    private val today = "2026-11-26"

    private fun todo(due: String, status: TodoStatus = TodoStatus.TODO) =
        Todo(title = "a", dueDate = due, status = status)

    @Test
    fun pastDue_notDone_isOverdue() {
        assertTrue(todo("2026-11-25").isOverdue(today))
        assertTrue(todo("2025-12-31", TodoStatus.IN_PROGRESS).isOverdue(today))
    }

    @Test
    fun todayOrFuture_isNotOverdue() {
        assertFalse(todo(today).isOverdue(today))
        assertFalse(todo("2026-12-01").isOverdue(today))
    }

    @Test
    fun noDueOrDone_isNotOverdue() {
        assertFalse(todo("").isOverdue(today))
        assertFalse(todo("2026-11-01", TodoStatus.DONE).isOverdue(today))
    }
}
