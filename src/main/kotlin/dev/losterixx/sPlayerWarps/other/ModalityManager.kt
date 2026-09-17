package dev.losterixx.sPlayerWarps.other

import dev.losterixx.sapi.utils.config.ConfigManager

object ModalityManager {

    const val SURVIVAL = "survival"
    const val ONEBLOCK = "oneblock"
    const val SKYBLOCK = "skyblock"
    const val CLASICO = "clasico"

    private val DEFAULT_MODALITIES = mapOf(
        SURVIVAL to listOf(
            "world", "overworld", "world_nether", "the_nether", "world_the_end", "the_end",
            "spawnwarps", "SpawnWarps", "dimensionalhome", "galactifun_space",
            "world_galactifun_earth_orbit", "world_galactifun_enceladus", "world_galactifun_io",
            "world_galactifun_mars", "world_galactifun_moon", "world_galactifun_the_moon",
            "world_galactifun_titan", "world_galactifun_venus"
        ),
        ONEBLOCK to listOf(
            "oneblock_world", "oneblock_world_nether", "oneblock_world_the_end"
        ),
        SKYBLOCK to listOf(
            "bskyblock_world", "bskyblock_world_nether", "bskyblock_world_the_end"
        ),
        CLASICO to listOf(
            "clasico", "clasico_nether", "clasico_the_end"
        )
    )

    private val DEFAULT_DENIED = setOf(
        "boss_arena", "boss_dimension", "drakes_bosses", "nlogin_limbo", "laboratorio"
    )

    fun getModality(worldName: String?): String {
        if (worldName == null) return "unknown"
        val w = worldName.lowercase()

        val config = ConfigManager.getConfig("config")
        val groupsSection = config?.getSection("modalities.groups")
        if (groupsSection != null) {
            for (key in groupsSection.getRoutesAsStrings(false)) {
                val worlds = groupsSection.getStringList(key, listOf())
                if (worlds.any { it.equals(w, ignoreCase = true) }) {
                    return key.lowercase()
                }
            }
        }

        for ((modality, worlds) in DEFAULT_MODALITIES) {
            if (worlds.any { it.equals(w, ignoreCase = true) }) {
                return modality
            }
        }

        return when {
            w.startsWith("oneblock") -> ONEBLOCK
            w.startsWith("bskyblock") -> SKYBLOCK
            w.startsWith("clasico") -> CLASICO
            isDenied(worldName) -> "denied"
            else -> SURVIVAL
        }
    }

    fun isDenied(worldName: String?): Boolean {
        if (worldName == null) return true
        val w = worldName.lowercase()
        val config = ConfigManager.getConfig("config")
        val deniedList = config?.getStringList("modalities.denied-worlds", listOf())
        if (deniedList != null && deniedList.isNotEmpty()) {
            if (deniedList.any { it.equals(w, ignoreCase = true) }) return true
        }
        return w in DEFAULT_DENIED
    }

    fun isSameModality(world1: String?, world2: String?): Boolean {
        val m1 = getModality(world1)
        val m2 = getModality(world2)
        if (m1 == "denied" || m2 == "denied" || m1 == "unknown" || m2 == "unknown") return false
        return m1 == m2
    }
}
