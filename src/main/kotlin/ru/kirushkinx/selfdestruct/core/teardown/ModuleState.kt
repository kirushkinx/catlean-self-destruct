package ru.kirushkinx.selfdestruct.core.teardown

import ru.kirushkinx.selfdestruct.util.Reflect
import su.catlean.api.addon.feature.AddonModule
import su.catlean.gofra.Gofra

object ModuleState {

    fun disable(module: AddonModule): Boolean {
        var off = false
        for (wrapper in wrappersOf(module)) {
            if (turnOff(wrapper)) off = true
            runCatching { Gofra.unplug(wrapper) }
        }
        runCatching { Gofra.unplug(module) } // the click path plugs the raw module too
        return off
    }

    // cachedBranches keys are the subscribers themselves and survive an unplug
    private fun wrappersOf(module: AddonModule): List<Any> {
        val cached = Reflect.staticValue(Gofra::class.java, "cachedBranches") as? Map<*, *> ?: return emptyList()
        return cached.keys.filterNotNull().filter { holds(it, module) }
    }

    private fun holds(holder: Any, module: AddonModule): Boolean {
        if (!Reflect.isClient(holder.javaClass)) return false
        return Reflect.fields(holder.javaClass).any { runCatching { it.get(holder) }.getOrNull() === module }
    }

    private fun turnOff(wrapper: Any): Boolean {
        var changed = lowerBooleans(wrapper)
        for (field in Reflect.fields(wrapper.javaClass)) {
            val nested = runCatching { field.get(wrapper) }.getOrNull() ?: continue
            if (Reflect.isClient(nested.javaClass) && lowerValues(nested)) changed = true
        }
        return changed
    }

    private fun lowerBooleans(owner: Any): Boolean {
        var changed = false
        for (field in Reflect.fields(owner.javaClass)) {
            if (field.type != Boolean::class.javaPrimitiveType && field.type != Boolean::class.javaObjectType) continue
            if (runCatching { field.get(owner) }.getOrNull() != true) continue
            if (runCatching { field.set(owner, false) }.isSuccess) changed = true
        }
        return changed
    }

    private fun lowerValues(setting: Any): Boolean {
        var changed = false
        for (field in Reflect.fields(setting.javaClass)) {
            if (field.type != Any::class.java) continue
            if (runCatching { field.get(setting) }.getOrNull() != true) continue
            if (runCatching { field.set(setting, false) }.isSuccess) changed = true
        }
        return changed
    }
}
