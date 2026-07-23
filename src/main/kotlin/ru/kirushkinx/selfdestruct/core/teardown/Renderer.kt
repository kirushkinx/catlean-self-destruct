package ru.kirushkinx.selfdestruct.core.teardown

import net.minecraft.client.Minecraft

object Renderer {

    fun refresh(mc: Minecraft): Boolean {
        if (mc.level == null) return false
        return runCatching { mc.levelExtractor.allChanged() }.isSuccess // chunk reload same as f3+a
    }
}
