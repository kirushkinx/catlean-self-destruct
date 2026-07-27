package ru.kirushkinx.selfdestruct

import ru.kirushkinx.selfdestruct.core.Arming
import ru.kirushkinx.selfdestruct.core.Detonator
import ru.kirushkinx.selfdestruct.util.Logger
import su.catlean.api.addon.feature.AddonModule

object SelfDestructModule : AddonModule(SelfDestruct.ID, "misc", listOf("sd", "kill", "killswitch", "panic")) {

    enum class Trigger {
        INSTANT, CONFIRM,
    }

    val trigger = setting("trigger", Trigger.INSTANT)
    val confirmMs = setting("confirm-ms", 10000, 1000..60000)
    val silent = setting("silent", true)

    override fun onEnable() {
        try {
            if (!SelfDestruct.ready || Detonator.hasFired) return

            if (trigger.value == Trigger.CONFIRM && !Arming.consume(confirmMs.value.toLong())) {
                Logger.chat("self-destruct armed, toggle again within ${confirmMs.value / 1000}s")
                return
            }

            Detonator.detonate("module")
        } finally {
            Detonator.disableToggleSoon() // never leave the toggle lit
        }
    }
}
