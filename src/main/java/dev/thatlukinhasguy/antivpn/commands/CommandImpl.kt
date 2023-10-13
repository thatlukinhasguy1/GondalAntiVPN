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

class CommandImpl : SimpleCommand {
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
            "whitelist" -> when (args[1]) {
                "ip" -> when (args[2]) {
                    "add" -> handleWhitelist(source, args, ip = true, remove = false)
                    "remove" -> handleWhitelist(source, args, ip = true, remove = true)
                }
                "user" -> when (args[2]) {
                    "add" -> handleWhitelist(source, args, ip = false, remove = false)
                    "remove" -> handleWhitelist(source, args, ip = false, remove = true)
                }
                else -> source.sendMessage(invalidUsageMessage())
            }
            "purge" -> handlePurge(source)
            "help" -> handleHelp(source)
            else -> source.sendMessage(invalidUsageMessage())
        }
    }

    private fun invalidUsageMessage(): Component {
        return createMessage("Correct usage: §a/antivpn purge §for §a/antivpn whitelist <user/ip> <add/remove> <value>")
    }

    private fun handleHelp(source: CommandSource) {
        source.sendMessage(createMessage("All commands:"))
        source.sendMessage(Component.text("§a- /antivpn purge: §fPurges the bad IP cache."))
        source.sendMessage(Component.text("§a- /antivpn whitelist <user/ip> <add/remove> <str>: §fHandles the whitelist methods."))
        source.sendMessage(Component.text("§a- /antivpn help: §fShows this message."))
    }

    private fun handlePurge(source: CommandSource) {
        try {
            GsonStorage(File("./plugins/AntiVPN/blacklist/list.json")).setListToEmpty("badIps")
            source.sendMessage(createMessage("Successfully purged the bad IP data!"))
        } catch (e: IOException) {
            source.sendMessage(createMessage("An error occurred while purging the bad IP data."))
            e.printStackTrace()
        }
    }

    private fun handleWhitelist(source: CommandSource, args: Array<String>, ip: Boolean, remove: Boolean) {
        val string = args[3]

        if (string.isEmpty()) {
            source.sendMessage(createMessage("Correct usage: §a/antivpn whitelist <user/ip> <add/remove> <str>"))
            return
        }

        val storage = GsonStorage(File("plugins/AntiVPN/whitelist/list.json"))

        if ((ip && storage.isValuePresentInList("ipWhitelist", string) && !remove) ||
            (!ip && storage.isValuePresentInList("userWhitelist", string) && !remove)
        ) {
            source.sendMessage(createMessage("The ${if (ip) "IP" else "username"} §a$string§f is already in the whitelist."))
            return
        }

        if ((ip && !storage.isValuePresentInList("ipWhitelist", string) && remove) ||
            (!ip && !storage.isValuePresentInList("userWhitelist", string) && remove)
        ) {
            source.sendMessage(createMessage("The ${if (ip) "IP" else "username"} §a$string§f is not in the whitelist."))
            return
        }

        try {
            when {
                remove -> {
                    storage.removeValueFromList(if (ip) "ipWhitelist" else "userWhitelist", string)
                    source.sendMessage(createMessage("The ${if (ip) "IP" else "username"} §a$string§f was successfully removed from the whitelist!"))
                }
                else -> {
                    storage.appendValueToList(if (ip) "ipWhitelist" else "userWhitelist", string)
                    source.sendMessage(createMessage("The ${if (ip) "IP" else "username"} §a$string§f was successfully added to the whitelist!"))
                }
            }
        } catch (e: IOException) {
            source.sendMessage(createMessage("A problem occurred while ${if (remove) "deleting" else "adding"} the §a${if (ip) "IP" else "username"}§f to the whitelist."))
            e.printStackTrace()
        }
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
