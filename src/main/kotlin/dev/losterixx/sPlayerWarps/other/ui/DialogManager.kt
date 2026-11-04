package dev.losterixx.sPlayerWarps.other.ui

import dev.losterixx.sPlayerWarps.Main
import dev.losterixx.sPlayerWarps.other.PWManager
import dev.losterixx.sapi.utils.config.ConfigManager
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.Material
import org.bukkit.entity.Player

object DialogManager {

    private val main = Main.instance
    private val mm = main.miniMessage
    private val config get() = ConfigManager.getConfig("config")
    private val messages get() = ConfigManager.getConfig(config.getString("langFile", "english"))
    
    fun openDisplaynameDialog(player: Player, warpIdentifier: String) {
        val textsList = messages.getStringList("dialogs.displayname.texts", listOf("Enter new displayname:"))
        val plainMessagesList = textsList.map { DialogBody.plainMessage(mm.deserialize(it)) }

        val dialog = Dialog.create { builder ->
            builder.empty()
                .base(DialogBase.builder(mm.deserialize(messages.getString("dialogs.displayname.title", "Change Displayname")))
                    .body(plainMessagesList)
                    .inputs(listOf(
                            DialogInput.text("displayname", Component.empty())
                                .width(250)
                                .labelVisible(true)
                                .maxLength(config.getInt("playerwarps.displayNameMaxLength", 40))
                                .build()
                        )
                    ).build()
                )
                .type(DialogType.confirmation(
                        ActionButton.create(
                            mm.deserialize(messages.getString("dialogs.displayname.buttons.confirm")),
                            Component.empty(),
                            100,
                            DialogAction.customClick(
                                { view, audience ->
                                    if (audience !is Player) return@customClick
                                    val displayname = view.getText("displayname") ?: return@customClick
                                    val warp = PWManager.getPlayerWarp(warpIdentifier) ?: return@customClick

                                    warp.displayName = displayname
                                    audience.closeDialog()
                                    GuiManager.openEditWarpMenu(audience, warpIdentifier)
                                },
                                ClickCallback.Options.builder()
                                    .uses(1)
                                    .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                    .build()
                            )
                        ),
                        ActionButton.create(
                            mm.deserialize(messages.getString("dialogs.displayname.buttons.cancel")),
                            Component.empty(),
                            100,
                            DialogAction.customClick(
                                { view, audience ->
                                    if (audience !is Player) return@customClick
                                    audience.closeDialog()
                                    GuiManager.openEditWarpMenu(audience, warpIdentifier)
                                },
                                ClickCallback.Options.builder()
                                    .uses(1)
                                    .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                    .build()
                            )
                        )
                    )
                )
        }

        player.showDialog(dialog)
    }

    fun openIconDialog(player: Player, warpIdentifier: String) {
        val textsList = messages.getStringList("dialogs.icon.texts", listOf("Write the name of the material you want to set as icon."))
        val plainMessagesList = textsList.map { DialogBody.plainMessage(mm.deserialize(it)) }

        val dialog = Dialog.create { builder ->
            builder.empty()
                .base(DialogBase.builder(mm.deserialize(messages.getString("dialogs.icon.title", "Change Icon")))
                    .body(plainMessagesList)
                    .inputs(listOf(
                        DialogInput.text("material", Component.empty())
                            .width(250)
                            .labelVisible(true)
                            .maxLength(50)
                            .build()
                    )
                    ).build()
                )
                .type(DialogType.confirmation(
                    ActionButton.create(
                        mm.deserialize(messages.getString("dialogs.icon.buttons.confirm")),
                        Component.empty(),
                        100,
                        DialogAction.customClick(
                            { view, audience ->
                                if (audience !is Player) return@customClick
                                val materialName = view.getText("material") ?: return@customClick
                                val warp = PWManager.getPlayerWarp(warpIdentifier) ?: return@customClick

                                val material = PWManager.resolveIconMaterial(materialName)

                                if (material == null) {
                                    audience.sendMessage(mm.deserialize(messages.getString("dialogs.icon.messages.invalidMaterial", "<red>Invalid material name!")))
                                    audience.closeDialog()
                                    return@customClick
                                }

                                warp.material = material
                                PWManager.saveAllToDisk()
                                audience.closeDialog()
                                GuiManager.openEditWarpMenu(audience, warpIdentifier)
                            },
                            ClickCallback.Options.builder()
                                .uses(1)
                                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                .build()
                        )
                    ),
                    ActionButton.create(
                        mm.deserialize(messages.getString("dialogs.icon.buttons.cancel")),
                        Component.empty(),
                        100,
                        DialogAction.customClick(
                            { view, audience ->
                                if (audience !is Player) return@customClick
                                audience.closeDialog()
                                GuiManager.openEditWarpMenu(audience, warpIdentifier)
                            },
                            ClickCallback.Options.builder()
                                .uses(1)
                                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                .build()
                        )
                    )
                )
                )
        }

        player.showDialog(dialog)
    }

    fun openDeleteDialog(player: Player, warpIdentifier: String) {
        val textsList = messages.getStringList("dialogs.delete.texts", listOf("Are you sure?"))
        val plainMessagesList = textsList.map { DialogBody.plainMessage(mm.deserialize(it)) }

        val dialog = Dialog.create { builder ->
            builder.empty()
                .base(DialogBase.builder(mm.deserialize(messages.getString("dialogs.delete.title", "Delete PlayerWarp")))
                    .body(plainMessagesList)
                    .build()
                )
                .type(DialogType.confirmation(
                    ActionButton.create(
                        mm.deserialize(messages.getString("dialogs.delete.buttons.confirm")),
                        Component.empty(),
                        100,
                        DialogAction.customClick(
                            { view, audience ->
                                if (audience !is Player) return@customClick
                                val warp = PWManager.getPlayerWarp(warpIdentifier) ?: return@customClick

                                PWManager.removePlayerWarp(warp)
                                audience.closeDialog()
                                GuiManager.openOwnWarpsMenu(audience)
                            },
                            ClickCallback.Options.builder()
                                .uses(1)
                                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                .build()
                        )
                    ),
                    ActionButton.create(
                        mm.deserialize(messages.getString("dialogs.delete.buttons.cancel")),
                        Component.empty(),
                        100,
                        DialogAction.customClick(
                            { view, audience ->
                                if (audience !is Player) return@customClick
                                audience.closeDialog()
                                GuiManager.openEditWarpMenu(audience, warpIdentifier)
                            },
                            ClickCallback.Options.builder()
                                .uses(1)
                                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                .build()
                        )
                    )
                )
                )
        }

        player.showDialog(dialog)
    }

    fun openCreateDialog(player: Player) {
        val textsList = messages.getStringList("dialogs.create.texts", listOf("Enter the identifier of your new PlayerWarp:"))
        val plainMessagesList = textsList.map { DialogBody.plainMessage(mm.deserialize(it)) }

        val dialog = Dialog.create { builder ->
            builder.empty()
                .base(DialogBase.builder(mm.deserialize(messages.getString("dialogs.create.title", "Create PlayerWarp")))
                    .body(plainMessagesList)
                    .inputs(listOf(
                        DialogInput.text("identifier", Component.empty())
                            .width(250)
                            .labelVisible(true)
                            .maxLength(config.getInt("playerwarps.identifier.maxLength", 16))
                            .build()
                    )
                    ).build()
                )
                .type(DialogType.confirmation(
                    ActionButton.create(
                        mm.deserialize(messages.getString("dialogs.create.buttons.confirm")),
                        Component.empty(),
                        100,
                        DialogAction.customClick(
                            { view, audience ->
                                if (audience !is Player) return@customClick
                                val identifier = view.getText("identifier") ?: return@customClick

                                audience.performCommand("playerwarp create $identifier")

                                audience.closeDialog()

                                if (PWManager.existsPlayerWarp(identifier) && PWManager.getPlayerWarp(identifier)?.owner == audience.uniqueId) {
                                    GuiManager.openEditWarpMenu(audience, identifier)
                                }
                            },
                            ClickCallback.Options.builder()
                                .uses(1)
                                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                .build()
                        )
                    ),
                    ActionButton.create(
                        mm.deserialize(messages.getString("dialogs.create.buttons.cancel")),
                        Component.empty(),
                        100,
                        DialogAction.customClick(
                            { view, audience ->
                                if (audience !is Player) return@customClick
                                audience.closeDialog()
                                GuiManager.openOwnWarpsMenu(audience)
                            },
                            ClickCallback.Options.builder()
                                .uses(1)
                                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                .build()
                        )
                    )
                )
                )
        }

        player.showDialog(dialog)
    }
    
}