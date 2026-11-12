package com.k0h5.todolist.vm

import com.k0h5.todolist.data.local.Todo
import com.k0h5.todolist.data.local.TodoStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoFilterTest {
    private val list = listOf(
        Todo(id = 1, title = "a", status = TodoStatus.TODO),
        Todo(id = 2, title = "b", status = TodoStatus.IN_PROGRESS),
        Todo(id = 3, title = "c", status = TodoStatus.DONE),
        Todo(id = 4, title = "d", status = TodoStatus.TODO)
    )

    @Test
    fun all_returnsEverything() {
        assertEquals(list, TodoFilter.ALL.apply(list))
    }

    @Test
    fun eachFilter_returnsOnlyMatchingStatus() {
        assertEquals(listOf(1L, 4L), TodoFilter.TODO.apply(list).map { it.id })
        assertEquals(listOf(2L), TodoFilter.IN_PROGRESS.apply(list).map { it.id })
        assertEquals(listOf(3L), TodoFilter.DONE.apply(list).map { it.id })
    }

    @Test
    fun filter_keepsOriginalOrder() {
        val reversed = list.reversed()
        assertEquals(listOf(4L, 1L), TodoFilter.TODO.apply(reversed).map { it.id })
    }

    @Test
    fun emptyList_returnsEmpty() {
        TodoFilter.entries.forEach { assertTrue(it.apply(emptyList()).isEmpty()) }
    }

    @Test
    fun countAll_countsEveryFilter() {
        val counts = TodoFilter.countAll(list)
        assertEquals(4, counts[TodoFilter.ALL])
        assertEquals(2, counts[TodoFilter.TODO])
        assertEquals(1, counts[TodoFilter.IN_PROGRESS])
        assertEquals(1, counts[TodoFilter.DONE])
    }
}
