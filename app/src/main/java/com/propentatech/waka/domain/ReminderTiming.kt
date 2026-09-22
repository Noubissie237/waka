package com.propentatech.waka.domain

import com.propentatech.waka.model.RepeatType
import java.util.Calendar

/**
 * Aucune date à choisir : seulement une heure, et pour les rappels récurrents, un jour de la
 * semaine. Le premier déclenchement est calculé comme la prochaine occurrence correspondante.
 */
object ReminderTiming {

    /** [weekday] utilise les constantes [Calendar] (MONDAY..SUNDAY) ; ignoré si [repeatType] est NONE. */
    fun nextOccurrence(
        repeatType: RepeatType,
        hour: Int,
        minute: Int,
        weekday: Int?,
        now: Long = System.currentTimeMillis(),
    ): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (weekday != null) {
            while (calendar.get(Calendar.DAY_OF_WEEK) != weekday) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        if (calendar.timeInMillis <= now) {
            when (repeatType) {
                RepeatType.NONE -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                RepeatType.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                RepeatType.WEEKLY -> calendar.add(Calendar.DAY_OF_YEAR, 7)
                RepeatType.MONTHLY -> calendar.add(Calendar.DAY_OF_YEAR, 28)
            }
        }
        return calendar.timeInMillis
    }

    /** Prochaine occurrence après un déclenchement, pour les rappels récurrents. */
    fun advance(current: Long, repeatType: RepeatType): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = current }
        when (repeatType) {
            RepeatType.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            RepeatType.WEEKLY -> calendar.add(Calendar.DAY_OF_YEAR, 7)
            RepeatType.MONTHLY -> calendar.add(Calendar.DAY_OF_YEAR, 28)
            RepeatType.NONE -> Unit
        }
        return calendar.timeInMillis
    }
}
