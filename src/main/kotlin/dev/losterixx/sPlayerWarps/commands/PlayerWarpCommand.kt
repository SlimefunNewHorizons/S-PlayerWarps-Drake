package dev.losterixx.sPlayerWarps.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.losterixx.sapi.utils.config.ConfigManager
import dev.losterixx.sPlayerWarps.Main
import dev.losterixx.sPlayerWarps.other.ui.GuiManager
import dev.losterixx.sPlayerWarps.other.PWManager
import dev.losterixx.sPlayerWarps.other.ModalityManager
import dev.losterixx.sPlayerWarps.other.PlayerWarp
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.CompletableFuture

object PlayerWarpCommand : Listener {

    private val main = Main.instance
    private val mm = main.miniMessage
    private val config get() = ConfigManager.getConfig("config")
    private val prefix get() = config.getString("prefix") ?: Main.DEFAULT_PREFIX
    private val messages get() = ConfigManager.getConfig(config.getString("langFile", "english"))

    private val pendingTeleports = mutableMapOf<UUID, Pair<BukkitTask, Location>>()

    private fun normalizeSoundKey(soundName: String): String = soundName.lowercase()

    fun get(): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal("playerwarp")
            .requires { ctx -> ctx.sender is Player && ctx.sender.hasPermission("s-playerwarps.command.playerwarp.use") }
            .executes { ctx ->
                val sender = ctx.source.sender as Player

                if (sender.hasPermission("s-playerwarps.command.playerwarp.menu")) {
                    sender.performCommand("playerwarp menu")
                } else {
                    sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.usage")))
                }

                return@executes 1
            }
            .then(Commands.literal("help")
                .requires { ctx -> ctx.sender.hasPermission("s-playerwarps.command.playerwarp.help") }
                .executes { ctx ->
                    val sender = ctx.source.sender as Player

                    sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.usage")))

                    return@executes 1
                }
            )
            .then(Commands.literal("create")
                .requires { ctx -> ctx.sender.hasPermission("s-playerwarps.command.playerwarp.create") }
                .then(Commands.argument("identifier", StringArgumentType.string())
                    .executes { ctx ->
                        val sender = ctx.source.sender as Player

                        val configuredMax = config.getInt("playerwarps.maxWarpsPerPlayer", 5)
                        val maxWarpsPerPlayer = if (configuredMax > 0) {
                            configuredMax
                        } else if (configuredMax < 0) {
                            Int.MAX_VALUE
                        } else {
                            var permLimit = 0
                            for (i in 100 downTo 1) {
                                if (sender.hasPermission("s-playerwarps.limit.$i")) {
                                    permLimit = i
                                    break
                                }
                            }
                            permLimit
                        }

                        if (PWManager.getPlayerWarpsByOwner(sender.uniqueId).size >= maxWarpsPerPlayer) {
                            sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.create.maxWarpsReached")))
                            return@executes 1
                        }

                        if (ModalityManager.isDenied(sender.world.name) && !sender.hasPermission("s-playerwarps.admin.crossmodality")) {
                            sender.sendMessage(mm.deserialize(prefix + (messages.getString("commands.playerwarp.create.deniedWorld") ?: "<red>You cannot create player warps in this world!")))
                            return@executes 1
                        }

                        val identifier = StringArgumentType.getString(ctx, "identifier")

                        val identifierRegex = Regex(config.getString("playerwarps.identifier.regex", "^[A-Za-z0-9_]+$"))
                        val minLen = config.getInt("playerwarps.identifier.minLength", 4)
                        val maxLen = config.getInt("playerwarps.identifier.maxLength", 16)

                        if (!identifierRegex.matches(identifier)) {
                            sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.create.identifierRegex")
                                .replace("%warp%", identifier)))
                            return@executes 1
                        }

                        if (identifier.length !in minLen..maxLen) {
                            sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.create.identifierLength")
                                .replace("%warp%", identifier)))
                            return@executes 1
                        }

                        if (PWManager.existsPlayerWarp(identifier)) {
                            sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.create.alreadyExists")
                                .replace("%warp%", identifier)))
                            return@executes 1
                        }

                        PWManager.addPlayerWarp(
                            PlayerWarp(identifier, sender.uniqueId, sender.location, material = Material.getMaterial(config.getString("playerwarps.defaultWarpIcon")) ?: Material.COMPASS)
                        )

                        sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.create.success")
                            .replace("%warp%", identifier)))

                        return@executes 1
                    }
                )
            )
            .then(Commands.literal("teleport")
                .requires { ctx -> ctx.sender.hasPermission("s-playerwarps.command.playerwarp.teleport") }
                .then(Commands.argument("identifier", StringArgumentType.string())
                    .suggests { ctx, builder ->
                        val input = try {
                            StringArgumentType.getString(ctx, "identifier").lowercase()
                        } catch (_: IllegalArgumentException) { "" }

                        val sender = ctx.source.sender as? Player
                        CompletableFuture.supplyAsync {
                            val playerModality = sender?.let { ModalityManager.getModality(it.world.name) }
                            PWManager.getAllWarps()
                                .filter { w ->
                                    if (sender == null || sender.hasPermission("s-playerwarps.admin.crossmodality")) true
                                    else ModalityManager.getModality(w.location.world?.name) == playerModality
                                }
                                .map { it.identifier }
                                .filter { it.lowercase().startsWith(input) || it.lowercase() == input || input.isEmpty() }
                        }.thenApply { filteredWarps ->
                            filteredWarps.forEach { builder.suggest(it) }
                            builder.build()
                        }
                    }
                    .executes { ctx ->
                        val sender = ctx.source.sender as Player

                        val identifier = StringArgumentType.getString(ctx, "identifier")

                        val identifierRegex = Regex(config.getString("playerwarps.identifier.regex", "^[a-z_]+$"))
                        val minLen = config.getInt("playerwarps.identifier.minLength", 4)
                        val maxLen = config.getInt("playerwarps.identifier.maxLength", 16)

                        if (!identifierRegex.matches(identifier) || identifier.length !in minLen..maxLen || !PWManager.existsPlayerWarp(identifier)) {
                            sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.teleport.notFound")
                                .replace("%warp%", identifier)))
                            return@executes 1
                        }

                        val playerWarp = PWManager.getPlayerWarp(identifier) ?: run {
                            sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.teleport.notFound")
                                .replace("%warp%", identifier)))
                            return@executes 1
                        }

                        if (!ModalityManager.isSameModality(sender.world.name, playerWarp.location.world?.name) && !sender.hasPermission("s-playerwarps.admin.crossmodality")) {
                            sender.sendMessage(mm.deserialize(prefix + (messages.getString("commands.playerwarp.teleport.otherModality") ?: "<red>You cannot teleport to a warp in another modality!")))
                            return@executes 1
                        }

                        pendingTeleports[sender.uniqueId]?.let { (existingTask, _) ->
                            existingTask.cancel()
                            pendingTeleports.remove(sender.uniqueId)
                        }

                        val delayEnabled = config.getBoolean("playerwarps.teleportation.delay.enabled", true)
                        val delaySeconds = config.getInt("playerwarps.teleportation.delay.delay", 3)

                        if (!delayEnabled || delaySeconds <= 0) {
                            sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.teleport.teleporting")
                                .replace("%warp%", identifier)))
                            playerWarp.teleport(sender)
                            sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.teleport.success")
                                .replace("%warp%", identifier)))

                            if (config.getBoolean("sounds.teleportSound.enabled", true)) {
                                val soundName = config.getString("sounds.teleportSound.sound", "ENTITY_ENDERMAN_TELEPORT")!!
                                val volume = config.getDouble("sounds.teleportSound.volume", 1.0).toFloat()
                                val pitch = config.getDouble("sounds.teleportSound.pitch", 1.0).toFloat()
                                try {
                                    sender.playSound(sender.location, normalizeSoundKey(soundName), volume, pitch)
                                } catch (e: Exception) {
                                    main.logger.warning("Error while playing sound $soundName: ${e.message}")
                                }
                            }
                        } else {
                            sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.teleport.teleporting")
                                .replace("%warp%", identifier)))
                            var remaining = delaySeconds

                            val task = main.server.scheduler.runTaskTimer(main, Runnable {
                                if (!sender.isOnline) {
                                    pendingTeleports.remove(sender.uniqueId)
                                    return@Runnable
                                }

                                remaining -= 1
                                if (remaining <= 0) {
                                    playerWarp.teleport(sender)
                                    sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.teleport.success")
                                        .replace("%warp%", identifier)))

                                    if (config.getBoolean("sounds.teleportSound.enabled", true)) {
                                        val soundName = config.getString("sounds.teleportSound.sound", "ENTITY_ENDERMAN_TELEPORT")!!
                                        val volume = config.getDouble("sounds.teleportSound.volume", 1.0).toFloat()
                                        val pitch = config.getDouble("sounds.teleportSound.pitch", 1.0).toFloat()
                                        try {
                                            sender.playSound(sender.location, normalizeSoundKey(soundName), volume, pitch)
                                        } catch (e: Exception) {
                                            main.logger.warning("Error while playing sound $soundName: ${e.message}" )
                                        }
                                    }

                                    pendingTeleports[sender.uniqueId]?.first?.cancel()
                                    pendingTeleports.remove(sender.uniqueId)
                                } else {
                                    if (config.getBoolean("sounds.delaySound.enabled", true)) {
                                        val delaySoundName = config.getString("sounds.delaySound.sound", "BLOCK_NOTE_BLOCK_BASS")!!
                                        val delaySoundVolume = config.getDouble("sounds.delaySound.volume", 1.0).toFloat()
                                        val delaySoundPitch = config.getDouble("sounds.delaySound.pitch", 1.0).toFloat()
                                        try {
                                            sender.playSound(sender.location, normalizeSoundKey(delaySoundName), delaySoundVolume, delaySoundPitch)
                                        } catch (e: Exception) {
                                            main.logger.warning("Error while playing sound $delaySoundName: ${e.message}")
                                        }
                                    }
                                }

                            }, 0L, 20L)

                            pendingTeleports[sender.uniqueId] = Pair(task, sender.location)
                        }

                        return@executes 1
                    }
                )
            )
            .then(Commands.literal("menu")
                .requires { ctx -> ctx.sender.hasPermission("s-playerwarps.command.playerwarp.menu") }
                .executes { ctx ->
                    val sender = ctx.source.sender as Player

                    GuiManager.openMainMenu(sender)

                    return@executes 1
                }
            )
            .then(Commands.literal("delete")
                .requires { ctx -> ctx.sender.hasPermission("s-playerwarps.command.playerwarp.delete") }
                .then(Commands.argument("identifier", StringArgumentType.string())
                    .suggests { ctx, builder ->
                        val input = try {
                            StringArgumentType.getString(ctx, "identifier").lowercase()
                        } catch (_: IllegalArgumentException) { "" }

                        val sender = ctx.source.sender as? Player

                        CompletableFuture.supplyAsync {
                            if (sender != null) {
                                PWManager.getPlayerWarpsByOwner(sender.uniqueId)
                                    .map { it.identifier }
                                    .filter { it.lowercase().startsWith(input) || it.lowercase() == input || input.isEmpty() }
                            } else {
                                emptyList()
                            }
                        }.thenApply { filteredWarps ->
                            filteredWarps.forEach { builder.suggest(it) }
                            builder.build()
                        }
                    }
                    .executes { ctx ->
                        val sender = ctx.source.sender as Player
                        val identifier = StringArgumentType.getString(ctx, "identifier")

                        val identifierRegex = Regex(config.getString("playerwarps.identifier.regex", "^[A-Za-z0-9_]+$"))
                        val minLen = config.getInt("playerwarps.identifier.minLength", 4)
                        val maxLen = config.getInt("playerwarps.identifier.maxLength", 16)

                        if (!identifierRegex.matches(identifier) || identifier.length !in minLen..maxLen) {
                            sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.delete.notFound")
                                .replace("%warp%", identifier)))
                            return@executes 1
                        }

                        val playerWarp = PWManager.getPlayerWarp(identifier)

                        if (playerWarp == null) {
                            sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.delete.notFound")
                                .replace("%warp%", identifier)))
                            return@executes 1
                        }

                        if (playerWarp.owner != sender.uniqueId) {
                            sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.delete.notOwner")
                                .replace("%warp%", identifier)))
                            return@executes 1
                        }

                        PWManager.removePlayerWarp(playerWarp)
                        sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.delete.success")
                            .replace("%warp%", identifier)))

                        return@executes 1
                    }
                )
            )
            .then(Commands.literal("edit")
                .requires { ctx -> ctx.sender.hasPermission("s-playerwarps.command.playerwarp.edit") }
                .then(Commands.argument("identifier", StringArgumentType.string())
                    .suggests { ctx, builder ->
                        val input = try {
                            StringArgumentType.getString(ctx, "identifier").lowercase()
                        } catch (_: IllegalArgumentException) { "" }

                        val sender = ctx.source.sender as? Player

                        CompletableFuture.supplyAsync {
                            if (sender != null) {
                                PWManager.getPlayerWarpsByOwner(sender.uniqueId)
                                    .map { it.identifier }
                                    .filter { it.lowercase().startsWith(input) || it.lowercase() == input || input.isEmpty() }
                            } else {
                                emptyList()
                            }
                        }.thenApply { filteredWarps ->
                            filteredWarps.forEach { builder.suggest(it) }
                            builder.build()
                        }
                    }
                    .then(Commands.literal("displayname")
                        .then(Commands.argument("value", StringArgumentType.greedyString())
                            .executes { ctx ->
                                val sender = ctx.source.sender as Player
                                val identifier = StringArgumentType.getString(ctx, "identifier")
                                val displayName = StringArgumentType.getString(ctx, "value")

                                val identifierRegex = Regex(config.getString("playerwarps.identifier.regex", "^[A-Za-z0-9_]+$"))
                                val minLen = config.getInt("playerwarps.identifier.minLength", 4)
                                val maxLen = config.getInt("playerwarps.identifier.maxLength", 16)

                                if (!identifierRegex.matches(identifier) || identifier.length !in minLen..maxLen) {
                                    sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.edit.notFound")
                                        .replace("%warp%", identifier)))
                                    return@executes 1
                                }

                                val playerWarp = PWManager.getPlayerWarp(identifier)

                                if (playerWarp == null) {
                                    sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.edit.notFound")
                                        .replace("%warp%", identifier)))
                                    return@executes 1
                                }

                                if (playerWarp.owner != sender.uniqueId) {
                                    sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.edit.notOwner")
                                        .replace("%warp%", identifier)))
                                    return@executes 1
                                }

                                val displayNameMaxLen = config.getInt("playerwarps.displayName.maxLength", 32)
                                if (displayName.length > displayNameMaxLen) {
                                    sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.edit.displayNameTooLong")
                                        .replace("%warp%", identifier)
                                        .replace("%max%", displayNameMaxLen.toString())))
                                    return@executes 1
                                }

                                playerWarp.displayName = displayName
                                sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.edit.displayNameSuccess")
                                    .replace("%warp%", identifier)
                                    .replace("%displayname%", displayName)))

                                return@executes 1
                            }
                        )
                    )
                    .then(Commands.literal("icon")
                        .then(Commands.argument("value", StringArgumentType.greedyString())
                            .executes { ctx ->
                                val sender = ctx.source.sender as Player
                                val identifier = StringArgumentType.getString(ctx, "identifier")
                                val materialName = StringArgumentType.getString(ctx, "value")

                                val identifierRegex = Regex(config.getString("playerwarps.identifier.regex", "^[A-Za-z0-9_]+$"))
                                val minLen = config.getInt("playerwarps.identifier.minLength", 4)
                                val maxLen = config.getInt("playerwarps.identifier.maxLength", 16)

                                if (!identifierRegex.matches(identifier) || identifier.length !in minLen..maxLen) {
                                    sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.edit.notFound")
                                        .replace("%warp%", identifier)))
                                    return@executes 1
                                }

                                val playerWarp = PWManager.getPlayerWarp(identifier)

                                if (playerWarp == null) {
                                    sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.edit.notFound")
                                        .replace("%warp%", identifier)))
                                    return@executes 1
                                }

                                if (playerWarp.owner != sender.uniqueId) {
                                    sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.edit.notOwner")
                                        .replace("%warp%", identifier)))
                                    return@executes 1
                                }

                                val normalizedMaterialName = materialName.uppercase().replace(" ", "_")
                                val material = Material.getMaterial(normalizedMaterialName)

                                if (material == null || !material.isItem) {
                                    sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.edit.invalidMaterial")
                                        .replace("%warp%", identifier)
                                        .replace("%material%", materialName)))
                                    return@executes 1
                                }

                                playerWarp.material = material
                                // Persist immediately so a restart cannot restore an invalid old icon.
                                PWManager.saveAllToDisk()
                                sender.sendMessage(mm.deserialize(prefix + messages.getString("commands.playerwarp.edit.iconSuccess")
                                    .replace("%warp%", identifier)
                                    .replace("%material%", material.name)))

                                return@executes 1
                            }
                        )
                    )
                )
            )
    }

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        val player = event.player
        val from = event.from
        val to = event.to

        val stored = pendingTeleports[player.uniqueId] ?: return
        if (!config.getBoolean("playerwarps.teleportation.delay.cancelOnMove", true)) return
        if (from.distanceSquared(to) < 0.02) return

        val (task, _) = stored
        if (!task.isCancelled) task.cancel()
        pendingTeleports.remove(player.uniqueId)

        if (config.getBoolean("sounds.cancelSound.enabled", true)) {
            val cancelSoundName = config.getString("sounds.cancelSound.sound", "ENTITY_VILLAGER_NO")!!
            val cancelVolume = config.getDouble("sounds.cancelSound.volume", 1.0).toFloat()
            val cancelPitch = config.getDouble("sounds.cancelSound.pitch", 1.0).toFloat()
            try {
                player.playSound(player.location, normalizeSoundKey(cancelSoundName), cancelVolume, cancelPitch)
            } catch (_: Exception) { }
        }

        player.sendMessage(mm.deserialize(prefix + (messages.getString("commands.playerwarp.teleport.canceled"))))
    }

}
