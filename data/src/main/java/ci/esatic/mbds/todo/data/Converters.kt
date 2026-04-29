package ci.esatic.mbds.todo.data

import androidx.room.TypeConverter

internal class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(separator = "\u001F")

    @TypeConverter
    fun toStringList(value: String): List<String> =
        value.takeIf { it.isNotBlank() }?.split("\u001F") ?: emptyList()

    @TypeConverter
    fun fromPriority(value: Priority): String = value.name

    @TypeConverter
    fun toPriority(value: String): Priority = runCatching { Priority.valueOf(value) }.getOrDefault(Priority.MEDIUM)
}

