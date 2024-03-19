package com.stripe.android.financialconnections.core

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity

internal fun Context.activityViewModel(): FinancialConnectionsSheetNativeViewModel {
    val activity = extractActivityFromContext(this) as FinancialConnectionsSheetNativeActivity
    return activity.viewModel
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
