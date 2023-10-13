package dev.thatlukinhasguy.antivpn.utils

import dev.thatlukinhasguy.antivpn.storage.GsonStorage
import org.json.JSONObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

object ApiRequest {
    fun check(ip: String): Boolean {
        val ipApiCheck = GsonStorage(File("./plugins/AntiVPN/config.json")).getObjectValue("api.ipApi")
        val ipHubCheck = GsonStorage(File("./plugins/AntiVPN/config.json")).getObjectValue("api.ipHub")

        if (ipApiCheck as Boolean && !(ipHubCheck as Boolean)) {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://v2.api.iphub.info/guest/ip/$ip?c=Fae9gi8a")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body.string()
                val value = JSONObject(json)
                return (value.getInt("block") != 0)
            }
            return false
        }
        if (ipHubCheck as Boolean && !ipApiCheck) {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("http://ip-api.com/json/$ip?fields=16973824")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body.string()
                val value = JSONObject(json)
                if (value.getBoolean("mobile") || value.getBoolean("proxy") || value.getBoolean("hosting"))
                    return true
            }
            return false
        }
        return false
    }
}