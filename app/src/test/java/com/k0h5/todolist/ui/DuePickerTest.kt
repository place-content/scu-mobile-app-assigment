package com.k0h5.todolist.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DuePickerTest {
    @Test
    fun formatDue_padsMonthAndDay() {
        assertEquals("2026-03-05", formatDue(2026, 3, 5))
        assertEquals("2026-12-31", formatDue(2026, 12, 31))
    }

    @Test
    fun parseDue_readsValidDate() {
        assertEquals(Triple(2026, 12, 1), parseDue("2026-12-01"))
    }

    @Test
    fun parseDue_returnsNullForBlankOrWrongFormat() {
        assertNull(parseDue(""))
        assertNull(parseDue("2026/12/01"))
        assertNull(parseDue("2026-ab-01"))
    }

    @Test
    fun formatThenParse_roundTrips() {
        assertEquals(Triple(2027, 1, 9), parseDue(formatDue(2027, 1, 9)))
    }
}
