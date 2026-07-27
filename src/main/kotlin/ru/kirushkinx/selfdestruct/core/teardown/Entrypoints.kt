package ru.kirushkinx.selfdestruct.core.teardown

import ru.kirushkinx.selfdestruct.util.Constants.LOADER
import ru.kirushkinx.selfdestruct.util.Mods
import ru.kirushkinx.selfdestruct.util.Reflect

/** Drops the loader's cached entrypoint containers, which keep the client's initializers and this addon alive. */
object Entrypoints {

    fun scrub(): Int {
        val type = Reflect.load(LOADER) ?: return 0
        val loader = Reflect.staticValue(type, "INSTANCE") ?: return 0
        val storage = Reflect.value(loader, "entrypointStorage") ?: return 0
        val map = Reflect.value(storage, "entryMap") as? MutableMap<*, *> ?: return 0
        val ids = Mods.catleanIds()
        var removed = 0
        for (entries in map.values.toList()) {
            val list = entries as? MutableList<*> ?: continue
            val before = list.size
            runCatching { list.removeIf { it == null || modIdOf(it) in ids } }
            removed += before - list.size
        }
        return removed
    }

    private fun modIdOf(entry: Any): String? =
        Reflect.call(Reflect.call(Reflect.call(entry, "getModContainer"), "getMetadata"), "getId") as? String
}
