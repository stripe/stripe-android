package com.stripe.android.financialconnections.utils

import androidx.activity.ComponentActivity
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.InternalMavericksApi
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.MavericksViewModelProvider
import com.stripe.android.core.exception.StripeException
import kotlinx.coroutines.CancellationException
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Replicates [com.airbnb.mvrx.viewModel] delegate, but without using [com.airbnb.mvrx.lifecycleAwareLazy]
 * to eagerly initialize the ViewModel in [ComponentActivity.onCreate].
 *
 * Some [MavericksViewModelFactory] implementations assume args are correct to instantiate
 * the viewModel, and the associated Dagger graph.
 *
 * This allows onCreate to check args and verify they're valid before accessing (and instantiating)
 * the viewModel.
 */
@OptIn(InternalMavericksApi::class)
internal inline fun <T, reified VM : MavericksViewModel<S>, reified S : MavericksState> T.viewModelLazy(
    viewModelClass: KClass<VM> = VM::class,
    crossinline keyFactory: () -> String = { viewModelClass.java.name }
): Lazy<VM> where T : ComponentActivity = lazy {
    MavericksViewModelProvider.get(
        viewModelClass = viewModelClass.java,
        stateClass = S::class.java,
        viewModelContext = ActivityViewModelContext(this, intent.extras?.get(Mavericks.KEY_ARG)),
        key = keyFactory()
    )
}

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
    this !is Fail -> false
    error is CancellationException -> true
    error is StripeException && error.cause is CancellationException -> true
    else -> false
}
