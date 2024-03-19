package com.stripe.android.financialconnections.core

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity

@Composable
internal inline fun <reified T : FinancialConnectionsViewModel<S>, S> rememberPaneViewModel(
    factory: (FinancialConnectionsSheetNativeViewModel) -> ViewModelProvider.Factory
): T {
    val parentActivity = extractActivityFromContext(LocalContext.current) as FinancialConnectionsSheetNativeActivity
    return viewModel<T>(factory = factory(parentActivity.viewModel))
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
