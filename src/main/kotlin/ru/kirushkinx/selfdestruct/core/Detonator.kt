package ru.kirushkinx.selfdestruct.core

import net.minecraft.client.Minecraft
import ru.kirushkinx.selfdestruct.SelfDestruct
import ru.kirushkinx.selfdestruct.SelfDestructModule
import ru.kirushkinx.selfdestruct.core.teardown.Bus
import ru.kirushkinx.selfdestruct.core.teardown.Entrypoints
import ru.kirushkinx.selfdestruct.core.teardown.MixinConfigs
import ru.kirushkinx.selfdestruct.core.teardown.ModList
import ru.kirushkinx.selfdestruct.core.teardown.ModuleState
import ru.kirushkinx.selfdestruct.core.teardown.RenderFlags
import ru.kirushkinx.selfdestruct.core.teardown.Renderer
import ru.kirushkinx.selfdestruct.core.teardown.Resources
import ru.kirushkinx.selfdestruct.core.teardown.Screens
import ru.kirushkinx.selfdestruct.util.Logger
import java.util.concurrent.atomic.AtomicBoolean

/** Step by step teardown. Every step swallows its own errors. */
object Detonator {

    private val fired = AtomicBoolean(false)

    val hasFired: Boolean
        get() = fired.get()

    fun detonate(source: String) {
        if (!SelfDestruct.ready) return
        if (!fired.compareAndSet(false, true)) return

        Arming.disarm()
        Logger.info("triggered by $source")
        onClient { teardown() }
    }

    fun disableToggleSoon() = onClient { ModuleState.disable(SelfDestructModule) }

    private fun teardown() {
        val mc = Minecraft.getInstance()

        if (!SelfDestructModule.silent.value) Logger.chat("self-destruct: client offline until restart")
        Screens.close(mc)
        Logger.info("bus: dropped ${Bus.clear()} buckets")
        MixinConfigs.strip().let { Logger.info("mixins: stripped ${it.configs} configs, ${it.targets} targets") }
        RenderFlags.reset()
        Renderer.refresh(mc)
        ModList.hide()
            .let { Logger.info("mod list: hid ${it.targeted} catlean mods (${it.menu} modmenu, ${it.loader} loader)") }
        Resources.reload(mc)
        Entrypoints.scrub().let { Logger.info("entrypoints: dropped $it containers") }

        Logger.info("teardown complete")
    }

    private fun onClient(block: () -> Unit) = Minecraft.getInstance().execute(block)
}
