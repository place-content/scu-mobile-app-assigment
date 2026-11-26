package com.k0h5.todolist.vm

import com.k0h5.todolist.data.local.TodoStatus
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

object TodoInput {
    enum class Error { TITLE_REQUIRED, DUE_INVALID, DUE_PAST }

    private val DUE_PATTERN = Regex("""\d{4}-\d{2}-\d{2}""")

    // earliestDue: 고를 수 있는 가장 이른 마감일 (YYYY-MM-DD). 같은 형식이라 문자열 비교로 날짜 비교 가능
    // 위에서부터 순서대로 검사해 처음 걸린 오류를 돌려줌. 마감일은 비어 있으면 통과(선택 항목)
    fun validate(title: String, dueDate: String, earliestDue: String? = null): Error? {
        val due = dueDate.trim()
        return when {
            title.isBlank() -> Error.TITLE_REQUIRED
            due.isEmpty() -> null
            !isValidDate(due) -> Error.DUE_INVALID
            earliestDue != null && due < earliestDue -> Error.DUE_PAST
            else -> null
        }
    }

    // 저장 버튼 활성화 기준: 필수 칸(제목)이 채워졌는지
    fun canSave(title: String): Boolean = title.isNotBlank()

    // 기본은 오늘부터. 편집 중인 할 일의 원래 마감일이 이미 지났다면 그 날짜까지는 허용
    fun earliestDue(today: String, originalDue: String): String =
        if (originalDue.isNotBlank() && originalDue < today) originalDue else today

    // 추가·편집 화면의 입력값 묶음 (저장하지 않고 나갈 때 변경 여부 비교용)
    data class Draft(
        val title: String = "",
        val note: String = "",
        val due: String = "",
        val status: TodoStatus = TodoStatus.TODO
    ) {
        fun trimmed() = copy(title = title.trim(), note = note.trim(), due = due.trim())
    }

    // 앞뒤 공백만 달라진 건 변경으로 보지 않음 (저장할 때도 trim 하므로)
    fun hasChanges(original: Draft, current: Draft): Boolean = original.trimmed() != current.trimmed()

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
