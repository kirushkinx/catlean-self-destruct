package ru.kirushkinx.selfdestruct.core.teardown

import ru.kirushkinx.selfdestruct.util.Constants.CATLEAN
import ru.kirushkinx.selfdestruct.util.Reflect

/** Interrupts background threads the client spawned. */
object Threads {

    fun stop(): Int {
        val current = Thread.currentThread()
        var hit = 0
        for ((thread, stack) in runCatching { Thread.getAllStackTraces() }.getOrDefault(emptyMap())) {
            if (thread === current || !thread.isAlive) continue
            if (!isClient(thread, stack)) continue
            runCatching { thread.interrupt() }.onSuccess { hit++ }
        }
        return hit
    }

    private fun isClient(thread: Thread, stack: Array<StackTraceElement>): Boolean {
        if (thread.name.contains(CATLEAN, ignoreCase = true)) return true
        val task = Reflect.forcedValue(thread, "holder")?.let { Reflect.forcedValue(it, "task") }
        if (task != null && Reflect.isClient(task.javaClass)) return true
        return stack.any { it.className.startsWith(Reflect.CLIENT_PACKAGE) }
    }
}
