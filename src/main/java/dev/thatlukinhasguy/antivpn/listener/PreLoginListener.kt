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

    private val configPath = "./plugins/AntiVPN/config.yml"
    private val whitelistPath = "./plugins/AntiVPN/whitelist/list.json"
    private val blacklistPath = "./plugins/AntiVPN/blacklist/list.json"
    @Subscribe
    fun onPreLogin(event: PreLoginEvent) {
        val ip = event.connection.remoteAddress.address.hostAddress
        val whitelist = GsonStorage(File(whitelistPath))
        val config = YamlStorage(File(configPath))
        val blacklist = GsonStorage(File(blacklistPath))
        val kickMessage = config.getObjectValue("kickMessage").toString()

        if (whitelist.isValuePresentInList(
                "userWhitelist",
                event.username
            ) || whitelist.isValuePresentInList("ipWhitelist", ip)
        ) {
            return
        }

        if (blacklist.isValuePresentInList("badIps", ip)) {
            event.result = PreLoginEvent.PreLoginComponentResult.denied(Component.text(kickMessage.replace("&", "§")))
            handleWebhook(config, event, ip)
            return
        }

        val check = ApiUtil.check(ip)
        if (check) {
            event.result = PreLoginEvent.PreLoginComponentResult.denied(Component.text(kickMessage.replace("&", "§")))
            blacklist.appendValueToList("badIps", ip)
            handleWebhook(config, event, ip)
            return
        }
        whitelist.appendValueToList("ipWhitelist", ip)
        return
    }

    private fun handleWebhook(config: YamlStorage, event: PreLoginEvent, ip: String) {
        if (config.getObjectValue("webhook.enabled") == true) {
            val webhookUrl = config.getObjectValue("webhook.url").toString()
            if (webhookUrl.isNotBlank()) {
                val webhook = WebhookUtil(webhookUrl)
                webhook.addEmbed(
                    WebhookUtil.EmbedObject()
                        .setTitle("VPN/Proxy detected!")
                        .addField("**User:**", event.username, false)
                        .addField("**IP:**", ip, false)
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