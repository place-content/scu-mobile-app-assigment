package com.k0h5.todolist.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY id DESC")
    fun observeAll(): Flow<List<Todo>>

    @Insert
    suspend fun insert(todo: Todo): Long

    @Update
    suspend fun update(todo: Todo)

    @Delete
    suspend fun delete(todo: Todo)

    @Query("DELETE FROM todos WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getById(id: Long): Todo?
}
