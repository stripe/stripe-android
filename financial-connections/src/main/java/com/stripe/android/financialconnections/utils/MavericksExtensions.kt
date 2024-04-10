package com.stripe.android.financialconnections.utils

import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.presentation.Async
import kotlinx.coroutines.CancellationException

/**
 * Prevents [CancellationException] to map to [Fail] when coroutine being cancelled
 * due to search query changes. In these cases, re-map the [Async] instance to [Loading]
 */
internal fun Async<*>.isCancellationError(): Boolean = when {
    this !is Async.Fail -> false
    error is CancellationException -> true
    error is StripeException && error.cause is CancellationException -> true
    else -> false
}

internal val <T> Async<T>.error: Throwable?
    get() = (this as? Async.Fail)?.error
