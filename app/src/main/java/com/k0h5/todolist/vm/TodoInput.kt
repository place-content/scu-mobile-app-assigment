package com.k0h5.todolist.vm

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

object TodoInput {
    enum class Error { TITLE_REQUIRED, DUE_INVALID, DUE_PAST }

    private val DUE_PATTERN = Regex("""\d{4}-\d{2}-\d{2}""")

    // earliestDue: 고를 수 있는 가장 이른 마감일 (YYYY-MM-DD). 같은 형식이라 문자열 비교로 날짜 비교 가능
    fun validate(title: String, dueDate: String, earliestDue: String? = null): Error? {
        if (title.isBlank()) return Error.TITLE_REQUIRED
        val due = dueDate.trim()
        if (due.isEmpty()) return null
        if (!isValidDate(due)) return Error.DUE_INVALID
        if (earliestDue != null && due < earliestDue) return Error.DUE_PAST
        return null
    }

    // 기본은 오늘부터. 편집 중인 할 일의 원래 마감일이 이미 지났다면 그 날짜까지는 허용
    fun earliestDue(today: String, originalDue: String): String =
        if (originalDue.isNotBlank() && originalDue < today) originalDue else today

    private fun isValidDate(s: String): Boolean {
        if (!DUE_PATTERN.matches(s)) return false
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        return try {
            fmt.parse(s)
            true
        } catch (e: ParseException) {
            false
        }
    }
}
