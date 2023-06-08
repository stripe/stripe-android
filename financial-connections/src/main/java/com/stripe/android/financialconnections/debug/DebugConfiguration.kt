package com.stripe.android.financialconnections.debug

import android.app.Application
import android.content.Context
import javax.inject.Inject

internal class DebugConfiguration @Inject constructor(
    context: Application
) {

    private val sharedPreferences = context
        .getSharedPreferences("FINANCIAL_CONNECTIONS_DEBUG", Context.MODE_PRIVATE)

    internal val overriddenNative: Boolean?
        get() = if (sharedPreferences.contains(KEY_OVERRIDE_NATIVE)) {
            sharedPreferences.getBoolean(KEY_OVERRIDE_NATIVE, false)
        } else {
            null
        }
}

private const val KEY_OVERRIDE_NATIVE = "financial_connections_override_native"
