package dev.losterixx.sPlayerWarps.other.ui

import dev.losterixx.sPlayerWarps.Main
import dev.losterixx.sPlayerWarps.other.Cache
import dev.losterixx.sPlayerWarps.other.PWManager
import dev.losterixx.sPlayerWarps.other.ModalityManager
import dev.losterixx.sapi.utils.builder.ItemBuilder
import dev.losterixx.sapi.utils.config.ConfigManager
import dev.triumphteam.gui.builder.item.PaperItemBuilder
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object GuiManager {

    private val main = Main.instance
    private val mm = main.miniMessage
    private val config get() = ConfigManager.getConfig("config")
    private val mainMenu get() = ConfigManager.getConfig("mainMenu")
    private val ownWarpsMenu get() = ConfigManager.getConfig("ownWarpsMenu")
    private val editWarpMenu get() = ConfigManager.getConfig("editWarpMenu")

    private val refreshTasks: MutableMap<UUID, BukkitTask> = ConcurrentHashMap()

    fun openMainMenu(player: Player) {
        val title = mm.deserialize(mainMenu.getString("title", "config error"))
        val rows = mainMenu.getInt("rows", 6).coerceIn(1, 6)
        val autoRefresh = mainMenu.getInt("autoRefresh", -1)

        val gui = Gui.paginated()
            .title(title)
            .rows(rows)
            .pageSize(mainMenu.getSection("extra")?.getSection("warpButton")?.getIntList("slots", listOf())!!.size)
            .create()

        val contentSection = mainMenu.getSection("content")
        if (contentSection != null) {
            for (key in contentSection.getRoutesAsStrings(false)) {
                val entry = contentSection.getSection(key) ?: continue
                val slots = entry.getIntList("slots")
                val itemSection = entry.getSection("item") ?: continue
                val guiItem = ItemBuilder.buildGuiItemFromConfig(itemSection) ?: continue
                for (slot in slots) {
                    gui.setItem(slot, guiItem)
                }
            }
        }

        val buttons = mainMenu.getSection("buttons")
        if (buttons != null) {
            val closeSection = buttons.getSection("closeButton")
            if (closeSection != null && closeSection.getBoolean("enabled", true)) {
                val slot = closeSection.getInt("slot", 49)
                val originalItem = ItemBuilder.buildGuiItemFromConfig(closeSection.getSection("item"))
                val itemStack = originalItem?.itemStack ?: PaperItemBuilder.from(Material.BARRIER).name(mm.deserialize("config error")).build()
                val closeItem = GuiItem(itemStack) { event ->
                    val player = event.whoClicked as? Player
                    player?.closeInventory()
                }
                gui.setItem(slot, closeItem)
            }

            val nextSection = buttons.getSection("nextPage")
            if (nextSection != null && nextSection.getBoolean("enabled", true)) {
                val slot = nextSection.getInt("slot", 52)
                val originalItem = ItemBuilder.buildGuiItemFromConfig(nextSection.getSection("item"))
                val itemStack = originalItem?.itemStack ?: PaperItemBuilder.from(Material.BARRIER).name(mm.deserialize("config error")).build()
                val nextItem = GuiItem(itemStack) { event ->
                    gui.next()
                }
                gui.setItem(slot, nextItem)
            }

            val prevSection = buttons.getSection("previousPage")
            if (prevSection != null && prevSection.getBoolean("enabled", true)) {
                val slot = prevSection.getInt("slot", 46)
                val originalItem = ItemBuilder.buildGuiItemFromConfig(prevSection.getSection("item"))
                val itemStack = originalItem?.itemStack ?: PaperItemBuilder.from(Material.BARRIER).name(mm.deserialize("config error")).build()
                val prevItem = GuiItem(itemStack) { event ->
                    gui.previous()
                }
                gui.setItem(slot, prevItem)
            }

            val ownSection = buttons.getSection("ownWarps")
            if (ownSection != null && ownSection.getBoolean("enabled", true)) {
                val slot = ownSection.getInt("slot", 4)
                val originalItem = ItemBuilder.buildGuiItemFromConfig(ownSection.getSection("item"))
                val itemStack = originalItem?.itemStack ?: PaperItemBuilder.from(Material.BARRIER).name(mm.deserialize("config error")).build()
                val ownItem = GuiItem(itemStack) { event ->
                    val player = event.whoClicked as? Player ?: return@GuiItem
                    openOwnWarpsMenu(player)
                }
                gui.setItem(slot, ownItem)
            }
        }

        val warpExtra = mainMenu.getSection("extra")?.getSection("warpButton")
        val warpNameTemplate = warpExtra?.getSection("item")?.getString("name", "%warp_displayname%")!!
        val warpLoreTemplate = warpExtra?.getSection("item")?.getStringList("lore", listOf())!!
        val playerModality = ModalityManager.getModality(player.world.name)
        val warps = PWManager.getAllWarps()
            .filter { w ->
                if (player.hasPermission("s-playerwarps.admin.crossmodality")) true
                else ModalityManager.getModality(w.location.world?.name) == playerModality
            }
            .sortedByDescending { w -> w.uniqueUses.size }

        for (warp in warps) {
            val material = warp.material
            val displayName = warp.displayName ?: warp.identifier
            val owner = Cache.nameCache[warp.owner] ?: main.server.getOfflinePlayer(warp.owner).name ?: "???"
            val world = warp.location.world?.name ?: "???"
            val location = "${warp.location.blockX}, ${warp.location.blockY}, ${warp.location.blockZ}"
            val uniqueUses = warp.uniqueUses.size
            val uses = warp.totalUses

            val nameStr = warpNameTemplate
                .replace("%warp_displayname%", displayName)
                .replace("%warp_owner%", owner)
                .replace("%warp_world%", world)
                .replace("%warp_location%", location)
                .replace("%warp_uses%", uses.toString())
                .replace("%warp_unique_uses%", uniqueUses.toString())

            val loreList = warpLoreTemplate.map { line ->
                line.replace("%warp_displayname%", displayName)
                    .replace("%warp_owner%", owner)
                    .replace("%warp_world%", world)
                    .replace("%warp_location%", location)
                    .replace("%warp_uses%", uses.toString())
                    .replace("%warp_unique_uses%", uniqueUses.toString())
            }

            val itemStack = ItemStack(material)
            val meta = itemStack.itemMeta
            meta?.displayName(mm.deserialize(nameStr).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
            meta?.lore(loreList.map { mm.deserialize(it).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE) })
            itemStack.itemMeta = meta

            val guiItem = GuiItem(itemStack) { event ->
                val p = event.whoClicked as? Player ?: return@GuiItem
                if (!ModalityManager.isSameModality(p.world.name, warp.location.world?.name) && !p.hasPermission("s-playerwarps.admin.crossmodality")) {
                    val messages = ConfigManager.getConfig(config?.getString("langFile", "english") ?: "english")
                    val prefix = config?.getString("prefix") ?: Main.DEFAULT_PREFIX
                    p.sendMessage(mm.deserialize(prefix + (messages?.getString("commands.playerwarp.teleport.otherModality") ?: "<red>This warp belongs to another modality!")))
                    p.closeInventory()
                    return@GuiItem
                }
                p.performCommand("playerwarp teleport ${warp.identifier}")
                p.closeInventory()
            }

            gui.addItem(guiItem)
        }

        gui.disableAllInteractions()
        gui.setCloseGuiAction { event ->
            val p = event.player as? Player
            refreshTasks.remove(p?.uniqueId)?.cancel()
        }
        gui.open(player)

        if (autoRefresh > 0) {
            refreshTasks.remove(player.uniqueId)?.cancel()

            val task = main.server.scheduler.runTaskTimer(main, Runnable {
                if (!player.isOnline) {
                    refreshTasks.remove(player.uniqueId)?.cancel()
                    return@Runnable
                }

                gui.update()
            }, autoRefresh * 20L, autoRefresh * 20L)

            refreshTasks[player.uniqueId] = task
        }
    }

    fun openOwnWarpsMenu(player: Player) {
        val title = mm.deserialize(ownWarpsMenu.getString("title", "config error"))
        val rows = ownWarpsMenu.getInt("rows", 6).coerceIn(1, 6)
        val autoRefresh = ownWarpsMenu.getInt("autoRefresh", -1)

        val gui = Gui.paginated()
            .title(title)
            .rows(rows)
            .pageSize(ownWarpsMenu.getSection("extra")?.getSection("warpButton")?.getIntList("slots", listOf())!!.size)
            .create()

        val contentSection = ownWarpsMenu.getSection("content")
        if (contentSection != null) {
            for (key in contentSection.getRoutesAsStrings(false)) {
                val entry = contentSection.getSection(key) ?: continue
                val slots = entry.getIntList("slots")
                val itemSection = entry.getSection("item") ?: continue
                val guiItem = ItemBuilder.buildGuiItemFromConfig(itemSection) ?: continue
                for (slot in slots) {
                    gui.setItem(slot, guiItem)
                }
            }
        }

        val buttons = ownWarpsMenu.getSection("buttons")
        if (buttons != null) {
            val returnSection = buttons.getSection("returnButton")
            if (returnSection != null && returnSection.getBoolean("enabled", true)) {
                val slot = returnSection.getInt("slot", 49)
                val originalItem = ItemBuilder.buildGuiItemFromConfig(returnSection.getSection("item"))
                val itemStack = originalItem?.itemStack ?: PaperItemBuilder.from(Material.BARRIER).name(mm.deserialize("config error")).build()
                val returnItem = GuiItem(itemStack) { event ->
                    val player = event.whoClicked as? Player ?: return@GuiItem
                    openMainMenu(player)
                }
                gui.setItem(slot, returnItem)
            }

            val nextSection = buttons.getSection("nextPage")
            if (nextSection != null && nextSection.getBoolean("enabled", true)) {
                val slot = nextSection.getInt("slot", 52)
                val originalItem = ItemBuilder.buildGuiItemFromConfig(nextSection.getSection("item"))
                val itemStack = originalItem?.itemStack ?: PaperItemBuilder.from(Material.BARRIER).name(mm.deserialize("config error")).build()
                val nextItem = GuiItem(itemStack) { event ->
                    gui.next()
                }
                gui.setItem(slot, nextItem)
            }

            val prevSection = buttons.getSection("previousPage")
            if (prevSection != null && prevSection.getBoolean("enabled", true)) {
                val slot = prevSection.getInt("slot", 46)
                val originalItem = ItemBuilder.buildGuiItemFromConfig(prevSection.getSection("item"))
                val itemStack = originalItem?.itemStack ?: PaperItemBuilder.from(Material.BARRIER).name(mm.deserialize("config error")).build()
                val prevItem = GuiItem(itemStack) { event ->
                    gui.previous()
                }
                gui.setItem(slot, prevItem)
            }

            val createSection = buttons.getSection("createButton")
            if (createSection != null && createSection.getBoolean("enabled", true)) {
                val slot = createSection.getInt("slot", 4)
                val originalItem = ItemBuilder.buildGuiItemFromConfig(createSection.getSection("item"))
                val itemStack = originalItem?.itemStack ?: PaperItemBuilder.from(Material.BARRIER).name(mm.deserialize("config error")).build()
                val createItem = GuiItem(itemStack) { event ->
                    val player = event.whoClicked as? Player ?: return@GuiItem
                    DialogManager.openCreateDialog(player)
                }
                gui.setItem(slot, createItem)
            }
        }

        val warpExtra = ownWarpsMenu.getSection("extra")?.getSection("warpButton")
        val warpNameTemplate = warpExtra?.getSection("item")?.getString("name", "%warp_displayname%")!!
        val warpLoreTemplate = warpExtra?.getSection("item")?.getStringList("lore", listOf())!!
        val playerModality = ModalityManager.getModality(player.world.name)
        val warps = PWManager.getPlayerWarpsByOwner(player.uniqueId)
            .filter { w ->
                if (player.hasPermission("s-playerwarps.admin.crossmodality")) true
                else ModalityManager.getModality(w.location.world?.name) == playerModality
            }
            .sortedByDescending { w -> w.uniqueUses.size }

        for (warp in warps) {
            val material = warp.material
            val displayName = warp.displayName ?: warp.identifier
            val owner = main.server.getOfflinePlayer(warp.owner).name ?: "???"
            val world = warp.location.world?.name ?: "???"
            val location = "${warp.location.blockX}, ${warp.location.blockY}, ${warp.location.blockZ}"
            val uniqueUses = warp.uniqueUses.size
            val uses = warp.totalUses

            val nameStr = warpNameTemplate
                .replace("%warp_displayname%", displayName)
                .replace("%warp_owner%", owner)
                .replace("%warp_world%", world)
                .replace("%warp_location%", location)
                .replace("%warp_uses%", uses.toString())
                .replace("%warp_unique_uses%", uniqueUses.toString())

            val loreList = warpLoreTemplate.map { line ->
                line.replace("%warp_displayname%", displayName)
                    .replace("%warp_owner%", owner)
                    .replace("%warp_world%", world)
                    .replace("%warp_location%", location)
                    .replace("%warp_uses%", uses.toString())
                    .replace("%warp_unique_uses%", uniqueUses.toString())
            }

            val itemStack = ItemStack(material)
            val meta = itemStack.itemMeta
            meta?.displayName(mm.deserialize(nameStr).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
            meta?.lore(loreList.map { mm.deserialize(it).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE) })
            itemStack.itemMeta = meta

            val guiItem = GuiItem(itemStack) { event ->
                val player = event.whoClicked as? Player ?: return@GuiItem
                openEditWarpMenu(player, warp.identifier)
            }

            gui.addItem(guiItem)
        }

        gui.disableAllInteractions()
        gui.setCloseGuiAction { event ->
            val p = event.player as? Player
            refreshTasks.remove(p?.uniqueId)?.cancel()
        }
        gui.open(player)

        if (autoRefresh > 0) {
            refreshTasks.remove(player.uniqueId)?.cancel()

            val task = main.server.scheduler.runTaskTimer(main, Runnable {
                if (!player.isOnline) {
                    refreshTasks.remove(player.uniqueId)?.cancel()
                    return@Runnable
                }

                gui.update()
            }, autoRefresh * 20L, autoRefresh * 20L)

            refreshTasks[player.uniqueId] = task
        }
    }

    fun openEditWarpMenu(player: Player, warpIdentifier: String) {
        val title = mm.deserialize(editWarpMenu.getString("title", "config error"))
        val rows = editWarpMenu.getInt("rows", 4).coerceIn(1, 6)
        val autoRefresh = editWarpMenu.getInt("autoRefresh", -1)

        val gui = Gui.gui()
            .title(title)
            .rows(rows)
            .create()

        val contentSection = editWarpMenu.getSection("content")
        if (contentSection != null) {
            for (key in contentSection.getRoutesAsStrings(false)) {
                val entry = contentSection.getSection(key) ?: continue
                val slots = entry.getIntList("slots")
                val itemSection = entry.getSection("item") ?: continue
                val guiItem = ItemBuilder.buildGuiItemFromConfig(itemSection) ?: continue
                for (slot in slots) {
                    gui.setItem(slot, guiItem)
                }
            }
        }

        val buttons = editWarpMenu.getSection("buttons")
        if (buttons != null) {
            val returnSection = buttons.getSection("returnButton")
            if (returnSection != null && returnSection.getBoolean("enabled", true)) {
                val slot = returnSection.getInt("slot", 31)
                val originalItem = ItemBuilder.buildGuiItemFromConfig(returnSection.getSection("item"))
                val itemStack = originalItem?.itemStack ?: PaperItemBuilder.from(Material.BARRIER)
                    .name(mm.deserialize("config error")).build()
                val returnItem = GuiItem(itemStack) { event ->
                    val player = event.whoClicked as? Player ?: return@GuiItem
                    openOwnWarpsMenu(player)
                }
                gui.setItem(slot, returnItem)
            }

            val teleportSection = buttons.getSection("teleportButton")
            if (teleportSection != null && teleportSection.getBoolean("enabled", true)) {
                val slot = teleportSection.getInt("slot", 10)
                val originalItem = ItemBuilder.buildGuiItemFromConfig(teleportSection.getSection("item"))
                val itemStack = originalItem?.itemStack ?: PaperItemBuilder.from(Material.BARRIER)
                    .name(mm.deserialize("config error")).build()
                val teleportItem = GuiItem(itemStack) { event ->
                    val player = event.whoClicked as? Player ?: return@GuiItem
                    player.performCommand("playerwarp teleport $warpIdentifier")
                    player.closeInventory()
                }
                gui.setItem(slot, teleportItem)
            }

            val displaynameSection = buttons.getSection("displaynameButton")
            if (displaynameSection != null && displaynameSection.getBoolean("enabled", true)) {
                val slot = displaynameSection.getInt("slot", 12)
                val originalItem = ItemBuilder.buildGuiItemFromConfig(displaynameSection.getSection("item"))
                val itemStack = originalItem?.itemStack ?: PaperItemBuilder.from(Material.BARRIER)
                    .name(mm.deserialize("config error")).build()
                val displaynameItem = GuiItem(itemStack) { event ->
                    val player = event.whoClicked as? Player ?: return@GuiItem
                    DialogManager.openDisplaynameDialog(player, warpIdentifier)
                }
                gui.setItem(slot, displaynameItem)
            }

            val iconSection = buttons.getSection("iconButton")
            if (iconSection != null && iconSection.getBoolean("enabled", true)) {
                val slot = iconSection.getInt("slot", 14)
                val originalItem = ItemBuilder.buildGuiItemFromConfig(iconSection.getSection("item"))
                val itemStack = originalItem?.itemStack ?: PaperItemBuilder.from(Material.BARRIER)
                    .name(mm.deserialize("config error")).build()
                val iconItem = GuiItem(itemStack) { event ->
                    val player = event.whoClicked as? Player ?: return@GuiItem
                    DialogManager.openIconDialog(player, warpIdentifier)
                }
                gui.setItem(slot, iconItem)
            }

            val deleteSection = buttons.getSection("deleteButton")
            if (deleteSection != null && deleteSection.getBoolean("enabled", true)) {
                val slot = deleteSection.getInt("slot", 31)
                val originalItem = ItemBuilder.buildGuiItemFromConfig(deleteSection.getSection("item"))
                val itemStack = originalItem?.itemStack ?: PaperItemBuilder.from(Material.BARRIER)
                    .name(mm.deserialize("config error")).build()
                val deleteItem = GuiItem(itemStack) { event ->
                    val player = event.whoClicked as? Player ?: return@GuiItem
                    DialogManager.openDeleteDialog(player, warpIdentifier)
                }
                gui.setItem(slot, deleteItem)
            }
        }

        gui.disableAllInteractions()
        gui.setCloseGuiAction { event ->
            val p = event.player as? Player
            refreshTasks.remove(p?.uniqueId)?.cancel()
        }
        gui.open(player)

        if (autoRefresh > 0) {
            refreshTasks.remove(player.uniqueId)?.cancel()

            val task = main.server.scheduler.runTaskTimer(main, Runnable {
                if (!player.isOnline) {
                    refreshTasks.remove(player.uniqueId)?.cancel()
                    return@Runnable
                }

                gui.update()
            }, autoRefresh * 20L, autoRefresh * 20L)

            refreshTasks[player.uniqueId] = task
        }
    }
}