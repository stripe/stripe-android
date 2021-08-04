package com.stripe.android.payments.core.injection

import androidx.annotation.VisibleForTesting
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * A [InjectorRegistry] implemented with a weak map. An entry from the map will be  will be garbage
 * collected once the [Injector] instance is no longer held elsewhere.
 */
object WeakMapInjectorRegistry : InjectorRegistry {

    @VisibleForTesting
    internal val staticCache = WeakHashMap<Injector, @InjectorKey Int>()

    private var CURRENT_REGISTER_KEY = AtomicInteger(0)

    @Synchronized
    override fun register(injector: Injector, @InjectorKey key: Int) {
        staticCache[injector] = key
    }

    override fun retrieve(@InjectorKey injectorKey: Int): Injector? {
        return staticCache.entries.firstOrNull {
            it.value == injectorKey
        }?.key
    }

    @InjectorKey
    override fun nextKey(): Int {
        return CURRENT_REGISTER_KEY.incrementAndGet()
    }
}
