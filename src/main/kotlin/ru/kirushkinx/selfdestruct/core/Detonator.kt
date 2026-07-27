package ru.kirushkinx.selfdestruct.core

import net.minecraft.client.Minecraft
import ru.kirushkinx.selfdestruct.SelfDestruct
import ru.kirushkinx.selfdestruct.SelfDestructModule
import ru.kirushkinx.selfdestruct.core.teardown.*
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
        JarRelease.collect() // needs the loader entries ModList.hide drops
        ModList.hide()
            .let { Logger.info("mod list: hid ${it.targeted} catlean mods (${it.menu} modmenu, ${it.loader} loader)") }
        Resources.reload(mc)
        Entrypoints.scrub().let { Logger.info("entrypoints: dropped $it containers") }
        Textures.release(mc).let { Logger.info("textures: released $it") }
        ChatLog.scrub(mc).let { Logger.info("chat: scrubbed $it lines") }
        Threads.stop().let { Logger.info("threads: interrupted $it") }
        JarRelease.release().let { Logger.info("jars: released $it handles") }

        Logger.info("teardown complete")
        Log.scrub().let { Logger.info("log: scrubbed $it lines") }
        SelfDestruct.release()
    }

    private fun onClient(block: () -> Unit) = Minecraft.getInstance().execute(block)
}
