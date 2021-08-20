package com.stripe.android.payments.core.injection

import androidx.annotation.VisibleForTesting
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * A [InjectorRegistry] implemented with a weak map. An entry from the map will be  will be garbage
 * collected once the [Injector] instance is no longer held elsewhere.
 */
internal object WeakMapInjectorRegistry : InjectorRegistry {
    /**
     * Cache to map [Injector] to its corresponding [InjectorKey].
     * Note: the [Injector] is the weak map key for itself to be garbage collected.
     */
    @VisibleForTesting
    internal val staticCacheMap = WeakHashMap<Injector, @InjectorKey Int>()

    /**
     * Global unique monotonically increasing key to be assigned to [Injector]s registered.
     */
    private var CURRENT_REGISTER_KEY = AtomicInteger(0)

    override fun register(injector: Injector, @InjectorKey key: Int) {
        staticCacheMap[injector] = key
    }

    override fun retrieve(@InjectorKey injectorKey: Int): Injector? {
        return staticCacheMap.entries.firstOrNull {
            it.value == injectorKey
        }?.key
    }

    @InjectorKey
    override fun nextKey(): Int {
        return CURRENT_REGISTER_KEY.incrementAndGet()
    }
}
