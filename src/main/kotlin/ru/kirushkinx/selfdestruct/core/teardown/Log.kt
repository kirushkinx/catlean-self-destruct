package ru.kirushkinx.selfdestruct.core.teardown

import net.minecraft.client.Minecraft
import ru.kirushkinx.selfdestruct.util.Reflect
import java.nio.file.Files
import java.nio.file.Path

/** Removes catlean lines from the live game log so the on-disk file carries no trace of the client. */
object Log {

    private val MARKERS = listOf("catlean", "self-destruct")
    private const val LATEST = "logs/latest.log"
    private const val FILE_MANAGER = "org.apache.logging.log4j.core.appender.RandomAccessFileManager"

    fun scrub(): Int {
        val removed = doScrub()
        // post-teardown vanilla logs (sound warnings, etc.) land a few frames later
        val mc = runCatching { net.minecraft.client.Minecraft.getInstance() }.getOrNull() ?: return removed
        Thread {
            Thread.sleep(1500)
            mc.execute { runCatching { doScrub() } }
        }.apply { isDaemon = true }.start()
        return removed
    }

    private fun doScrub(): Int {
        val path = latestLog() ?: return 0
        val manager = fileManager()
        if (manager == null) return writeFiltered(path) // no live appender reachable -> best effort
        return synchronized(manager) {
            runCatching { Reflect.call(manager, "flush") }
            val raf = Reflect.value(manager, "randomAccessFile") ?: return@synchronized writeFiltered(path)
            rewrite(raf, path)
        }
    }

    private fun latestLog(): Path? =
        runCatching { Minecraft.getInstance().gameDirectory.toPath().resolve(LATEST) }
            .getOrNull()?.takeIf { Files.isRegularFile(it) }

    // the random access file manager is shared per jvm
    private fun fileManager(): Any? {
        val type = Reflect.load(FILE_MANAGER) ?: return null
        val ctx = runCatching { Reflect.call(Reflect.load("org.apache.logging.log4j.LogManager"), "getContext", false) }.getOrNull() ?: return null
        val config = Reflect.call(ctx, "getConfiguration") ?: return null
        val appenders = Reflect.call(config, "getAppenders") as? Map<*, *> ?: return null
        for (appender in appenders.values) {
            val manager = Reflect.call(appender, "getManager") ?: continue
            if (manager.javaClass != type) continue
            return manager
        }
        return null
    }

    private fun rewrite(raf: Any, path: Path): Int {
        val before = readLines(path)
        val kept = before.filterNot { line -> MARKERS.any { it in line } }
        val out = kept.joinToString("\n") + "\n"
        runCatching {
            Reflect.call(raf, "setLength", 0L)
            Reflect.call(raf, "seek", 0L)
            raf.javaClass.getMethod("write", ByteArray::class.java).invoke(raf, out.toByteArray(Charsets.UTF_8))
        }
        return before.size - kept.size
    }

    private fun writeFiltered(path: Path): Int {
        val before = readLines(path)
        val kept = before.filterNot { line -> MARKERS.any { it in line } }
        val out = kept.joinToString("\n") + "\n"
        runCatching { Files.write(path, out.toByteArray(Charsets.UTF_8)) }
        return before.size - kept.size
    }

    private fun readLines(path: Path): List<String> =
        runCatching { Files.readString(path).split("\n") }.getOrDefault(emptyList())
}