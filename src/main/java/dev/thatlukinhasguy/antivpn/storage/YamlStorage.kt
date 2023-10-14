package dev.thatlukinhasguy.antivpn.storage

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

/**
 * YamlStorage provides utility functions for handling YAML data using SnakeYAML library.
 */
class YamlStorage(private val yamlFilePath: File) {

    private val dumperOptions = DumperOptions()
    private val yaml = Yaml(dumperOptions)
    private var data: MutableMap<String, Any> = loadYamlFromFile(yamlFilePath)

    private fun loadYamlFromFile(file: File): MutableMap<String, Any> {
        try {
            if (file.exists()) {
                FileReader(file).use { reader ->
                    return yaml.load(reader) ?: mutableMapOf()
                }
            }
        } catch (e: IOException) {
            println("Error reading from file: ${e.message}")
        }
        return mutableMapOf()
    }

    private fun saveYamlToFile() {
        try {
            FileWriter(yamlFilePath).use { writer ->
                yaml.dump(data, writer)
            }
        } catch (e: IOException) {
            println("Error writing to file: ${e.message}")
        }
    }

    fun getObjectValue(key: String): Any? {
        val keyList = key.split(".")
        var result: Any = data

        for (k in keyList) {
            if (result is Map<*, *>) {
                result = result[k] ?: return null
            } else {
                return null
            }
        }

        return result
    }

    fun saveData(key: String, value: Any) {
        data[key] = value
        saveYamlToFile()
    }
}
