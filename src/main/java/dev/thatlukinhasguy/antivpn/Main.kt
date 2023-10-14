package dev.thatlukinhasguy.antivpn

import com.google.gson.GsonBuilder
import com.google.inject.Inject
import com.velocitypowered.api.command.CommandManager
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import dev.thatlukinhasguy.antivpn.commands.CommandImpl
import dev.thatlukinhasguy.antivpn.listener.PreLoginListener
import org.slf4j.Logger
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.*

@Plugin(id = "antivpn", name = "GondalAntiVPN", version = "1.6")
class Main @Inject constructor(
    private val logger: Logger,
    private val server: ProxyServer
) {
    private val configPath = "./plugins/AntiVPN/config.yml"
    private val whitelistPath = "./plugins/AntiVPN/whitelist/list.json"
    private val blacklistPath = "./plugins/AntiVPN/blacklist/list.json"

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent?) {
        try {
            logger.info("Creating the config files if they are not present...")
            setupConfig()
            logger.info("Registering the commands...")
            val commandManager: CommandManager = server.commandManager
            val commandMeta = commandManager.metaBuilder("antivpn").plugin(server).build()
            commandManager.register(commandMeta, CommandImpl())
            logger.info("Registering the events...")
            server.eventManager.register(this, PreLoginListener())
            logger.info("All done!")
        } catch (e: IOException) {
            logger.error(e.toString())
        }
    }

    private fun setupConfig() {
        createIfNotExists(configPath, getConfigData(), false)
        createIfNotExists(blacklistPath, getBlacklistData(), true)
        createIfNotExists(whitelistPath, getWhitelistData(), true)
    }

    private fun createIfNotExists(filePath: String, data: Map<String, Any>, json: Boolean) {
        val file = File(filePath)
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
            if (json) {
                saveJsonToFile(File(filePath), data)
                return
            }
            saveYamlToFile(file, data)
            return
        }
    }

    private fun getConfigData(): Map<String, Any> {
        return mapOf(
            "kickMessage" to "&cTurn your VPN/Proxy off!",
            "api" to mapOf(
                "ipHub" to mapOf("enabled" to true),
                "ipApi" to mapOf("enabled" to false),
                "proxyCheck" to mapOf("enabled" to false, "apiKey" to ""),
                "vpnApi" to mapOf("enabled" to false, "apiKey" to "")
            ),
            "webhook" to mapOf("enabled" to false, "url" to "")
        )
    }

    private fun getBlacklistData(): Map<String, Any> {
        return mapOf("badIps" to listOf<String>())
    }

    private fun getWhitelistData(): Map<String, Any> {
        return mapOf("userWhitelist" to listOf(), "ipWhitelist" to listOf<String>())
    }

    private fun saveYamlToFile(file: File, data: Map<String, Any>) {
        val dumperOptions = DumperOptions()
        dumperOptions.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        val yaml = Yaml(dumperOptions)
        try {
            FileWriter(file).use { writer ->
                if (data.containsKey("kickMessage") && data.containsKey("api") && data.containsKey("webhook")) {
                    val configData = mapOf(
                        "kickMessage" to data["kickMessage"],
                        "api" to data["api"],
                        "webhook" to data["webhook"]
                    )
                    yaml.dump(configData, writer)
                } else {
                    yaml.dump(data, writer)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    private fun saveJsonToFile(file: File, data: Any) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        try {
            BufferedWriter(FileWriter(file)).use { writer ->
                writer.write(gson.toJson(data).replace("\\u0026", "&"))
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}