package com.stripe.android.paymentsheet.example.playground.network

import android.content.Context
import com.stripe.android.paymentsheet.example.Settings
import com.stripe.android.paymentsheet.example.playground.settings.CustomEndpointDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettings

internal abstract class BasePlaygroundRequester(
    protected val playgroundSettings: PlaygroundSettings.Snapshot,
    protected val applicationContext: Context,
) {
    protected val settings by lazy {
        Settings(applicationContext)
    }

    protected val baseUrl: String
        get() {
            val customEndpoint = playgroundSettings[CustomEndpointDefinition]
            return customEndpoint ?: settings.playgroundBackendUrl
        }
}
