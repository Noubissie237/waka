package com.propentatech.waka.ui.components

import android.app.DatePickerDialog
import android.content.Context
import java.util.Calendar

/** Sélecteur de date natif (jour, sans heure), utilisé pour l'échéance d'un projet. */
fun showDatePicker(context: Context, initialMillis: Long, onPicked: (Long) -> Unit) {
    val calendar = Calendar.getInstance().apply { timeInMillis = initialMillis }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth, 0, 0, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            onPicked(calendar.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH),
    ).show()
}
