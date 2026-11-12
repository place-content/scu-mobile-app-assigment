package com.k0h5.todolist.vm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodoInputTest {
    @Test
    fun blankTitle_isRejected() {
        assertEquals(TodoInput.Error.TITLE_REQUIRED, TodoInput.validate("   ", ""))
    }

    @Test
    fun titleOnly_isValid() {
        assertNull(TodoInput.validate("장보기", ""))
    }

    @Test
    fun validDueDate_isAccepted() {
        assertNull(TodoInput.validate("과제", "2026-12-01"))
        assertNull(TodoInput.validate("과제", "2028-02-29"))
    }

    @Test
    fun wrongFormatOrImpossibleDate_isRejected() {
        listOf("2026/12/01", "26-12-01", "2026-13-01", "2026-02-30", "abc").forEach {
            assertEquals(it, TodoInput.Error.DUE_INVALID, TodoInput.validate("과제", it))
        }
    }
}

class TodoInputPastDueTest {
    private val today = "2026-11-12"

    @Test
    fun pastDue_isRejected_whenEarliestGiven() {
        assertEquals(TodoInput.Error.DUE_PAST, TodoInput.validate("과제", "2026-11-11", earliestDue = today))
    }

    @Test
    fun todayOrLater_isAccepted() {
        assertNull(TodoInput.validate("과제", today, earliestDue = today))
        assertNull(TodoInput.validate("과제", "2027-01-01", earliestDue = today))
    }

    @Test
    fun blankDue_isAccepted_evenWithEarliest() {
        assertNull(TodoInput.validate("과제", "", earliestDue = today))
    }

    @Test
    fun earliestDue_isToday_forNewOrFutureDue() {
        assertEquals(today, TodoInput.earliestDue(today, ""))
        assertEquals(today, TodoInput.earliestDue(today, "2026-12-01"))
    }

    @Test
    fun earliestDue_allowsKeepingAlreadyPassedOriginal() {
        assertEquals("2026-10-01", TodoInput.earliestDue(today, "2026-10-01"))
        // 원래 마감일은 그대로 저장 가능, 그보다 더 과거는 불가
        assertNull(TodoInput.validate("과제", "2026-10-01", TodoInput.earliestDue(today, "2026-10-01")))
        assertEquals(
            TodoInput.Error.DUE_PAST,
            TodoInput.validate("과제", "2026-09-30", TodoInput.earliestDue(today, "2026-10-01"))
        )
    }
}
