package com.k0h5.todolist.data

import com.k0h5.todolist.data.local.Todo
import com.k0h5.todolist.data.local.TodoDatabase
import com.k0h5.todolist.data.local.TodoStatus
import kotlinx.coroutines.flow.Flow

class TodoRepository(db: TodoDatabase) {
    private val dao = db.todoDao()

    val all: Flow<List<Todo>> = dao.observeAll()

    suspend fun getById(id: Long): Todo? = dao.getById(id)

    // 새 할 일 id를 돌려줌 (제목이 비어 저장하지 않았으면 null)
    suspend fun add(title: String, note: String, dueDate: String): Long? {
        val t = title.trim()
        if (t.isBlank()) return null
        return dao.insert(Todo(title = t, note = note.trim(), dueDate = dueDate.trim()))
    }

    suspend fun update(id: Long, title: String, note: String, dueDate: String, status: TodoStatus) {
        val t = title.trim()
        if (t.isBlank()) return
        val item = dao.getById(id) ?: return
        dao.update(item.copy(title = t, note = note.trim(), dueDate = dueDate.trim(), status = status))
    }

    suspend fun setStatus(id: Long, status: TodoStatus) {
        val item = dao.getById(id) ?: return
        if (item.status != status) dao.update(item.copy(status = status))
    }

    suspend fun delete(id: Long) {
        val item = dao.getById(id) ?: return
        dao.delete(item)
    }

    suspend fun deleteAll(ids: Collection<Long>) {
        if (ids.isNotEmpty()) dao.deleteByIds(ids.toList())
    }
}
