package com.aesthetic.tracker.data

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate): String = value.toString()

    @TypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)

    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(LIST_SEPARATOR)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split(LIST_SEPARATOR)

    private companion object {
        const val LIST_SEPARATOR = "\u001F"
    }
}
