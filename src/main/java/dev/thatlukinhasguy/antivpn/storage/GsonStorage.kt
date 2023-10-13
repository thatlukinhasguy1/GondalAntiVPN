package dev.thatlukinhasguy.antivpn.storage

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

class GsonStorage(private val jsonFilePath: File) {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private var data: MutableMap<String, Any> = loadJsonFromFile(jsonFilePath)

    private fun loadJsonFromFile(file: File): MutableMap<String, Any> {
        try {
            if (jsonFilePath.exists()) {
                FileReader(file).use { reader ->
                    val type = object : TypeToken<MutableMap<String, Any>>() {}.type
                    return gson.fromJson(reader, type) ?: mutableMapOf()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return mutableMapOf()
    }

    private fun saveJsonToFile() {
        try {
            FileWriter(jsonFilePath).use { writer ->
                gson.toJson(data, writer)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getObjectValue(key: String): Any? {
        return data[key]
    }

    fun isValuePresentInList(key: String, value: Any): Boolean {
        val list = data[key] as? List<*>
        return list?.contains(value) == true
    }

    fun removeValueFromList(key: String, value: Any) {
        val list = data[key] as? MutableList<*>
        list?.remove(value)
        saveJsonToFile()
    }

    fun appendValueToList(key: String, value: Any) {
        val list = (data[key] as? MutableList<Any>) ?: mutableListOf()
        list.add(value)
        data[key] = list
        saveJsonToFile()
    }
}