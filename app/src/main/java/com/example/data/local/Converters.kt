package com.example.data.local

import androidx.room.TypeConverter
import org.json.JSONArray

class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        val array = JSONArray()
        value?.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return try {
            val array = JSONArray(value)
            val list = ArrayList<String>(array.length())
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
