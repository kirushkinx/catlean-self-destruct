package ru.kirushkinx.selfdestruct

import ru.kirushkinx.selfdestruct.core.Arming
import ru.kirushkinx.selfdestruct.core.Detonator
import su.catlean.api.addon.feature.AddonCommand
import su.catlean.api.addon.feature.AddonCommandContext

object SelfDestructCommand : AddonCommand("selfdestruct") {

    override fun execute(context: AddonCommandContext): Int {
        if (Detonator.hasFired) return 1

        val window = SelfDestructModule.confirmMs.value.toLong()
        if (!Arming.consume(window)) {
            context.message("self-destruct armed, run again within ${window / 1000}s")
            return 1
        }

        Detonator.detonate("command")
        return 1
    }
}
