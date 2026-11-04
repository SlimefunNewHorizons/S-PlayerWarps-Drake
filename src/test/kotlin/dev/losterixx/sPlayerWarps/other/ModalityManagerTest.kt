package dev.losterixx.sPlayerWarps.other

import dev.dejvokep.boostedyaml.YamlDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModalityManagerTest {
    private fun config(yaml: String = "modalities: {}") =
        YamlDocument.create(yaml.byteInputStream())

    @Test
    fun `each modality sees only its own dimensions`() {
        val settings = config("modalities: {isolate-onechunk: true}")
        val worlds = listOf("world", "oneblock_world", "bskyblock_world", "clasico", "onechunk_world")
        for (source in worlds) {
            for (destination in worlds) {
                assertEquals("$source -> $destination", source == destination,
                    ModalityManager.isSameModality(source, destination, settings))
            }
            assertTrue(ModalityManager.isSameModality(source, source + "_nether", settings))
            assertTrue(ModalityManager.isSameModality(source, source + "_the_end", settings))
        }
    }

    @Test
    fun `onechunk switch preserves the old behaviour until enabled`() {
        assertTrue(ModalityManager.isSameModality("onechunk_world", "world", config()))
        assertFalse(ModalityManager.isSameModality("onechunk_world", "world",
            config("modalities: {isolate-onechunk: true}")))
    }

    @Test
    fun `denied world takes precedence over configured group`() {
        val settings = config("""
            modalities:
              groups:
                survival: [world, laboratorio]
              denied-worlds: [oneblock_world]
        """.trimIndent())
        for (denied in listOf("laboratorio", "oneblock_world", "boss_arena", "nlogin_limbo")) {
            assertEquals("denied", ModalityManager.getModality(denied, settings))
            assertFalse(ModalityManager.isSameModality(denied, "world", settings))
            assertFalse(ModalityManager.isSameModality(denied, denied, settings))
        }
    }

    @Test
    fun `denied prefix takes precedence over default modality inference`() {
        val settings = config("modalities: {denied-worlds: [clasico_nether, onechunk_world]}")
        assertEquals("denied", ModalityManager.getModality("clasico_nether", settings))
        assertEquals("denied", ModalityManager.getModality("onechunk_world", settings))
    }

    @Test
    fun `configured world groups are respected case insensitively`() {
        val settings = config("modalities: {groups: {islands: [custom_island, custom_nether]}}")
        assertEquals("islands", ModalityManager.getModality("CUSTOM_ISLAND", settings))
        assertTrue(ModalityManager.isSameModality("CUSTOM_ISLAND", "custom_nether", settings))
        assertFalse(ModalityManager.isSameModality("custom_island", "world", settings))
    }

    @Test
    fun `unloaded world never becomes a valid destination`() {
        assertFalse(ModalityManager.isSameModality(null, null, config()))
        assertFalse(ModalityManager.isSameModality("world", null, config()))
        assertFalse(ModalityManager.isSameModality(null, "world", config()))
    }
}
