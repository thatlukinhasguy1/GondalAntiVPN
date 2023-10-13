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
        return createMessage("Correct usage: §a/antivpn whitelist <user/ip> <add/remove> <str>")
    }

    private fun handleWhitelist(source: CommandSource, args: Array<String>, ip: Boolean, remove: Boolean) {
        val string = args[3]

        if (string.isEmpty()) {
            source.sendMessage(createMessage("Correct usage: §a/antivpn whitelist <user/ip> <add/remove> <str>"))
            return
        }

        val storage = GsonStorage(File("plugins/AntiVPN/whitelist/list.json"))

        if (ip && storage.isValuePresentInList("ipWhitelist", string ) && !remove ||
                !ip && storage.isValuePresentInList("userWhitelist", string) && !remove
        ) {
            source.sendMessage(createMessage("The ${if (ip) "IP" else "username"} §a$string§f is already in whitelist."))
            return
        }

        if (ip && !storage.isValuePresentInList("ipWhitelist", string ) && remove ||
                !ip && !storage.isValuePresentInList("userWhitelist", string) && remove
        ) {
            source.sendMessage(createMessage("The ${if (ip) "IP" else "username"} §a$string§f is not in whitelist."))
            return
        }

        if (remove) {
            try {
                storage.removeValueFromList(if (ip) "ipWhitelist" else "userWhitelist", string)
                source.sendMessage(createMessage("The ${if (ip) "IP" else "username"} §a$string§f was successfully removed from the whitelist!"))
                return
            } catch (e: IOException) {
                source.sendMessage(createMessage("A problem occurred while deleting the §a${if (ip) "IP" else "username"}§f from the whitelist."))
                e.printStackTrace()
                return
            }
        }

        try {
            storage.appendValueToList(if (ip) "ipWhitelist" else "userWhitelist", string)
            source.sendMessage(createMessage("The ${if (ip) "IP" else "username"} §a$string§f was successfully added to the whitelist!"))
            return
        } catch (e: IOException) {
            source.sendMessage(createMessage("A problem occurred while adding the §a${if (ip) "IP" else "username"}§f to the whitelist."))
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