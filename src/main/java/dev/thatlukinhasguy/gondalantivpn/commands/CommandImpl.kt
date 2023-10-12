package dev.thatlukinhasguy.gondalantivpn.commands

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.command.SimpleCommand.Invocation
import dev.thatlukinhasguy.gondalantivpn.storage.GsonStorage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.io.File
import java.io.IOException
import java.util.concurrent.CompletableFuture

class CommandImpl : SimpleCommand {
    override fun execute(invocation: Invocation?) {
        val source = invocation?.source()
        val args = invocation?.arguments()

        if (args?.size !!< 4) {
            source?.sendMessage(invalidUsageMessage())
            return
        }

        when (args[0]) {
            "whitelist" -> when (args[1]) {
                "ip" -> when (args[2]) {
                    "add" -> handleWhitelistAdd(source!!, args, ip = true)
                    "remove" -> handleWhitelistRemove(source!!, args, ip = true)
                }
                "user" -> when (args[2]) {
                    "add" -> handleWhitelistAdd(source!!, args, ip = false)
                    "remove" -> handleWhitelistRemove(source!!, args, ip = false)
                }
                else -> source?.sendMessage(invalidUsageMessage())
            }
            else -> source?.sendMessage(invalidUsageMessage())
        }
    }

    private fun invalidUsageMessage(): Component {
        return createMessage("Invalid usage of the subcommands.")
    }

    private fun handleWhitelistAdd(source: CommandSource, args: Array<String>, ip: Boolean) {
        val string = args[3]

        if (string.isBlank()) {
            source.sendMessage(createMessage("Invalid usage of the subcommand."))
            return
        }

        val storage = GsonStorage(File("plugins/GondalAntiVPN/whitelist/list.json"))

        if (ip && storage.isValuePresentInList("ipWhitelist", string) ||
                !ip && storage.isValuePresentInList("userWhitelist", string)
        ) {
            source.sendMessage(createMessage("The ${if (ip) "IP" else "username"} '$string' is already in the whitelist."))
            return
        }

        try {
            storage.appendValueToList(if (ip) "ipWhitelist" else "userWhitelist", string)
            source.sendMessage(createMessage("Successfully added '$string' to the whitelist."))
        } catch (e: IOException) {
            source.sendMessage(createMessage("There was an error while adding the ${if (ip) "IP" else "username"} from the whitelist."))
            e.printStackTrace()
        }
    }

    private fun handleWhitelistRemove(source: CommandSource, args: Array<String>, ip: Boolean) {
        val string = args[3]

        if (string.isBlank()) {
            source.sendMessage(createMessage("Invalid usage of the subcommand."))
            return
        }

        val storage = GsonStorage(File("plugins/GondalAntiVPN/whitelist/list.json"))

        if (ip && !storage.isValuePresentInList("ipWhitelist", string) ||
                !ip && !storage.isValuePresentInList("userWhitelist", string)
        ) {
            source.sendMessage(createMessage("The ${if (ip) "IP" else "username"} '$string' isn't in the whitelist."))
            return
        }

        try {
            storage.removeValueFromList(if (ip) "ipWhitelist" else "userWhitelist", string)
            source.sendMessage(createMessage("Successfully removed '$string' from the whitelist."))
        } catch (e: IOException) {
            source.sendMessage(createMessage("There was an error while removing the ${if (ip) "IP" else "username"} from the whitelist."))
            e.printStackTrace()
        }
    }


    private fun createMessage(message: String): Component {
        return Component.text("GondalAntiVPN", NamedTextColor.BLUE)
                .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                .append(Component.text(message, NamedTextColor.WHITE))
    }

    override fun hasPermission(invocation: Invocation): Boolean {
        return invocation.source().hasPermission("dev.thatlukinhasguy.GondalAntiVPN.reload")
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