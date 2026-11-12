package com.k0h5.todolist.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.k0h5.todolist.R
import com.k0h5.todolist.data.local.Todo
import com.k0h5.todolist.data.local.TodoStatus
import com.k0h5.todolist.databinding.ItemTodoBinding
import com.k0h5.todolist.vm.TodoRow

class TodoAdapter(
    private val onSelect: (Todo) -> Unit,
    private val onMove: (Todo) -> Unit,
    private val onClick: (Todo) -> Unit,
    private val onLongPress: (Todo) -> Unit
) : ListAdapter<TodoRow, TodoAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<TodoRow>() {
            override fun areItemsTheSame(old: TodoRow, new: TodoRow) = old.todo.id == new.todo.id
            override fun areContentsTheSame(old: TodoRow, new: TodoRow) = old == new
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTodoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val b: ItemTodoBinding) : RecyclerView.ViewHolder(b.root) {
        private var current: Todo? = null

        init {
            b.cbSelect.setOnClickListener { current?.let(onSelect) }
            b.btnMove.setOnClickListener { current?.let(onMove) }
            b.root.setOnClickListener { current?.let(onClick) }
            b.root.setOnLongClickListener {
                current?.let(onLongPress)
                true
            }
        }

        fun bind(row: TodoRow) {
            val item = row.todo
            current = item
            val ctx = b.root.context

            b.cbSelect.isChecked = row.selected
            b.tvItemTitle.text = if (item.status == TodoStatus.IN_PROGRESS) {
                ctx.getString(R.string.in_progress_prefix, item.title)
            } else {
                item.title
            }

            // 완료된 항목은 이동 버튼 없음
            when (item.status) {
                TodoStatus.TODO -> b.btnMove.setText(R.string.move_start)
                TodoStatus.IN_PROGRESS -> b.btnMove.setText(R.string.move_done)
                TodoStatus.DONE -> Unit
            }
            b.btnMove.visibility = if (item.status.next() == null) View.INVISIBLE else View.VISIBLE

            b.tvNote.text = item.note
            b.tvNote.visibility = if (item.note.isBlank()) View.GONE else View.VISIBLE
            b.tvDueDate.text = ctx.getString(R.string.due_label, item.dueDate)
            b.tvDueDate.visibility = if (item.dueDate.isBlank()) View.GONE else View.VISIBLE

            // F1.6 완료된 항목은 취소선 + 흐리게
            val done = item.status == TodoStatus.DONE
            b.tvItemTitle.paintFlags = if (done) {
                b.tvItemTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                b.tvItemTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            b.tvItemTitle.alpha = if (done) 0.5f else 1f
            b.tvNote.alpha = b.tvItemTitle.alpha
            b.tvDueDate.alpha = b.tvItemTitle.alpha
        }
    }
}
