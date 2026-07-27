package ru.kirushkinx.selfdestruct.core.teardown

import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import ru.kirushkinx.selfdestruct.util.Reflect

/** Releases the client's textures and modmenu icons held by the texture manager. */
object Textures {

    private const val MODMENU = "modmenu"
    private const val ICON_SUFFIX = "_icon"

    fun release(mc: Minecraft): Int {
        val manager = mc.textureManager
        val byPath = Reflect.value(manager, "byPath") as? Map<*, *> ?: return 0
        val ids = ModList.catleanIds()
        val hits = byPath.keys.filterIsInstance<Identifier>().filter { matches(it, ids) }
        var released = 0
        for (id in hits) runCatching { manager.release(id) }.onSuccess { released++ }
        return released
    }

    private fun matches(key: Identifier, ids: Set<String>): Boolean {
        if (key.namespace in ids) return true
        return key.namespace == MODMENU && key.path.endsWith(ICON_SUFFIX) &&
            key.path.removeSuffix(ICON_SUFFIX) in ids
    }
}
