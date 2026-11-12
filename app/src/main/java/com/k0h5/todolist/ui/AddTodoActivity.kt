package com.k0h5.todolist.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.k0h5.todolist.databinding.ActivityAddTodoBinding
import com.k0h5.todolist.vm.TodoInput

class AddTodoActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_NOTE = "extra_note"
        const val EXTRA_DUE = "extra_due"
    }

    private lateinit var binding: ActivityAddTodoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddTodoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.addTodoView.applySystemBarPadding()
        binding.etDue.setupDuePicker { todayDue() }

        binding.btnSave.setOnClickListener { save() }
        binding.toolbar.setNavigationOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private fun save() {
        val title = binding.etTitle.text.toString().trim()
        val note = binding.etNote.text.toString().trim()
        val due = binding.etDue.text.toString().trim()

        val error = TodoInput.validate(title, due, earliestDue = todayDue())
        if (error != null) {
            Toast.makeText(this, getString(error.messageRes()), Toast.LENGTH_SHORT).show()
            return
        }

        val data = Intent()
            .putExtra(EXTRA_TITLE, title)
            .putExtra(EXTRA_NOTE, note)
            .putExtra(EXTRA_DUE, due)
        setResult(Activity.RESULT_OK, data)
        finish()
    }
}
