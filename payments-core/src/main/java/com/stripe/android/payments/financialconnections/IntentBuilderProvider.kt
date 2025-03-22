package com.stripe.android.payments.financialconnections

import android.content.Context
import android.content.Intent
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.lite.liteIntentBuilder
import com.stripe.android.payments.financialconnections.IntentBuilderProvider.IntentBuilder
import com.stripe.android.financialconnections.intentBuilder as fullIntentBuilder

internal interface IntentBuilderProvider {
    fun provide(context: Context, isFullSdkAvailable: Boolean): IntentBuilder

    sealed class IntentBuilder(val provider: (FinancialConnectionsSheetActivityArgs) -> Intent) {
        class Lite(context: Context) : IntentBuilder(liteIntentBuilder(context))
        class Full(context: Context) : IntentBuilder(fullIntentBuilder(context))
    }
}

internal class DefaultIntentBuilderProvider : IntentBuilderProvider {

    override fun provide(context: Context, isFullSdkAvailable: Boolean): IntentBuilder {
        return if (isFullSdkAvailable) {
            IntentBuilder.Full(context)
        } else {
            IntentBuilder.Lite(context)
        }
    }
}
