package ru.kirushkinx.selfdestruct.core.teardown

import ru.kirushkinx.selfdestruct.util.Constants.LOADER
import ru.kirushkinx.selfdestruct.util.Mods
import ru.kirushkinx.selfdestruct.util.Reflect

/** Drops catlean and its addon from ModMenu's maps and the loader's list. */
object ModList {

    private const val MODMENU = "com.terraformersmc.modmenu.ModMenu"

    data class Report(val targeted: Int, val menu: Int, val loader: Int)

    fun hide(): Report {
        val ids = Mods.catleanIds()
        return Report(ids.size, fromModMenu(ids), fromLoader(ids))
    }

    // ModMenu builds these once and reads them on every open
    private fun fromModMenu(ids: Set<String>): Int {
        val type = Reflect.load(MODMENU) ?: return 0
        var removed = 0
        for (name in listOf("MODS", "ROOT_MODS")) {
            val mods = Reflect.staticValue(type, name) as? MutableMap<*, *> ?: continue
            for (id in ids) if (mods.remove(id) != null) removed++
        }
        @Suppress("UNCHECKED_CAST")
        val factories = Reflect.staticValue(type, "configScreenFactories") as? MutableMap<Any?, Any?>
        for (id in ids) if (runCatching { factories?.remove(id) }.getOrNull() != null) removed++
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
