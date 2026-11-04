package dev.losterixx.sPlayerWarps.other

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.UUID

data class PlayerWarp(
    val identifier: String,
    val owner: UUID,
    var location: Location,
    var displayName: String? = null,
    var material: Material = Material.ENDER_PEARL,
    var totalUses: Int = 0,
    var uniqueUses: MutableList<UUID> = mutableListOf(),
    var modality: String = ModalityManager.getModality(location.world?.name)
) {
    fun teleport(player: Player) {
        player.teleport(location)
        totalUses += 1
        if (!uniqueUses.contains(player.uniqueId)) {
            uniqueUses.add(player.uniqueId)
        }
    }
}
