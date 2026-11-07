package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.Subtask
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromIntList(list: List<Int>): String {
        return list.joinToString(",")
    }

    @TypeConverter
    fun toIntList(data: String): List<Int> {
        if (data.isBlank()) return emptyList()
        return data.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    @TypeConverter
    fun fromSubtaskList(subtasks: List<Subtask>): String {
        val array = JSONArray()
        for (item in subtasks) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("isCompleted", item.isCompleted)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toSubtaskList(data: String): List<Subtask> {
        if (data.isBlank()) return emptyList()
        return try {
            val array = JSONArray(data)
            val list = mutableListOf<Subtask>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Subtask(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        isCompleted = obj.optBoolean("isCompleted", false)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
