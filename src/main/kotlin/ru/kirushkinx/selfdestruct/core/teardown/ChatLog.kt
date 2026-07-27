package ru.kirushkinx.selfdestruct.core.teardown

import net.minecraft.client.Minecraft
import ru.kirushkinx.selfdestruct.util.Reflect

/** Removes our own lines from the in-memory chat history. */
object ChatLog {

    private const val MARKER = "self-destruct"

    fun scrub(mc: Minecraft): Int {
        val chat = mc.gui.hud.chat
        var removed = 0
        removed += drop(Reflect.value(chat, "allMessages") as? MutableCollection<*>) { mentions(it) }
        removed += drop(Reflect.value(chat, "trimmedMessages") as? MutableCollection<*>) { line ->
            mentions(Reflect.call(line, "parent"))
        }
        return removed
    }

    private fun mentions(message: Any?): Boolean {
        val content = Reflect.call(message, "content") ?: return false
        val text = Reflect.call(content, "getString") as? String ?: return false
        return text.contains(MARKER, ignoreCase = true)
    }

    private fun drop(list: MutableCollection<*>?, predicate: (Any?) -> Boolean): Int {
        list ?: return 0
        val before = list.size
        runCatching { list.removeIf { predicate(it) } }
        return before - list.size
    }
}
