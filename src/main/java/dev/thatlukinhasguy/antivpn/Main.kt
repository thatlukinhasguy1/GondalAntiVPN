@file:Suppress("NAME_SHADOWING")

package dev.thatlukinhasguy.antivpn

import com.google.gson.GsonBuilder
import com.google.inject.Inject
import com.velocitypowered.api.command.CommandManager
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PreLoginEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import dev.thatlukinhasguy.antivpn.commands.*
import dev.thatlukinhasguy.antivpn.storage.*
import dev.thatlukinhasguy.antivpn.utils.*
import net.kyori.adventure.text.Component
import org.slf4j.Logger
import java.awt.Color
import java.io.*

@Plugin(id = "antivpn", name = "GondalAntiVPN", version = "1.5")
class Main @Inject constructor(
    private val logger: Logger,
    private val server: ProxyServer
) {
    private val configPath = "./plugins/AntiVPN/config.json"
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
            server.eventManager.register(this, PreLoginEvent::class.java) { event ->
                val ip = event.connection.remoteAddress.address.hostAddress
                val whitelist = GsonStorage(File(whitelistPath))
                val config = GsonStorage(File(configPath))
                val blacklist = GsonStorage(File(blacklistPath))
                val kickMessage = config.getObjectValue("kickMessage").toString()

                if (whitelist.isValuePresentInList("userWhitelist", event.username) || whitelist.isValuePresentInList("ipWhitelist", ip)) {
                    return@register
                }

                if (blacklist.isValuePresentInList("badIps", ip)) {
                    handleBlacklist(event, config, kickMessage, ip)
                    return@register
                }

                handleCheck(event, ip, config, blacklist, kickMessage)
            }
            logger.info("All done!")
        } catch (e: IOException) {
            logger.error(e.toString())
        }
    }

    private fun handleCheck(event: PreLoginEvent, ip: String, config: GsonStorage, blacklist: GsonStorage, kickMessage: String) {
        val check = ApiUtil.check(ip)
        if (check) {
            event.result = PreLoginEvent.PreLoginComponentResult.denied(Component.text(kickMessage.replace("&", "§")))
            blacklist.appendValueToList("badIps", ip)
            handleWebhook(config, event, ip)
        }
    }

    private fun handleWebhook(config: GsonStorage, event: PreLoginEvent, ip: String) {
        if (config.getObjectValue("discordWebhook.enabled") == true) {
            val webhookUrl = config.getObjectValue("discordWebhook.url").toString()
            if (webhookUrl.isNotBlank()) {
                val webhook = WebhookUtil(webhookUrl)
                webhook.addEmbed(
                    WebhookUtil.EmbedObject()
                        .setTitle("VPN/Proxy detected!")
                        .addField("**User:**", event.username, false)
                        .addField("**IP:**", ip, false)
                        .setFooter("AntiVPN by ThatLukinhasGuy", "https://static.wikia.nocookie.net/minecraft/images/8/8d/BarrierNew.png")
                        .setColor(Color.BLACK)
                )
                webhook.execute()
            }
        }
    }

    private fun handleBlacklist(event: PreLoginEvent, config: GsonStorage, kickMessage: String, ip: String) {
        event.result = PreLoginEvent.PreLoginComponentResult.denied(Component.text(kickMessage.replace("&", "§")))
        handleWebhook(config, event, ip)
    }

    private fun setupConfig() {
        createIfNotExists(configPath, ConfigData())
        createIfNotExists(blacklistPath, BlacklistData())
        createIfNotExists(whitelistPath, WhitelistData())
    }

    private fun createIfNotExists(filePath: String, data: Any) {
        val file = File(filePath)
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
            saveJsonToFile(file, data)
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

data class ConfigData(val kickMessage: String = "&cTurn your VPN/Proxy off!",
                      val api: Map<String, Map<String, Any>> = mapOf("ipHub" to mapOf("enabled" to true), "ipApi" to mapOf("enabled" to false), "proxyCheck" to mapOf("enabled" to false, "apiKey" to "")),
                      val discordWebhook: Map<String, Any> = mapOf("enabled" to false, "url" to ""))
data class WhitelistData(val userWhitelist: List<String> = listOf(), val ipWhitelist: List<String> = listOf())
data class BlacklistData(val badIps: List<String> = listOf())
