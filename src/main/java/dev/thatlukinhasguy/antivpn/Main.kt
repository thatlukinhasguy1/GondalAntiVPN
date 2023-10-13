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
import dev.thatlukinhasguy.antivpn.commands.CommandImpl
import dev.thatlukinhasguy.antivpn.storage.GsonStorage
import dev.thatlukinhasguy.antivpn.utils.ApiRequest
import dev.thatlukinhasguy.antivpn.utils.DiscordWebhook
import net.kyori.adventure.text.Component
import org.slf4j.Logger
import java.awt.Color
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException


@Plugin(id = "antivpn", name = "GondalAntiVPN", version = "1.2")
class Main @Inject constructor(
        private val logger: Logger,
        private val server: ProxyServer
) {
    private val configPath = "./plugins/AntiVPN/config.json"
    private val whitelistPath = "./plugins/AntiVPN/whitelist/list.json"

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
                val kickMessage = config.getObjectValue("kickMessage").toString()

                if (whitelist.isValuePresentInList("userWhitelist", event.username) || whitelist.isValuePresentInList("ipWhitelist", ip)) {
                    return@register
                }

                if (ApiRequest.check(ip)) {
                    event.result = PreLoginEvent.PreLoginComponentResult.denied(Component.text(kickMessage.replace("&", "§")))
                    if (config.getObjectValue("discordWebhookEnabled") == true) {
                        val webhookUrl = config.getObjectValue("discordWebhookUrl").toString()
                        if (webhookUrl.isBlank()) {
                            return@register
                        }
                        val webhook = DiscordWebhook(webhookUrl)
                        webhook.addEmbed(DiscordWebhook.EmbedObject()
                                .setTitle("VPN/Proxy detectado!")
                                .addField("**Usuário:**", event.username, false)
                                .addField("**IP:**", ip, false)
                                .setFooter("AntiVPN por ThatLukinhasGuy", "https://static.wikia.nocookie.net/minecraft/images/8/8d/BarrierNew.png")
                                .setColor(Color.BLACK)
                        )
                        webhook.execute()
                    }
                }
            }
            logger.info("All done!")
        } catch (e: IOException) {
            logger.error(e.toString())
        }
    }

    private fun setupConfig() {
        createIfNotExists(configPath, ConfigData(kickMessage = "&cDesative sua VPN/Proxy!", discordWebhookEnabled = false, discordWebhookUrl = ""))
        createIfNotExists(whitelistPath, WhitelistData(listOf(), listOf()))
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

data class ConfigData( val kickMessage: String, val discordWebhookEnabled: Boolean, val discordWebhookUrl: String)
data class WhitelistData(val userWhitelist: List<String>, val ipWhitelist: List<String>)