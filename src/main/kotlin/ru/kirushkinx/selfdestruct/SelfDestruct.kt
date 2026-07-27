package ru.kirushkinx.selfdestruct

import ru.kirushkinx.selfdestruct.util.Constants.SELF_DESTRUCT
import ru.kirushkinx.selfdestruct.util.Logger
import su.catlean.api.addon.CatLeanAddon
import su.catlean.api.addon.CatLeanAddonLogger
import su.catlean.api.addon.CatLeanApi

object SelfDestruct : CatLeanAddon {

    const val ID = SELF_DESTRUCT

    @Volatile
    var api: CatLeanApi? = null
        private set

    @Volatile
    var addonLogger: CatLeanAddonLogger? = null
        private set

    @Volatile
    var ready = false
        private set

    override fun onCatLeanAddon(api: CatLeanApi) {
        this.api = api
        this.addonLogger = api.logger(ID)

        api.registry.registerModule(SelfDestructModule)
        api.registry.registerCommand(SelfDestructCommand)

        ready = true
        Logger.info("self-destruct initialised")
    }

    fun release() {
        ready = false
        api = null
        addonLogger = null
    }
}
