package com.stripe.android.financialconnections.utils

import androidx.activity.ComponentActivity
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Mavericks
import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.presentation.Async
import kotlinx.coroutines.CancellationException
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Replicates [com.airbnb.mvrx.argsOrNull] for [ComponentActivity].
 */
internal fun <V> argsOrNull() = object : ReadOnlyProperty<ComponentActivity, V?> {
    var value: V? = null
    var read: Boolean = false

    override fun getValue(thisRef: ComponentActivity, property: KProperty<*>): V? {
        if (!read) {
            val args = thisRef.intent
            val argUntyped = args.extras?.get(Mavericks.KEY_ARG)
            @Suppress("UNCHECKED_CAST")
            value = argUntyped as? V
            read = true
        }
        return value
    }
}

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
