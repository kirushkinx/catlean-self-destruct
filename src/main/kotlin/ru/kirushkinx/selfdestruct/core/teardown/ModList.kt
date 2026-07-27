package ru.kirushkinx.selfdestruct.core.teardown

import net.fabricmc.loader.api.FabricLoader
import ru.kirushkinx.selfdestruct.util.Reflect
import su.catlean.api.addon.CatLeanAddon

/** Drops catlean and its addon from ModMenu's maps and the loader's list. */
object ModList {

    private const val MODMENU = "com.terraformersmc.modmenu.ModMenu"
    private const val LOADER = "net.fabricmc.loader.impl.FabricLoaderImpl"
    private const val CLIENT = "catlean"
    private const val ADDON_ENTRYPOINT = "catlean:addon"

    data class Report(val targeted: Int, val menu: Int, val loader: Int)

    fun hide(): Report {
        val ids = catleanIds()
        return Report(ids.size, fromModMenu(ids), fromLoader(ids))
    }

    // the client plus every mod that registers a catlean:addon entrypoint
    fun catleanIds(): Set<String> {
        val ids = linkedSetOf(CLIENT)
        runCatching {
            FabricLoader.getInstance()
                .getEntrypointContainers(ADDON_ENTRYPOINT, CatLeanAddon::class.java)
                .forEach { ids.add(it.provider.metadata.id) }
        }
        return ids
    }

    // ModMenu builds these once and reads them on every open
    private fun fromModMenu(ids: Set<String>): Int {
        val type = Reflect.load(MODMENU) ?: return 0
        var removed = 0
        for (name in listOf("MODS", "ROOT_MODS")) {
            val mods = Reflect.staticValue(type, name) as? MutableMap<*, *> ?: continue
            for (id in ids) if (mods.remove(id) != null) removed++
        }
        unparent(type, ids)
        Reflect.call(type, "clearModCountCache")
        return removed
    }

    private fun unparent(type: Class<*>, ids: Set<String>) {
        val parents = Reflect.staticValue(type, "PARENT_MAP") ?: return
        val keys = (Reflect.call(parents, "keySet") as? Set<*>)?.toList().orEmpty()
        for (key in keys) if (idOf(key) in ids) Reflect.call(parents, "removeAll", key)
        (Reflect.call(parents, "values") as? MutableCollection<*>)?.removeIf { idOf(it) in ids }
    }

    private fun fromLoader(ids: Set<String>): Int {
        val type = Reflect.load(LOADER) ?: return 0
        val loader = Reflect.staticValue(type, "INSTANCE") ?: return 0
        var removed = 0
        (Reflect.value(loader, "modMap") as? MutableMap<*, *>)?.let { map ->
            for (id in ids) if (map.remove(id) != null) removed++
        }
        (Reflect.value(loader, "mods") as? MutableList<*>)?.removeIf { idOf(Reflect.call(it, "getMetadata")) in ids }
        return removed
    }

    private fun idOf(target: Any?): String? = Reflect.call(target, "getId") as? String
}
