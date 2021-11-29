package com.stripe.android.payments.core.injection

import androidx.annotation.RestrictTo
import com.stripe.android.BuildConfig
import com.stripe.android.core.Logger

/**
 * Try use an [InjectorKey] to retrieve an [Injector] and inject, if no [Injector] is found,
 * invoke [Injectable.fallbackInitialize] with [fallbackInitializeParam].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <FallbackInitializeParam> Injectable<FallbackInitializeParam>.injectWithFallback(
    @InjectorKey injectorKey: String?,
    fallbackInitializeParam: FallbackInitializeParam
) {
    val logger = Logger.getInstance(BuildConfig.DEBUG)

    injectorKey?.let {
        WeakMapInjectorRegistry.retrieve(it)
    }?.let {
        logger.info(
            "Injector available, " +
                "injecting dependencies into ${this::class.java.canonicalName}"
        )
        it.inject(this)
    } ?: run {
        logger.info(
            "Injector unavailable, " +
                "initializing dependencies of ${this::class.java.canonicalName}"
        )
        fallbackInitialize(fallbackInitializeParam)
    }
}
