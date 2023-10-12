@file:Suppress("NAME_SHADOWING")

package dev.thatlukinhasguy.gondalantivpn

import com.google.gson.GsonBuilder
import com.google.inject.Inject
import com.velocitypowered.api.command.CommandManager
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import dev.thatlukinhasguy.gondalantivpn.commands.CommandImpl
import dev.thatlukinhasguy.gondalantivpn.storage.GsonStorage
import dev.thatlukinhasguy.gondalantivpn.utils.ApiRequest
import dev.thatlukinhasguy.gondalantivpn.utils.DiscordWebhook
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.slf4j.Logger
import java.io.File
import java.io.FileWriter
import java.io.IOException


@Plugin(id = "gondalantivpn", name = "GondalAntiVPN", version = BuildConstants.VERSION)
class Main @Inject constructor(
        private val logger: Logger,
        private val server: ProxyServer
) {

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent?) {
        logger.info("Creating the config files if not present...")
        setupConfig()
        logger.info("Registering the commands...")
        val commandManager: CommandManager = server.commandManager
        val commandMeta = commandManager.metaBuilder("gondalAntiVPN")
                .plugin(server)
                .build()
        commandManager.register(commandMeta, CommandImpl())
        logger.info("Registering the events...")
        server.eventManager.register(this, LoginEvent::class.java) { event ->
            val ip = event.player.remoteAddress.address.hostAddress

            val request = ApiRequest().check(ip)

            if (GsonStorage(File("./plugins/GondalAntiVPN/whitelist/list.json")).isValuePresentInList("whitelist", event.player.username)) return@register

            if (request) {
                val kickMessage = GsonStorage(File("./plugins/GondalAntiVPN/config.json")).getObjectValue("kickMessage").toString()
                event.player.disconnect(Component.text(kickMessage, NamedTextColor.RED))
                if (GsonStorage(File("./plugins/GondalAntiVPN/config.json")).getObjectValue("discordWebhookEnabled") == true) {
                    val webhookUrl = GsonStorage(File("./plugins/GondalAntiVPN/config.json")).getObjectValue("discordWebhookUrl").toString()
                    if (webhookUrl.isBlank()) {
                        return@register
                    }
                    val webhook = DiscordWebhook(webhookUrl)
                    webhook.addEmbed(DiscordWebhook.EmbedObject()
                            .setTitle("VPN/Proxy detected!")
                            .addField("**USER:**", event.player.username, true)
                            .addField("**IP:**", ip, false)
                            .setFooter("GondalAntiVPN by ThatLukinhasGuy", "https://static.wikia.nocookie.net/minecraft/images/8/8d/BarrierNew.png")
                    )
                    webhook.execute()
                }
            }
        }
        logger.info("All done!")
    }

    private fun setupConfig() {
        val configFile = File("./plugins/GondalAntiVPN/config.json")
        val whitelistFile = File("./plugins/GondalAntiVPN/whitelist/list.json")

        if (!configFile.exists()) {
            configFile.parentFile.mkdirs()
            configFile.createNewFile()
            val configData = ConfigData(kickMessage = "Turn off your VPN/Proxy!", discordWebhookEnabled = false, discordWebhookUrl = "")
            saveJsonToFile(configFile, configData)
        }

        if (!whitelistFile.exists()) {
            whitelistFile.parentFile.mkdirs()
            whitelistFile.createNewFile()
            val whitelistData = WhitelistData(listOf())
            saveJsonToFile(whitelistFile, whitelistData)
        }
    }

    private fun saveJsonToFile(file: File, data: Any) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        try {
            FileWriter(file).use { writer ->
                gson.toJson(data, writer)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

data class ConfigData(val kickMessage: String, val discordWebhookEnabled: Boolean, val discordWebhookUrl: String)
data class WhitelistData(val whitelist: List<String>)