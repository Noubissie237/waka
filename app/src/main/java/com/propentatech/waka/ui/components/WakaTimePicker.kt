package com.propentatech.waka.ui.components

import android.app.TimePickerDialog
import android.content.Context

/** Sélecteur natif d'heure seule, les rappels n'ont pas de date à choisir. */
fun showTimePicker(context: Context, initialHour: Int, initialMinute: Int, onPicked: (hour: Int, minute: Int) -> Unit) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onPicked(hour, minute) },
        initialHour,
        initialMinute,
        true,
    ).show()
}
