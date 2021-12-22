package com.stripe.android.core.injection

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * A [InjectorRegistry] implemented with a weak map. An entry from the map will be  will be garbage
 * collected once the [Injector] instance is no longer held elsewhere.
 *
 * Note: the weak map will be cleared when app process is killed by system.
 * [Injectable] implementations are responsible for detecting this and call
 * [Injectable.fallbackInitialize] accordingly.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object WeakMapInjectorRegistry : InjectorRegistry {
    /**
     * Cache to map [Injector] to its corresponding [InjectorKey].
     * Note: the [Injector] is the weak map key for itself to be garbage collected.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @VisibleForTesting
    val staticCacheMap = WeakHashMap<Injector, @InjectorKey String>()

    /**
     * Global unique monotonically increasing key to be assigned as a suffixes to
     * registered [Injector]s.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @VisibleForTesting
    val CURRENT_REGISTER_KEY = AtomicInteger(0)

    @Synchronized
    override fun register(injector: Injector, @InjectorKey key: String) {
        synchronized(staticCacheMap.entries) {
            staticCacheMap[injector] = key
            println("thread id: ${Thread.currentThread().id}, registering ${key} staticCacheMap: ${staticCacheMap}")
        }
    }

    @Synchronized
    override fun retrieve(@InjectorKey injectorKey: String): Injector? {
        synchronized(staticCacheMap) {
            println("thread id: ${Thread.currentThread().id}, retrieving ${injectorKey} staticCacheMap: ${staticCacheMap}")
            return staticCacheMap.entries.firstOrNull {
                it.value == injectorKey
            }?.key
        }
    }

    @InjectorKey
    override fun nextKey(prefix: String): String {
        return prefix + CURRENT_REGISTER_KEY.incrementAndGet()
    }

    fun clear() {
        synchronized(staticCacheMap) {
            println("thread id: ${Thread.currentThread().id}, Clearing WeakMapRegistry")
            staticCacheMap.clear()
        }
    }
}
