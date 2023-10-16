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
    override fun execute(invocation: Invocation) {
        val source = invocation.source()
        val args = invocation.arguments()

        try {
            if (args.size != 4 && args[0] != "purge" && args[0] != "help") {
                source.sendMessage(invalidUsageMessage())
                return
            }
        } catch (e: ArrayIndexOutOfBoundsException) {
            source.sendMessage(invalidUsageMessage())
            return
        }

        when (args[0]) {
            "whitelist" -> handleWhitelistCommand(source, args)
            "purge" -> handlePurgeCommand(source, args)
            "help" -> handleHelpCommand(source)
            else -> source.sendMessage(invalidUsageMessage())
        }
    }

    private fun handleWhitelistCommand(source: CommandSource, args: Array<String>) {
        when (args[1]) {
            "ip" -> handleWhitelist(source, args, ip = true)
            "user" -> handleWhitelist(source, args, ip = false)
            else -> source.sendMessage(invalidUsageMessage())
        }
    }

    private fun handlePurgeCommand(source: CommandSource, args: Array<String>) {
        try {
            GsonStorage(File("./plugins/AntiVPN/blacklist/list.json")).setListToEmpty("badIps")
            source.sendMessage(createMessage("Successfully purged the bad IP data!"))
        } catch (e: IOException) {
            handleException(source, "purging", args)
        }
    }

    private fun handleWhitelist(source: CommandSource, args: Array<String>, ip: Boolean) {
        val string = args[3]

        if (string.isEmpty()) {
            source.sendMessage(createMessage("Correct usage: §a/antivpn whitelist <user/ip> <add/remove> <str>"))
            return
        }

        val storage = GsonStorage(File("plugins/AntiVPN/whitelist/list.json"))
        val type = if (ip) "ipWhitelist" else "userWhitelist"

        if ((ip && storage.isValuePresentInList(type, string)) || (!ip && storage.isValuePresentInList(type, string))) {
            val message = "The ${if (ip) "IP" else "username"} §a$string§f is ${if (!ip) "already " else ""}in the whitelist."
            source.sendMessage(createMessage(message))
            return
        }

        try {
            when {
                args[2] == "remove" -> {
                    storage.removeValueFromList(type, string)
                    val successMessage = "The ${if (ip) "IP" else "username"} §a$string§f was successfully removed from the whitelist!"
                    source.sendMessage(createMessage(successMessage))
                }
                else -> {
                    storage.appendValueToList(type, string)
                    if (ip) {
                        try {
                            GsonStorage(File("plugins/AntiVPN/blacklist/list.json")).removeValueFromList("badIps", string)
                        } catch (e: IOException) {
                            source.sendMessage(createMessage("The IP §a$string§f was successfully added to the whitelist!"))
                            return
                        }
                    }
                    val successMessage = "The ${if (ip) "IP" else "username"} §a$string§f was successfully added to the whitelist!"
                    source.sendMessage(createMessage(successMessage))
                    return
                }
            }
        } catch (e: IOException) {
            handleException(source, if (args[2] == "remove") "deleting" else "adding", args)
        }
    }

    private fun handleException(source: CommandSource, action: String, args: Array<String>) {
        source.sendMessage(createMessage("A problem occurred while $action the §a${if (args[2] == "ip") "IP" else "username"}§f to the whitelist."))
    }

    private fun invalidUsageMessage(): Component {
        return createMessage("Correct usage: §a/antivpn purge §for §a/antivpn whitelist <user/ip> <add/remove> <value>")
    }

    private fun handleHelpCommand(source: CommandSource) {
        source.sendMessage(createMessage("All commands:"))
        source.sendMessage(Component.text("§a- /antivpn purge: §fPurges the bad IP cache."))
        source.sendMessage(Component.text("§a- /antivpn whitelist <user/ip> <add/remove> <str>: §fHandles the whitelist methods."))
        source.sendMessage(Component.text("§a- /antivpn help: §fShows this message."))
    }

    private fun createMessage(message: String): Component {
        return Component.text("AntiVPN", NamedTextColor.BLUE)
            .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
            .append(Component.text(message, NamedTextColor.WHITE))
    }

    override fun hasPermission(invocation: Invocation): Boolean {
        return invocation.source().hasPermission("dev.thatlukinhasguy.antivpn")
    }

    override fun suggestAsync(invocation: Invocation): CompletableFuture<List<String>> {
        val args = invocation.arguments()
        return when {
            args.isEmpty() -> CompletableFuture.completedFuture(listOf("whitelist", "purge", "help"))
            args[0] == "whitelist" -> when (args.size) {
                1, 2 -> CompletableFuture.completedFuture(listOf("user", "ip"))
                3 -> CompletableFuture.completedFuture(listOf("add", "remove"))
                4 -> CompletableFuture.completedFuture(listOf("<value>"))
                else -> CompletableFuture.completedFuture(emptyList())
            }
            else -> CompletableFuture.completedFuture(emptyList())
        }
    }
}