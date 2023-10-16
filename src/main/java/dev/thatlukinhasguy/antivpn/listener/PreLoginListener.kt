package dev.thatlukinhasguy.antivpn.listener

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PreLoginEvent
import dev.thatlukinhasguy.antivpn.storage.GsonStorage
import dev.thatlukinhasguy.antivpn.storage.YamlStorage
import dev.thatlukinhasguy.antivpn.utils.ApiUtil
import dev.thatlukinhasguy.antivpn.utils.WebhookUtil
import net.kyori.adventure.text.Component
import java.awt.Color
import java.io.File

class PreLoginListener {

    private val configFilePath = "./plugins/AntiVPN/config.yml"
    private val whitelistFilePath = "./plugins/AntiVPN/whitelist/list.json"
    private val blacklistFilePath = "./plugins/AntiVPN/blacklist/list.json"

    @Subscribe
    fun onPreLogin(event: PreLoginEvent) {
        val ipAddress = event.connection.remoteAddress.address.hostAddress
        val whitelist = GsonStorage(File(whitelistFilePath))
        val config = YamlStorage(File(configFilePath))
        val blacklist = GsonStorage(File(blacklistFilePath))
        val kickMessage = config.getObjectValue("kickMessage").toString()

        if (whitelist.isValuePresentInList("userWhitelist", event.username) || whitelist.isValuePresentInList("ipWhitelist", ipAddress)) {
            return
        }

        if (blacklist.isValuePresentInList("badIps", ipAddress)) {
            event.result = PreLoginEvent.PreLoginComponentResult.denied(Component.text(kickMessage.replace("&", "§")))
            handleWebhook(config, event, ipAddress)
            return
        }

        val isVpnDetected = ApiUtil.check(ipAddress)
        if (isVpnDetected) {
            event.result = PreLoginEvent.PreLoginComponentResult.denied(Component.text(kickMessage.replace("&", "§")))
            blacklist.appendValueToList("badIps", ipAddress)
            handleWebhook(config, event, ipAddress)
            return
        }
        whitelist.appendValueToList("ipWhitelist", ipAddress)
        return
    }

    private fun handleWebhook(config: YamlStorage, event: PreLoginEvent, ipAddress: String) {
        if (config.getObjectValue("webhook.enabled") == true) {
            val webhookUrl = config.getObjectValue("webhook.url").toString()
            if (webhookUrl.isNotBlank()) {
                val webhook = WebhookUtil(webhookUrl)
                webhook.addEmbed(
                    WebhookUtil.EmbedObject()
                        .setTitle("VPN/Proxy detected!")
                        .addField("**User:**", event.username, false)
                        .addField("**IP:**", ipAddress, false)
                        .setFooter(
                            "GondalAntiVPN by ThatLukinhasGuy",
                            "https://static.wikia.nocookie.net/minecraft/images/8/8d/BarrierNew.png"
                        )
                        .setColor(Color.BLACK)
                )
                webhook.execute()
            }
        }
    }
}