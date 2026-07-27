package ru.kirushkinx.selfdestruct.core.teardown

import net.fabricmc.loader.api.FabricLoader
import ru.kirushkinx.selfdestruct.util.Mods
import ru.kirushkinx.selfdestruct.util.Reflect
import java.io.File
import java.net.URL
import java.nio.file.FileSystem
import java.nio.file.Path
import java.util.jar.JarFile

/** Frees the jars of catlean and its addons so they can be deleted from mods/ bypassing Windows lockout */
object JarRelease {

    private const val JAR_FACTORY = "sun.net.www.protocol.jar.JarFileFactory"

    private val windows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)

    private val files = LinkedHashSet<File>()
    private val fileSystems = LinkedHashSet<FileSystem>()

    /** Reads the loader before ModList.hide drops the containers. */
    fun collect() {
        if (!windows) return // windows only
        runCatching {
            val loader = FabricLoader.getInstance()
            for (id in Mods.catleanIds()) {
                val container = loader.getModContainer(id).orElse(null) ?: continue
                for (path in paths(container, "getRootPaths")) {
                    runCatching { path.fileSystem }.getOrNull()
                        ?.takeIf { it.provider().scheme == "jar" }
                        ?.let { fileSystems.add(it) }
                }
                for (path in paths(Reflect.call(container, "getOrigin"), "getPaths")) {
                    runCatching { files.add(path.toFile().canonicalFile) }
                }
            }
        }
    }

    fun release(): Int {
        if (!windows) return 0
        preload()
        var freed = 0
        for (file in files) freed += dropFromLoaders(file)
        for (fs in fileSystems) runCatching { fs.close() }
        freed += purgeUrlCache()
        if (files.isNotEmpty()) {
            runCatching { System.gc() } // windows frees the native zip handle via the cleaner
            runCatching { Thread.sleep(50L) }
        }
        return freed
    }

    /** Links every class of the target jars, meaning no lazy load ever needs thwe files again */
    private fun preload() {
        val loader = JarRelease::class.java.classLoader
        for (file in files) {
            if (file.isDirectory) {
                file.walkTopDown().filter { it.extension == "class" }.forEach { entry ->
                    load(entry.relativeTo(file).path.removeSuffix(".class").replace(File.separatorChar, '.'), loader)
                }
                continue
            }
            runCatching {
                JarFile(file).use { jar ->
                    for (entry in jar.entries()) {
                        if (entry.name.endsWith(".class")) load(entry.name.removeSuffix(".class").replace('/', '.'), loader)
                    }
                }
            }
        }
    }

    private fun load(name: String, loader: ClassLoader) {
        runCatching { Class.forName(name, false, loader) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun paths(target: Any?, method: String): List<Path> =
        (Reflect.call(target, method) as? List<Path>).orEmpty()

    private fun dropFromLoaders(file: File): Int {
        var freed = 0
        var cl = JarRelease::class.java.classLoader
        while (cl != null) {
            freed += dropFrom(cl, file)
            cl = cl.parent
        }
        return freed
    }

    // knot wraps the URLClassLoader
    private fun dropFrom(cl: ClassLoader, file: File): Int {
        val owner = Reflect.value(cl, "urlLoader") ?: cl
        val ucp = Reflect.forcedValue(owner, "ucp") ?: return 0
        val loaders = Reflect.forcedValue(ucp, "loaders") as? MutableList<*> ?: return 0
        val lmap = Reflect.forcedValue(ucp, "lmap") as? MutableMap<*, *>
        val path = Reflect.forcedValue(ucp, "path") as? MutableList<*>
        var freed = 0
        synchronized(loaders) {
            val iterator = loaders.iterator()
            while (iterator.hasNext()) {
                val cpLoader = iterator.next() ?: continue
                val jar = Reflect.forcedValue(cpLoader, "jar") as? JarFile ?: continue
                if (!sameFile(jar, file)) continue
                iterator.remove()
                runCatching { lmap?.entries?.removeIf { it.value === cpLoader } }
                runCatching { path?.removeIf { serves(it, file) } } // a leftover url would reopen the jar
                runCatching { jar.close() }
                freed++
            }
        }
        return freed
    }

    // global jar url cache
    private fun purgeUrlCache(): Int {
        val type = Reflect.load(JAR_FACTORY) ?: return 0
        val cache = Reflect.forcedValue(type, "fileCache") as? MutableMap<*, *> ?: return 0
        var freed = 0
        synchronized(cache) {
            runCatching {
                (Reflect.forcedValue(type, "urlCache") as? MutableMap<*, *>)
                    ?.keys?.removeIf { it is JarFile && held(it) }
            }
            val iterator = cache.entries.iterator()
            while (iterator.hasNext()) {
                val jar = iterator.next().value as? JarFile ?: continue
                if (!held(jar)) continue
                iterator.remove()
                runCatching { jar.close() }
                freed++
            }
        }
        return freed
    }

    private fun held(jar: JarFile): Boolean = files.any { sameFile(jar, it) }

    private fun sameFile(jar: JarFile, target: File): Boolean =
        runCatching { File(jar.name).canonicalFile == target }.getOrDefault(false)

    private fun serves(url: Any?, target: File): Boolean =
        url is URL && runCatching { File(url.toURI()).canonicalFile == target }.getOrDefault(false)
}
