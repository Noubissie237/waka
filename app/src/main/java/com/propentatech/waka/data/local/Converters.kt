package com.propentatech.waka.data.local

import androidx.room.TypeConverter
import com.propentatech.waka.model.Currency
import com.propentatech.waka.model.RepeatType

class Converters {
    @TypeConverter
    fun fromCurrency(value: Currency?): String? = value?.name

    @TypeConverter
    fun toCurrency(value: String?): Currency? = value?.let { Currency.valueOf(it) }

    @TypeConverter
    fun fromRepeatType(value: RepeatType): String = value.name

    @TypeConverter
    fun toRepeatType(value: String): RepeatType = RepeatType.valueOf(value)
}
