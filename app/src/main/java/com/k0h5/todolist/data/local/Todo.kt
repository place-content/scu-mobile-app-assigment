package com.k0h5.todolist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val dueDate: String = "",
    val status: TodoStatus = TodoStatus.TODO,
    val createdAt: Long = System.currentTimeMillis()
)
