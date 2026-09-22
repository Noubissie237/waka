package com.propentatech.waka.ui.format

import java.util.Calendar

private const val DAY_MS = 86_400_000L

private fun startOfDay(millis: Long): Calendar = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

/** Écart en jours calendaires entiers entre aujourd'hui et [deadlineAtMillis] (négatif si dépassée). */
fun daysUntil(deadlineAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): Long =
    (startOfDay(deadlineAtMillis).timeInMillis - startOfDay(nowMillis).timeInMillis) / DAY_MS

/** Décompose l'écart entre deux instants en mois calendaires pleins + jours restants. */
private fun monthsAndDaysBetween(fromMillis: Long, toMillis: Long): Pair<Int, Int> {
    val from = startOfDay(fromMillis)
    val to = startOfDay(toMillis)
    var months = 0
    val cursor = from.clone() as Calendar
    while (true) {
        val next = cursor.clone() as Calendar
        next.add(Calendar.MONTH, 1)
        if (next.after(to)) break
        cursor.timeInMillis = next.timeInMillis
        months++
    }
    val days = ((to.timeInMillis - cursor.timeInMillis) / DAY_MS).toInt()
    return months to days
}

private fun formatMonthsAndDays(fromMillis: Long, toMillis: Long): String = buildString {
    val (months, days) = monthsAndDaysBetween(fromMillis, toMillis)
    if (months > 0) append(if (months == 1) "1 mois" else "$months mois")
    if (days > 0 || months == 0) {
        if (isNotEmpty()) append(" ")
        append(if (days == 1) "1 jour" else "$days jours")
    }
}

/** Échéance d'un projet en clair : « Demain », « Dans 14 mois 14 jours », « En retard de 3 jours »… */
fun formatDaysRemaining(deadlineAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val totalDays = daysUntil(deadlineAtMillis, nowMillis)
    return when {
        totalDays == 0L -> "Aujourd'hui"
        totalDays == 1L -> "Demain"
        totalDays > 0 -> "Dans ${formatMonthsAndDays(nowMillis, deadlineAtMillis)}"
        totalDays == -1L -> "En retard de 1 jour"
        else -> "En retard de ${formatMonthsAndDays(deadlineAtMillis, nowMillis)}"
    }
}
