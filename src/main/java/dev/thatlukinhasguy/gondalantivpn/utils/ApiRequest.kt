package dev.thatlukinhasguy.gondalantivpn.utils

import org.json.JSONObject
import okhttp3.OkHttpClient
import okhttp3.Request

class ApiRequest {
    fun check(ip: String): Boolean {
        val client = OkHttpClient()
        val request = Request.Builder()
                .url("https://v2.api.iphub.info/guest/ip/$ip?c=Fae9gi8a")
                .build()

        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val json = response.body.string()
            val int = JSONObject(json)
            if (int.getInt("block") == 1) {
                return true
            }
        } else {
            return false
        }
        return false
    }
}