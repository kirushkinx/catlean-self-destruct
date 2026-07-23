package ru.kirushkinx.selfdestruct.core.teardown

import su.catlean.api.event.GofraState

/** Resets the GofraState flags a module left on. */
object RenderFlags {

    fun reset() {
        runCatching { GofraState.modifyBuffer = false }
        runCatching { GofraState.modifyCollisions = false }
        runCatching { GofraState.stopSwapBuffers = false }
        runCatching { GofraState.xray = false }
    }
}
