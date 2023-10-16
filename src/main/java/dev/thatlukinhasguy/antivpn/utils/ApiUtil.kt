package dev.thatlukinhasguy.antivpn.utils

import dev.thatlukinhasguy.antivpn.storage.YamlStorage
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

object ApiUtil {

    private val configYaml = YamlStorage(File("./plugins/AntiVPN/config.yml"))

    fun check(ip: String): Boolean {
        if (ip == "127.0.0.1") return false

        val proxyCheckUrl = getProxyCheckUrl(ip)
        val vpnApiUrl = getVpnApiUrl(ip)

        fun OkHttpClient.getResponse(url: String): String? {
            return newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (response.isSuccessful) response.body.string() else null
            }
        }

        fun JSONObject.getIntOrZero(key: String): Int {
            return if (this.has(key)) getInt(key) else 0
        }

        fun JSONObject.getBooleanOrFalse(key: String): Boolean {
            return if (this.has(key)) getBoolean(key) else false
        }

        return when {
            configYaml.getObjectValue("api.ipApi.enabled") as Boolean -> {
                val response = OkHttpClient().getResponse("https://v2.api.iphub.info/guest/ip/$ip?c=Fae9gi8a")
                val json = response?.let { JSONObject(it) }
                json?.getIntOrZero("block") != 0
            }
            configYaml.getObjectValue("api.ipHub.enabled") as Boolean -> {
                val response = OkHttpClient().getResponse("http://ip-api.com/json/$ip?fields=16973824")
                val json = response?.let { JSONObject(it) }
                json?.let {
                    it.getBooleanOrFalse("mobile") || it.getBooleanOrFalse("proxy") || it.getBooleanOrFalse("hosting")
                } ?: false
            }
            configYaml.getObjectValue("api.proxyCheck.enabled") as Boolean -> {
                val response = OkHttpClient().getResponse(proxyCheckUrl)
                val json = response?.let { JSONObject(it).getJSONObject(ip) }
                json?.getString("proxy") == "yes"
            }
            configYaml.getObjectValue("api.vpnApi.enabled") as Boolean -> {
                val response = OkHttpClient().getResponse(vpnApiUrl)
                val json = response?.let { JSONObject(it) }
                json?.getBoolean("is_proxy") == true
            }
            else -> false
        }
    }

    private fun getProxyCheckUrl(ip: String): String {
        return try {
            "https://proxycheck.io/v2/$ip?key=${configYaml.getObjectValue("api.proxyCheck.apiKey")}&vpn=1&asn=1"
        } catch (e: NullPointerException) {
            "https://proxycheck.io/v2/$ip?vpn=1&asn=1"
        }
    }

    private fun getVpnApiUrl(ip: String): String {
        return try {
            "http://vpn-api.xyz/detector?key=${configYaml.getObjectValue("api.vpnApi.apiKey")}&host=$ip"
        } catch (e: NullPointerException) {
            throw e
        }
    }
}
