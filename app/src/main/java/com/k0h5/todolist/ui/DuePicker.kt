package com.k0h5.todolist.ui

import android.app.DatePickerDialog
import android.content.DialogInterface
import android.widget.EditText
import com.k0h5.todolist.R
import java.util.Calendar
import java.util.Locale

// 마감일 칸을 누르면 달력(DatePickerDialog)을 띄우고, 고른 날짜를 YYYY-MM-DD로 채움
// earliest: 고를 수 있는 가장 이른 날짜 (그 이전 날짜는 달력에서 선택 불가)
fun EditText.setupDuePicker(earliest: () -> String) {
    setOnClickListener {
        val min = earliest()
        val start = text.toString().takeIf { it.isNotBlank() && it >= min } ?: min
        val (y, m, d) = parseDue(start) ?: parseDue(todayDue())!!

        val dialog = DatePickerDialog(
            context,
            { _, year, month, day -> setText(formatDue(year, month + 1, day)) },
            y,
            m - 1,
            d
        )
        parseDue(min)?.let { (minY, minM, minD) ->
            dialog.datePicker.minDate = Calendar.getInstance().apply {
                set(minY, minM - 1, minD, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, context.getString(R.string.btn_clear_date)) { _, _ ->
            setText("")
        }
        dialog.show()
    }
}

fun todayDue(): String {
    val cal = Calendar.getInstance()
    return formatDue(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
}

fun formatDue(year: Int, month: Int, day: Int): String = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

// "2026-12-01" → (2026, 12, 1), 형식이 다르면 null
fun parseDue(s: String): Triple<Int, Int, Int>? {
    val parts = s.trim().split("-")
    if (parts.size != 3) return null
    val nums = parts.map { it.toIntOrNull() ?: return null }
    return Triple(nums[0], nums[1], nums[2])
}
