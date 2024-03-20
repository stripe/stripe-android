package com.stripe.android.financialconnections.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.reflect.KProperty1

/**
 * Retrieves or builds a ViewModel instance, providing the [FinancialConnectionsSheetNativeComponent] (activity
 * retained component) to the factory to facilitate its creation via dependency injection.
 */
@Composable
internal inline fun <reified T : FinancialConnectionsViewModel<S>, S> paneViewModel(
    factory: (FinancialConnectionsSheetNativeComponent) -> ViewModelProvider.Factory
): T { return viewModel<T>(factory = factory(parentActivity().viewModel.activityRetainedComponent)) }

/**
 * Retrieves the parent [FinancialConnectionsSheetNativeActivity] from the current Compose context.
 */
@Composable
internal fun parentActivity(): FinancialConnectionsSheetNativeActivity {
    return extractActivityFromContext(LocalContext.current) as FinancialConnectionsSheetNativeActivity
}

private fun extractActivityFromContext(context: Context): ComponentActivity? {
    var currentContext = context
    if (currentContext is ComponentActivity) {
        return currentContext
    } else {
        while (currentContext is ContextWrapper) {
            if (currentContext is ComponentActivity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
    }
    return null
}

/**
 * Creates a Compose State variable that will only update when the value of this property changes.
 * Prefer this to subscribing to entire state classes which will trigger a recomposition whenever
 * any state variable changes.
 *
 * If you find yourself subscribing to many state properties in a single composable,
 * consider breaking it up into smaller ones.
 */
@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun <VM : FinancialConnectionsViewModel<S>, S, A> VM.collectAsState(prop1: KProperty1<S, A>): State<A> {
    val mappedFlow = remember(prop1) { stateFlow.map { prop1.get(it) }.distinctUntilChanged() }
    return mappedFlow.collectAsState(initial = prop1.get(stateFlow.value))
}
