package com.k0h5.todolist.data.local

enum class TodoStatus(val label: String) {
    TODO("할 일"),
    IN_PROGRESS("진행 중"),
    DONE("완료");

    // 이동 버튼용: 할 일 → 진행 중 → 완료. 완료 다음은 없음(null)
    fun next(): TodoStatus? = when (this) {
        TODO -> IN_PROGRESS
        IN_PROGRESS -> DONE
        DONE -> null
    }
}
