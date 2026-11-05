package dev.losterixx.sPlayerWarps.other

import org.bukkit.Material
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PWManagerIconTest {

    private val mockIsItem: (Material) -> Boolean = { mat ->
        !mat.name.contains("_WALL_") &&
            !mat.name.startsWith("WALL_") &&
            mat != Material.WATER &&
            mat != Material.LAVA &&
            mat != Material.POWDER_SNOW &&
            mat != Material.AIR &&
            mat != Material.FIRE
    }

    @Test
    fun `converts wall skulls and wall torches to their item counterparts`() {
        assertEquals(
            Material.WITHER_SKELETON_SKULL,
            PWManager.resolveIconMaterial("WITHER_SKELETON_WALL_SKULL", mockIsItem)
        )
        assertEquals(
            Material.TORCH,
            PWManager.resolveIconMaterial("WALL_TORCH", mockIsItem)
        )
        assertEquals(
            Material.WITHER_SKELETON_SKULL,
            PWManager.sanitizeIconMaterial(Material.WITHER_SKELETON_WALL_SKULL, Material.ENDER_PEARL, mockIsItem)
        )
    }

    @Test
    fun `falls back to ender pearl for non-item materials without item equivalent`() {
        assertNull(PWManager.resolveIconMaterial("FIRE", mockIsItem))
        assertEquals(
            Material.ENDER_PEARL,
            PWManager.sanitizeIconMaterial(Material.FIRE, Material.ENDER_PEARL, mockIsItem)
        )
        assertEquals(
            Material.ENDER_PEARL,
            PWManager.sanitizeIconMaterial(null, Material.ENDER_PEARL, mockIsItem)
        )
    }

    @Test
    fun `maps fluid block names to their bucket items`() {
        assertEquals(Material.WATER_BUCKET, PWManager.resolveIconMaterial("water", mockIsItem))
        assertEquals(Material.LAVA_BUCKET, PWManager.resolveIconMaterial("lava", mockIsItem))
    }
}
