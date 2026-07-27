package ru.kirushkinx.selfdestruct.core.teardown

import ru.kirushkinx.selfdestruct.util.Constants.CATLEAN
import ru.kirushkinx.selfdestruct.util.Reflect

/** Empties the client's mixin configs */
object MixinConfigs {

    private const val CONFIG = "org.spongepowered.asm.mixin.transformer.Config"
    private const val MIXINS = "org.spongepowered.asm.mixin.Mixins"

    // mixinMapping is the live lookup MixinProcessor reads per class load
    private val DATA = listOf(
        "mixinMapping", "mixins", "pendingMixins", "unhandledTargets",
        "mixinClasses", "mixinClassesClient", "mixinClassesServer",
    )

    class Report {
        var configs = 0
        var targets = 0
    }

    fun strip(): Report {
        val report = Report()
        for (handle in handles()) {
            val config = Reflect.call(handle, "getConfig") ?: continue
            if (!isClient(handle, config)) continue

            report.configs++
            report.targets += targetCount(config)
            drain(config)
            pending()?.let { runCatching { it.remove(handle) } }
        }
        return report
    }

    // allConfigs is a processor that drains getConfigs the moment it selects a config
    private fun handles(): List<Any> {
        val type = Reflect.load(CONFIG) ?: return emptyList()
        val all = Reflect.staticValue(type, "allConfigs") as? Map<*, *> ?: return emptyList()
        return all.values.filterNotNull()
    }

    @Suppress("UNCHECKED_CAST")
    private fun pending(): MutableSet<Any>? =
        Reflect.load(MIXINS)?.let { Reflect.call(it, "getConfigs") } as? MutableSet<Any>

    private fun isClient(handle: Any, config: Any): Boolean =
        string(handle, "getName").contains(CATLEAN, ignoreCase = true) ||
            string(config, "getMixinPackage").startsWith(Reflect.CLIENT_PACKAGE)

    private fun targetCount(config: Any): Int =
        (Reflect.call(config, "getTargets") as? Set<*>)?.size ?: 0

    private fun drain(config: Any) {
        for (name in DATA) {
            when (val value = Reflect.value(config, name)) {
                is Map<*, *> -> runCatching { (value as MutableMap<*, *>).clear() }
                is Collection<*> -> runCatching { (value as MutableCollection<*>).clear() }
            }
        }
    }

    private fun string(target: Any, method: String): String =
        (Reflect.call(target, method) as? String).orEmpty()
}
