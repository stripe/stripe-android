package com.stripe.android.financialconnections.utils

import android.app.Activity
import androidx.activity.ComponentActivity
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.InternalMavericksApi
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.MavericksViewModelProvider
import kotlin.reflect.KClass

/**
 * Replicates [com.airbnb.mvrx.viewModel] delegate, but returning an optional [MavericksViewModel]
 * instance instead. ViewModel will just be instantiated if the [Mavericks.KEY_ARG] extras are validated
 * by [argsValidator].
 *
 * Some [MavericksViewModelFactory] implementations assume args are correct to instantiate
 * the viewModel, and the associated Dagger graph.
 *
 * @see [com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel.Companion]
 * @see [com.stripe.android.financialconnections.FinancialConnectionsSheetViewModel.Companion]
 */
@OptIn(InternalMavericksApi::class)
internal inline fun <reified VM : MavericksViewModel<S>, reified S : MavericksState> ComponentActivity.viewModelIfArgsValid(
    viewModelClass: KClass<VM> = VM::class,
): Lazy<VM> = lazy {
    MavericksViewModelProvider.get(
        viewModelClass = viewModelClass.java,
        stateClass = S::class.java,
        viewModelContext = ActivityViewModelContext(this, intent.extras?.get(Mavericks.KEY_ARG)),
        key = viewModelClass.java.name
    )
}

internal fun Activity.providerIsInvalid(provider: () -> Unit): Boolean {
    return try {
        provider()
        false
    } catch (e: IllegalStateException) {
        finish()
        true
    }
}
