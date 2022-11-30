package com.stripe.android.financialconnections.utils

import androidx.activity.ComponentActivity
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.InternalMavericksApi
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.MavericksViewModelProvider
import com.airbnb.mvrx.lifecycleAwareLazy
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
internal inline fun <T, reified VM : MavericksViewModel<S>, reified S : MavericksState> T.viewModelIfArgsValid(
    viewModelClass: KClass<VM>,
    crossinline argsValidator: (Any?) -> Boolean,
    crossinline keyFactory: () -> String = { viewModelClass.java.name }
): Lazy<VM?> where T : ComponentActivity = lifecycleAwareLazy(this) {
    val mavericksArgs = intent.extras?.get(Mavericks.KEY_ARG)
    if (argsValidator(mavericksArgs))
        MavericksViewModelProvider.get(
            viewModelClass = viewModelClass.java,
            stateClass = S::class.java,
            viewModelContext = ActivityViewModelContext(this, mavericksArgs),
            key = keyFactory()
        )
    else {
        null
    }
}
