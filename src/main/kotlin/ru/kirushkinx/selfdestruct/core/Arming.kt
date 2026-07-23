package ru.kirushkinx.selfdestruct.core

/** Shared arm state, therefore the module/command can arm one and confirm the other. */
object Arming {

    @Volatile
    private var armedAt = 0L

    @Synchronized
    fun consume(windowMs: Long): Boolean {
        val now = System.currentTimeMillis()
        if (armedAt != 0L && now - armedAt <= windowMs) {
            armedAt = 0L
            return true
        }

        armedAt = now
        return false
    }

    @Synchronized
    fun disarm() {
        armedAt = 0L
    }
}
