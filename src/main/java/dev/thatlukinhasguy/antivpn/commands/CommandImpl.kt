package dev.thatlukinhasguy.antivpn.commands

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.command.SimpleCommand.Invocation
import dev.thatlukinhasguy.antivpn.storage.GsonStorage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.io.File
import java.io.IOException
import java.util.concurrent.CompletableFuture

class CommandImpl: SimpleCommand {
    override fun execute(invocation: Invocation?) {
        val source = invocation?.source()
        val args = invocation?.arguments()

        if (args?.size != 4) {
            source?.sendMessage(invalidUsageMessage())
            return
        }

        when (args[0]) {
            "whitelist" -> when (args[1]) {
                "ip" -> when (args[2]) {
                    "add" -> handleWhitelist(source!!, args, ip = true, remove = false)
                    "remove" -> handleWhitelist(source!!, args, ip = true, remove = true)
                }
                "user" -> when (args[2]) {
                    "add" -> handleWhitelist(source!!, args, ip = false, remove = false)
                    "remove" -> handleWhitelist(source!!, args, ip = false, remove = true)
                }
                else -> source?.sendMessage(invalidUsageMessage())
            }
            else -> source?.sendMessage(invalidUsageMessage())
        }
    }

    private fun invalidUsageMessage(): Component {
        return createMessage("Uso correto: §a/antivpn whitelist <user/ip> <add/remove> <str>")
    }

    private fun handleWhitelist(source: CommandSource, args: Array<String>, ip: Boolean, remove: Boolean) {
        val string = args[3]

        if (string.isEmpty()) {
            source.sendMessage(createMessage("Uso correto: §a/antivpn whitelist <user/ip> <add/remove> <str>"))
            return
        }

        val storage = GsonStorage(File("plugins/AntiVPN/whitelist/list.json"))

        if (ip && storage.isValuePresentInList("ipWhitelist", string ) && !remove ||
                !ip && storage.isValuePresentInList("userWhitelist", string) && !remove
        ) {
            source.sendMessage(createMessage("O ${if (ip) "IP" else "usuário"} §a$string§f já está na whitelist."))
            return
        }

        if (ip && !storage.isValuePresentInList("ipWhitelist", string ) && remove ||
                !ip && !storage.isValuePresentInList("userWhitelist", string) && remove
        ) {
            source.sendMessage(createMessage("O ${if (ip) "IP" else "username"} §a$string§f não está na whitelist."))
            return
        }

        if (remove) {
            try {
                storage.removeValueFromList(if (ip) "ipWhitelist" else "userWhitelist", string)
                source.sendMessage(createMessage("O ${if (ip) "IP" else "username"} §a$string§f foi removido da whitelist com sucesso!"))
                return
            } catch (e: IOException) {
                source.sendMessage(createMessage("Ocorreu um erro ao tentar remover o §a${if (ip) "IP" else "usuário"}§f da whitelist."))
                e.printStackTrace()
                return
            }
        }

        try {
            storage.appendValueToList(if (ip) "ipWhitelist" else "userWhitelist", string)
            source.sendMessage(createMessage("O ${if (ip) "IP" else "username"} §a$string§f foi adicionado à whitelist com sucesso!"))
            return
        } catch (e: IOException) {
            source.sendMessage(createMessage("Ocorreu um erro ao tentar adicionar o §a${if (ip) "IP" else "usuário"}§f à whitelist."))
            e.printStackTrace()
            return
        }
    }

    private fun createMessage(message: String): Component {
        return Component.text("AntiVPN", NamedTextColor.BLUE)
                .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                .append(Component.text(message, NamedTextColor.WHITE))
    }

    override fun hasPermission(invocation: Invocation): Boolean {
        return invocation.source().hasPermission("dev.thatlukinhasguy.AntiVPN.reload")
    }

    override fun suggestAsync(invocation: Invocation): CompletableFuture<List<String>> {
        val args = invocation.arguments()
        return when (args.size) {
            0, 1 -> CompletableFuture.completedFuture(listOf("whitelist"))
            2 -> CompletableFuture.completedFuture(listOf("user", "ip"))
            3 -> CompletableFuture.completedFuture(listOf("add", "remove"))
            else -> CompletableFuture.completedFuture(emptyList())
        }
    }
}