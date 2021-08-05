package com.stripe.android.payments.core.injection

import androidx.annotation.VisibleForTesting
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * A [InjectorRegistry] implemented with a weak map. An entry from the map will be  will be garbage
 * collected once the [Injector] instance is no longer held elsewhere.
 */
internal object WeakSetInjectorRegistry : InjectorRegistry {

    @VisibleForTesting
    internal val staticCacheSet = Collections.newSetFromMap<Injector>(WeakHashMap())

    private var CURRENT_REGISTER_KEY = AtomicInteger(0)

    override fun register(injector: Injector, @InjectorKey key: Int) {
        staticCacheSet.add(injector)
        injector.injectorKey = key
    }

    override fun retrieve(@InjectorKey injectorKey: Int): Injector? {
        return staticCacheSet.firstOrNull {
            it.injectorKey == injectorKey
        }
    }

    @InjectorKey
    override fun nextKey(): Int {
        return CURRENT_REGISTER_KEY.incrementAndGet()
    }
}
