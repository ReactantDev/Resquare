package dev.reactant.resquare.utils

import java.util.concurrent.ConcurrentHashMap

private val cache = ConcurrentHashMap<Class<*>, List<Class<*>>>()

internal fun getAllExtendedClass(clazz: Class<*>): List<Class<*>> = cache.getOrPut(clazz) {
    val extended = ((clazz.superclass?.let { arrayOf(it) } ?: arrayOf()) + clazz.interfaces)
    (arrayOf(clazz) + extended.flatMap { getAllExtendedClass(it) }).toList()
}
