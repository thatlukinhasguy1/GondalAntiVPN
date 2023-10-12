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

class CommandImpl: SimpleCommand {
    override fun execute(invocation: Invocation?) {
        val source = invocation?.source()
        val args = invocation?.arguments()
        if (args?.size !!< 3 && !args[0].equals("reload")) {
            source?.sendMessage(invalidUsageMessage())
            return
        }

        when (args[0]) {
            "whitelist" -> {
                when (args[1]) {
                    "add" -> handleWhitelistAdd(source!!, args)
                    "remove" -> handleWhitelistRemove(source!!, args)
                    else -> source?.sendMessage(invalidUsageMessage())
                }
            }
            else -> source?.sendMessage(invalidUsageMessage())
        }
    }

    private fun invalidUsageMessage(): Component {
        return Component.text("GondalAntiVPN", NamedTextColor.BLUE)
                .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Invalid usage of the subcommands.", NamedTextColor.WHITE))
    }

    private fun handleWhitelistAdd(source: CommandSource, args: Array<String>) {
        val userName = args[2]

        if (GsonStorage(File("plugins/GondalAntiVPN/whitelist/list.json"))
                        .isValuePresentInList("whitelist", userName)) {
            source.sendMessage(successMessage("The username '$userName' is already in the whitelist."))
            return
        }

        try {
            GsonStorage(File("plugins/GondalAntiVPN/whitelist/list.json"))
                    .appendValueToList("whitelist", userName)
            source.sendMessage(successMessage("Successfully added '$userName' to the whitelist."))
        } catch (e: IOException) {
            source.sendMessage(errorMessage("There was an error while adding the user to the whitelist."))
            e.printStackTrace()
        }
    }

    private fun handleWhitelistRemove(source: CommandSource, args: Array<String>) {
        val userName = args[2]

        if (!GsonStorage(File("plugins/GondalAntiVPN/whitelist/list.json"))
                        .isValuePresentInList("whitelist", userName)) {
            source.sendMessage(successMessage("The username '$userName' was never in the whitelist."))
            return
        }

        try {
            GsonStorage(File("plugins/GondalAntiVPN/whitelist/list.json"))
                    .removeValueFromList("whitelist", userName)
            source.sendMessage(successMessage("Successfully removed '$userName' from the whitelist."))
        } catch (e: IOException) {
            source.sendMessage(errorMessage("There was an error while removing the user from the whitelist."))
            e.printStackTrace()
        }
    }

    private fun successMessage(message: String): Component {
        return Component.text("GondalAntiVPN", NamedTextColor.BLUE)
                .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                .append(Component.text(message, NamedTextColor.WHITE))
    }

    private fun errorMessage(message: String): Component {
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
            0 or 1 -> CompletableFuture.completedFuture(listOf("whitelist"))
            2 -> CompletableFuture.completedFuture(listOf("add", "remove"))
            else -> CompletableFuture.completedFuture(emptyList())
        }
    }
}
