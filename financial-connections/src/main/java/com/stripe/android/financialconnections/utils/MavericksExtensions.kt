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
internal inline fun <reified VM : MavericksViewModel<S>, reified S : MavericksState> ComponentActivity.viewModelIfArgsValid(
    viewModelClass: KClass<VM> = VM::class,
): Lazy<VM> {
    return MavericksViewModelLazy(viewModelClass, S::class.java, this)
}

internal class MavericksViewModelLazy<VM : MavericksViewModel<S>, S : MavericksState>(
    private val viewModelClass: KClass<VM>,
    private val stateClass: Class<out S>,
    private val activity: ComponentActivity,
): Lazy<VM> {
    private var cached: VM? = null

    override val value: VM
        @OptIn(InternalMavericksApi::class)
        get() {
            cached?.let {
                return it
            }

            val viewModel = MavericksViewModelProvider.get(
                viewModelClass = viewModelClass.java,
                stateClass = stateClass,
                viewModelContext = ActivityViewModelContext(activity, activity.intent.extras?.get(Mavericks.KEY_ARG)),
                key = viewModelClass.java.name
            )
            cached = viewModel
            return viewModel
        }

    override fun isInitialized(): Boolean {
        return cached != null
    }

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
