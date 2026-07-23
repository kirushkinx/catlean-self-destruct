package ru.kirushkinx.selfdestruct.core.teardown

import ru.kirushkinx.selfdestruct.util.Reflect
import su.catlean.gofra.Gofra

/** Wipes Gofra, the clients' one dispatch table */
object Bus {

    private val NAMES = listOf("branches", "cachedBranches")

    fun clear(): Int {
        var dropped = 0
        for (name in NAMES) {
            // drop the map, not the lists inside
            val map = Reflect.staticValue(Gofra::class.java, name) as? MutableMap<*, *> ?: continue
            dropped += map.size
            runCatching { map.clear() }
        }
        return dropped
    }
}
