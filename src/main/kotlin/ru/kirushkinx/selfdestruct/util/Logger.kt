package ru.kirushkinx.selfdestruct.util

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import ru.kirushkinx.selfdestruct.SelfDestruct
import ru.kirushkinx.selfdestruct.SelfDestructModule

object Logger {

    fun info(message: String) {
        if (SelfDestructModule.silent.value) return
        runCatching { SelfDestruct.addonLogger?.info(message) }
    }

    fun error(message: String, cause: Throwable) {
        if (SelfDestructModule.silent.value) return
        runCatching { SelfDestruct.addonLogger?.error(message, cause) }
    }

    fun chat(text: String) {
        runCatching {
            val player = Minecraft.getInstance().player ?: return
            player.sendSystemMessage(Component.literal(text))
        }
    }
}
