## CatLean Self-Destruct
Kill switch addon for the [CatLean](https://catlean.su/)

### About
On trigger the client stops existing inside the running game: nothing ticks, nothing renders, no key is read, no packet is touched. Only a game restart brings it back.

### Usage
Category `Utility` -> `Misc` -> `Self Destruct`  or the `^selfdestruct` command.

| Setting      | Default   | Effect                                          |
|--------------|-----------|-------------------------------------------------|
| `Trigger`    | `Instant` | `Confirm` makes the first toggle arm only       |
| `Confirm Ms` | `10000`   | confirmation window for `Confirm`               |
| `Silent`     | `true`    | leave nothing behind - no chat, no console line |

### Teardown
Each step swallows its own errors - a failure in one can't stop the rest.

- [Screens](src/main/kotlin/ru/kirushkinx/selfdestruct/core/teardown/Screens.kt) - closes the catlean clickgui before the sweep, to keep it from freezing
- [Bus](src/main/kotlin/ru/kirushkinx/selfdestruct/core/teardown/Bus.kt) - drains Gofra, the client's one dispatch table - no `@Flow` method runs again
- [MixinConfigs](src/main/kotlin/ru/kirushkinx/selfdestruct/core/teardown/MixinConfigs.kt) - empties the client's mixin configs
- [RenderFlags](src/main/kotlin/ru/kirushkinx/selfdestruct/core/teardown/RenderFlags.kt) - resets the render state flags a module left on
- [Renderer](src/main/kotlin/ru/kirushkinx/selfdestruct/core/teardown/Renderer.kt) - reloads the chunks, same as F3+A
- [ModList](src/main/kotlin/ru/kirushkinx/selfdestruct/core/teardown/ModList.kt) - drops catlean and its addons from modmenu and the loader
- [Resources](src/main/kotlin/ru/kirushkinx/selfdestruct/core/teardown/Resources.kt) - reloads resource packs, which drops the catlean packs out
- [Entrypoints](src/main/kotlin/ru/kirushkinx/selfdestruct/core/teardown/Entrypoints.kt) - clears the loader entrypoint cache that keeps the client's initializers and this addon alive
- [Textures](src/main/kotlin/ru/kirushkinx/selfdestruct/core/teardown/Textures.kt) - releases the catlean textures and the modmenu icons from the texture manager
- [ChatLog](src/main/kotlin/ru/kirushkinx/selfdestruct/core/teardown/ChatLog.kt) - removes our own lines from chat history
- [Threads](src/main/kotlin/ru/kirushkinx/selfdestruct/core/teardown/Threads.kt) - interrupts background threads the client spawned
- [JarRelease](src/main/kotlin/ru/kirushkinx/selfdestruct/core/teardown/JarRelease.kt) - frees catlean and addon jar handles (Windows lock), letting you delete the files from `mods/` without a restart
- [Log](src/main/kotlin/ru/kirushkinx/selfdestruct/core/teardown/Log.kt) - scrubs catlean lines from the live game log

p.s. A teardown can't unload the client classes or un-weave the mixins, so the restore path stays a game restart, but the cleanup above is about as close to complete as a running client allows - everything catlean left in memory and on disk gets scrubbed.


