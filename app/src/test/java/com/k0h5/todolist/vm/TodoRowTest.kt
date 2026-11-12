package com.k0h5.todolist.vm

import com.k0h5.todolist.data.local.Todo
import org.junit.Assert.assertEquals
import org.junit.Test

class TodoRowTest {
    private val list = listOf(Todo(id = 1, title = "a"), Todo(id = 2, title = "b"), Todo(id = 3, title = "c"))

    @Test
    fun marksOnlySelectedIds() {
        val rows = TodoRow.of(list, setOf(1L, 3L))
        assertEquals(listOf(true, false, true), rows.map { it.selected })
    }

    @Test
    fun noSelection_allUnselected() {
        assertEquals(listOf(false, false, false), TodoRow.of(list, emptySet()).map { it.selected })
    }

    @Test
    fun selectedIdNotInList_isIgnored() {
        val rows = TodoRow.of(list, setOf(99L))
        assertEquals(3, rows.size)
        assertEquals(0, rows.count { it.selected })
    }
}
