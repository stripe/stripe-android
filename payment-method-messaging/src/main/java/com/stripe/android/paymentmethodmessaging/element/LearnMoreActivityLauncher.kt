package com.stripe.android.paymentmethodmessaging.element

import android.content.Context

internal interface LearnMoreActivityLauncher {
    fun launchLearnMoreActivity(context: Context, args: LearnMoreActivityArgs)
}

internal class DefaultLearnMoreActivityLauncher : LearnMoreActivityLauncher {
    override fun launchLearnMoreActivity(context: Context, args: LearnMoreActivityArgs) {
        context.startActivity(
            LearnMoreActivityArgs.createIntent(
                context,
                args
            )
        )
    }
}