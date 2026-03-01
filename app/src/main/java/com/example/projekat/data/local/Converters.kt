package com.example.projekat.data.local

import androidx.room.TypeConverter
import com.example.projekat.data.model.TaskStatus

class Converters {
    @TypeConverter
    fun fromTaskStatus(status: TaskStatus): String {
        return status.name
    }

    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus {
        return TaskStatus.valueOf(value)
    }
}
