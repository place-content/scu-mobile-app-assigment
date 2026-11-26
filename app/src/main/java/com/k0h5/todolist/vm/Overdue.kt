package com.k0h5.todolist.vm

import com.k0h5.todolist.data.local.Todo
import com.k0h5.todolist.data.local.TodoStatus

// 마감일이 오늘보다 이전인데 아직 완료하지 않은 할 일. 오늘이 마감인 건 지난 게 아님
// today: YYYY-MM-DD (같은 형식이라 문자열 비교로 날짜 비교 가능)
fun Todo.isOverdue(today: String): Boolean = dueDate.isNotBlank() && dueDate < today && status != TodoStatus.DONE
