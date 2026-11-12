package com.k0h5.todolist.data

import com.k0h5.todolist.data.local.TodoStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodoStatusTest {
    @Test
    fun next_movesForward_andStopsAtDone() {
        assertEquals(TodoStatus.IN_PROGRESS, TodoStatus.TODO.next())
        assertEquals(TodoStatus.DONE, TodoStatus.IN_PROGRESS.next())
        assertNull(TodoStatus.DONE.next())
    }
}
