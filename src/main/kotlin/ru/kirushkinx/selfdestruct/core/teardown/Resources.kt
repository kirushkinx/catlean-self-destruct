package ru.kirushkinx.selfdestruct.core.teardown

import net.minecraft.client.Minecraft

object Resources {

     /** Rebuilds the resource manager through vanilla, dropping the hidden catlean packs and their namespace. */
    fun reload(mc: Minecraft): Boolean =
        runCatching { mc.reloadResourcePacks() }.isSuccess
}
