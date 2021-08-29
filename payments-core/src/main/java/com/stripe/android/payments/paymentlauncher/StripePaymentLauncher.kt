package com.stripe.android.payments.paymentlauncher

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.payments.core.injection.DaggerPaymentLauncherComponent
import com.stripe.android.payments.core.injection.Injectable
import com.stripe.android.payments.core.injection.Injector
import com.stripe.android.payments.core.injection.InjectorKey
import com.stripe.android.payments.core.injection.PaymentLauncherComponent

/**
 * Implementation of [PaymentLauncher], start an [PaymentLauncherConfirmationActivity] to confirm and
 * handle next actions for intents.
 */
internal class StripePaymentLauncher internal constructor(
    private val hostActivityLauncher: ActivityResultLauncher<PaymentLauncherContract.Args>,
    context: Context,
    publishableKey: String,
    stripeAccountId: String? = null,
    @InjectorKey private val injectorKey: Int
) : PaymentLauncher, Injector {

    private val paymentLauncherComponent: PaymentLauncherComponent =
        DaggerPaymentLauncherComponent.builder()
            .context(context)
            .publishableKey(publishableKey)
            .stripeAccountId(stripeAccountId)
            .build()

    override fun inject(injectable: Injectable) {
        when (injectable) {
            is PaymentLauncherViewModel.Factory -> {
                paymentLauncherComponent.inject(injectable)
            }
            else -> {
                throw IllegalArgumentException("invalid Injectable $injectable requested in $this")
            }
        }
    }

    override fun confirm(params: ConfirmPaymentIntentParams) {
        hostActivityLauncher.launch(
            PaymentLauncherContract.Args.IntentConfirmationArgs(
                injectorKey = injectorKey,
                confirmStripeIntentParams = params
            )
        )
    }

    override fun confirm(params: ConfirmSetupIntentParams) {
        hostActivityLauncher.launch(
            PaymentLauncherContract.Args.IntentConfirmationArgs(
                injectorKey = injectorKey,
                confirmStripeIntentParams = params
            )
        )
    }

    override fun handleNextActionForPaymentIntent(clientSecret: String) {
        hostActivityLauncher.launch(
            PaymentLauncherContract.Args.PaymentIntentNextActionArgs(
                injectorKey = injectorKey,
                paymentIntentClientSecret = clientSecret
            )
        )
    }

    override fun handleNextActionForSetupIntent(clientSecret: String) {
        hostActivityLauncher.launch(
            PaymentLauncherContract.Args.SetupIntentNextActionArgs(
                injectorKey = injectorKey,
                setupIntentClientSecret = clientSecret
            )
        )
    }
}
