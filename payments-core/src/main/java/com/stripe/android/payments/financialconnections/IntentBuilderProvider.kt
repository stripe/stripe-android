package com.stripe.android.payments.financialconnections

import android.content.Context
import android.content.Intent
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.lite.liteIntentBuilder
import com.stripe.android.payments.financialconnections.IntentBuilderProvider.IntentBuilder
import com.stripe.android.financialconnections.intentBuilder as fullIntentBuilder

interface IntentBuilderProvider {
    fun provide(context: Context, isFinancialConnectionsAvailable: Boolean): IntentBuilder

    sealed class IntentBuilder(val provider: (FinancialConnectionsSheetActivityArgs) -> Intent) {
        class Lite(context: Context) : IntentBuilder(liteIntentBuilder(context))
        class Full(context: Context) : IntentBuilder(fullIntentBuilder(context))
    }
}

class DefaultIntentBuilderProvider() : IntentBuilderProvider {

    override fun provide(context: Context, isFinancialConnectionsAvailable: Boolean): IntentBuilder {
        return if (isFinancialConnectionsAvailable) {
            IntentBuilder.Full(context)
        } else {
            IntentBuilder.Lite(context)
        }
    }
}
