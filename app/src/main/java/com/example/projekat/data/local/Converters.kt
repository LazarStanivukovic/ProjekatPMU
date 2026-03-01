package com.example.projekat.data.local

import androidx.room.TypeConverter
import com.example.projekat.data.model.TaskStatus
import org.json.JSONArray

class Converters {
    @TypeConverter
    fun fromTaskStatus(status: TaskStatus): String {
        return status.name
    }

    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus {
        return TaskStatus.valueOf(value)
    }

    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return JSONArray(list).toString()
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        val jsonArray = JSONArray(value)
        return (0 until jsonArray.length()).map { jsonArray.getString(it) }
    }
}
