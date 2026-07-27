package ru.kirushkinx.selfdestruct.util

import net.fabricmc.loader.api.FabricLoader
import su.catlean.api.addon.CatLeanAddon

object Mods {

    private const val ADDON_ENTRYPOINT = "catlean:addon"

    // the client plus every mod that registers a catlean:addon entrypoint
    fun catleanIds(): Set<String> {
        val ids = linkedSetOf(Constants.CATLEAN)
        runCatching {
            FabricLoader.getInstance()
                .getEntrypointContainers(ADDON_ENTRYPOINT, CatLeanAddon::class.java)
                .forEach { ids.add(it.provider.metadata.id) }
        }
        return ids
    }
}
