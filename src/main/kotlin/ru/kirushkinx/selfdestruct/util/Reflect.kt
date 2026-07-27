package ru.kirushkinx.selfdestruct.util

import java.lang.reflect.Field
import java.lang.reflect.Modifier

object Reflect {

    const val CLIENT_PACKAGE = "su.catlean"

    fun isClient(type: Class<*>): Boolean = type.name.startsWith(CLIENT_PACKAGE)

    fun load(name: String): Class<*>? =
        runCatching { Class.forName(name, false, Reflect::class.java.classLoader) }.getOrNull()

    fun field(type: Class<*>, name: String): Field? {
        var current: Class<*>? = type
        while (current != null) {
            runCatching { current.getDeclaredField(name).apply { isAccessible = true } }.getOrNull()?.let { return it }
            current = current.superclass
        }
        return null
    }

    fun staticValue(type: Class<*>, name: String): Any? =
        field(type, name)?.let { runCatching { it.get(null) }.getOrNull() }

    fun value(target: Any, name: String): Any? =
        field(target.javaClass, name)?.let { runCatching { it.get(target) }.getOrNull() }

    /** Invokes a public method by name and arg count; pass a Class as target for a static call*/
    fun call(target: Any?, method: String, vararg args: Any?): Any? {
        val type = target as? Class<*> ?: target?.javaClass ?: return null
        val receiver = if (target is Class<*>) null else target
        val handle = type.methods.firstOrNull { it.name == method && it.parameterCount == args.size } ?: return null
        runCatching { handle.isAccessible = true } // package-private loader classes
        return runCatching { handle.invoke(receiver, *args) }.getOrNull()
    }

    /** Instance fields up the hierarchy, accessible. */
    fun fields(type: Class<*>): List<Field> {
        val result = ArrayList<Field>()
        var current: Class<*>? = type
        while (current != null && current != Any::class.java) {
            val declared = runCatching { current.declaredFields }.getOrNull() ?: break
            for (field in declared) {
                if (Modifier.isStatic(field.modifiers)) continue //skip static
                if (runCatching { field.isAccessible = true }.isFailure) continue
                result.add(field)
            }
            current = current.superclass
        }
        return result
    }

    /** Declared field up the hierarchy, left inaccessible. */
    fun raw(type: Class<*>, name: String): Field? {
        var current: Class<*>? = type
        while (current != null) {
            runCatching { current.getDeclaredField(name) }.getOrNull()?.let { return it }
            current = current.superclass
        }
        return null
    }

    /** Reads a field through Unsafe, for JDK-internal types where setAccesible is blocked. */
    fun forcedValue(target: Any?, name: String): Any? {
        val u = unsafe ?: return null
        val type = target as? Class<*> ?: target?.javaClass ?: return null
        val field = raw(type, name) ?: return null
        return runCatching {
            if (Modifier.isStatic(field.modifiers)) {
                val base = unsafeCall(u, "staticFieldBase", field)
                unsafeCall(u, "getObject", base, unsafeCall(u, "staticFieldOffset", field) as Long)
            } else {
                unsafeCall(u, "getObject", target, unsafeCall(u, "objectFieldOffset", field) as Long)
            }
        }.getOrNull()
    }

    private val unsafe: Any? by lazy {
        runCatching {
            val field = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe")
            field.isAccessible = true
            field.get(null)
        }.getOrNull()
    }

    private fun unsafeCall(unsafe: Any, name: String, vararg args: Any?): Any? {
        val method = unsafe.javaClass.methods.firstOrNull { it.name == name && it.parameterCount == args.size } ?: return null
        return method.invoke(unsafe, *args)
    }
}
