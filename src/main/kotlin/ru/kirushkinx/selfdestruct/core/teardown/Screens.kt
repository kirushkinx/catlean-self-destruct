package ru.kirushkinx.selfdestruct.core.teardown

import net.minecraft.client.Minecraft
import ru.kirushkinx.selfdestruct.util.Reflect

object Screens {

    /** Closes an open client clickgui to prevent freeze. */
    fun close(mc: Minecraft): Boolean {
        val screen = mc.gui.screen() ?: return false
        if (!Reflect.isClient(screen.javaClass)) return false
        return runCatching { mc.gui.setScreen(null) }.isSuccess
    }
}
