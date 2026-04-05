package com.example.projekat.data.local

import androidx.room.TypeConverter
import com.example.projekat.data.model.ChecklistItem
import com.example.projekat.data.model.SyncStatus
import com.example.projekat.data.model.TaskPriority
import com.example.projekat.data.model.TaskStatus
import org.json.JSONArray
import org.json.JSONObject

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
    fun fromTaskPriority(priority: TaskPriority): String {
        return priority.name
    }

    @TypeConverter
    fun toTaskPriority(value: String): TaskPriority {
        return TaskPriority.valueOf(value)
    }

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String {
        return status.name
    }

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus {
        return SyncStatus.valueOf(value)
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

    @TypeConverter
    fun fromChecklistItemList(items: List<ChecklistItem>): String {
        val jsonArray = JSONArray()
        items.forEach { item ->
            val jsonObject = JSONObject().apply {
                put("id", item.id)
                put("text", item.text)
                put("isChecked", item.isChecked)
                put("order", item.order)
            }
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toChecklistItemList(value: String): List<ChecklistItem> {
        if (value.isBlank()) return emptyList()
        val jsonArray = JSONArray(value)
        return (0 until jsonArray.length()).map { index ->
            val jsonObject = jsonArray.getJSONObject(index)
            ChecklistItem(
                id = jsonObject.optString("id", ""),
                text = jsonObject.optString("text", ""),
                isChecked = jsonObject.optBoolean("isChecked", false),
                order = jsonObject.optInt("order", 0)
            )
        }
    }
}
