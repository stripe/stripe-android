package com.stripe.android.core.injection

import androidx.annotation.RestrictTo
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger

/**
 * Try to use an [InjectorKey] to retrieve an [Injector] and inject, if no [Injector] is found,
 * invoke [Injectable.fallbackInitialize] with [fallbackInitializeParam].
 *
 * @return The [Injector] used to inject the dependencies into this class. Null if the class was
 *      injected by [fallbackInitialize] and the method does not return an [Injector].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <FallbackInitializeParam> Injectable<FallbackInitializeParam>.injectWithFallback(
    @InjectorKey injectorKey: String?,
    fallbackInitializeParam: FallbackInitializeParam
): Injector? {
    val logger = Logger.getInstance(BuildConfig.DEBUG)

    return injectorKey?.let {
        WeakMapInjectorRegistry.retrieve(it)
    }?.let {
        logger.info(
            "Injector available, " +
                "injecting dependencies into ${this::class.java.canonicalName}"
        )
        it.inject(this)
        it
    } ?: run {
        logger.info(
            "Injector unavailable, " +
                "initializing dependencies of ${this::class.java.canonicalName}"
        )
        fallbackInitialize(fallbackInitializeParam)
    }
}
