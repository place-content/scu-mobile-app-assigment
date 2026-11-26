package com.k0h5.todolist.vm

import com.k0h5.todolist.data.local.Todo
import com.k0h5.todolist.data.local.TodoStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodoRowTest {
    private val today = "2026-11-26"
    private val list = listOf(Todo(id = 1, title = "a"), Todo(id = 2, title = "b"), Todo(id = 3, title = "c"))

    @Test
    fun marksOnlySelectedIds() {
        val rows = TodoRow.of(list, setOf(1L, 3L), today)
        assertEquals(listOf(true, false, true), rows.map { it.selected })
    }

    @Test
    fun noSelection_allUnselected() {
        assertEquals(listOf(false, false, false), TodoRow.of(list, emptySet(), today).map { it.selected })
    }

    @Test
    fun selectedIdNotInList_isIgnored() {
        val rows = TodoRow.of(list, setOf(99L), today)
        assertEquals(3, rows.size)
        assertEquals(0, rows.count { it.selected })
    }

    @Test
    fun marksOverdue_byToday() {
        val todos = listOf(
            Todo(id = 1, title = "지남", dueDate = "2026-11-25"),
            Todo(id = 2, title = "오늘", dueDate = today),
            Todo(id = 3, title = "완료", dueDate = "2026-11-01", status = TodoStatus.DONE)
        )
        assertEquals(listOf(true, false, false), TodoRow.of(todos, emptySet(), today).map { it.overdue })
    }

    @Test
    fun dateChange_makesRowDifferent() {
        // 자정이 지나면 같은 할 일이라도 줄이 달라져야 DiffUtil이 다시 그림
        val todo = listOf(Todo(id = 1, title = "a", dueDate = "2026-11-26"))
        assertNotEquals(TodoRow.of(todo, emptySet(), "2026-11-26"), TodoRow.of(todo, emptySet(), "2026-11-27"))
    }

    @Test
    fun positionOf_findsIdOrNull() {
        val rows = TodoRow.of(list, emptySet(), today)
        assertEquals(0, TodoRow.positionOf(rows, 1L))
        assertEquals(2, TodoRow.positionOf(rows, 3L))
        assertNull(TodoRow.positionOf(rows, 99L))
        assertNull(TodoRow.positionOf(emptyList(), 1L))
    }
}

class TodoSelectionTest {
    @Test
    fun toggle_addsOrRemoves() {
        assertEquals(setOf(1L, 2L), TodoSelection.toggle(setOf(1L), 2L))
        assertEquals(emptySet<Long>(), TodoSelection.toggle(setOf(1L), 1L))
    }

    @Test
    fun onTap_notSelecting_returnsNull() {
        // 선택 중이 아니면 null → 화면은 편집을 엶
        assertNull(TodoSelection.onTap(emptySet(), 1L))
    }

    @Test
    fun onTap_selecting_togglesTappedItem() {
        assertEquals(setOf(1L, 2L), TodoSelection.onTap(setOf(1L), 2L))
        // 마지막 하나를 탭하면 선택이 0개가 됨 → 다음 탭부터는 다시 편집
        assertEquals(emptySet<Long>(), TodoSelection.onTap(setOf(1L), 1L))
    }
}
