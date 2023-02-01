package com.stripe.android.paymentsheet

import androidx.activity.result.ActivityResultCaller
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncher
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.extensions.registerPollingAuthenticator
import com.stripe.android.paymentsheet.extensions.unregisterPollingAuthenticator
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.ConfirmStripeIntentParamsFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.channels.Channel
import javax.inject.Inject
import javax.inject.Provider

internal interface ConfirmationHandler {

    fun registerFromActivity(activityResultCaller: ActivityResultCaller)

    suspend fun confirm(
        paymentSelection: PaymentSelection,
        clientSecret: ClientSecret,
        shipping: ConfirmPaymentIntentParams.Shipping?,
    ): Result<PaymentResult>

    fun unregisterFromActivity()
}

internal class DefaultConfirmationHandler @Inject constructor(
    private val paymentLauncherFactory: StripePaymentLauncherAssistedFactory,
    private val lazyPaymentConfig: Provider<PaymentConfiguration>,
) : ConfirmationHandler {

    private var paymentLauncher: StripePaymentLauncher? = null
    private val resultChannel = Channel<PaymentResult>(capacity = 1)

    override fun registerFromActivity(activityResultCaller: ActivityResultCaller) {
        paymentLauncher = paymentLauncherFactory.create(
            publishableKey = { lazyPaymentConfig.get().publishableKey },
            stripeAccountId = { lazyPaymentConfig.get().stripeAccountId },
            hostActivityLauncher = activityResultCaller.registerForActivityResult(
                PaymentLauncherContract(),
                ::onPaymentResult
            )
        ).also {
            it.registerPollingAuthenticator()
        }
    }

    override suspend fun confirm(
        paymentSelection: PaymentSelection,
        clientSecret: ClientSecret,
        shipping: ConfirmPaymentIntentParams.Shipping?
    ): Result<PaymentResult> {
        val params = createConfirmIntentParams(paymentSelection, clientSecret, shipping)

        return if (params != null) {
            confirmIntent(params)
        } else {
            Result.failure(
                IllegalStateException("Attempted to create ConfirmStripeIntentParams " +
                    "for invalid payment selection $paymentSelection")
            )
        }
    }

    private fun createConfirmIntentParams(
        paymentSelection: PaymentSelection,
        clientSecret: ClientSecret,
        shipping: ConfirmPaymentIntentParams.Shipping?,
    ): ConfirmStripeIntentParams? {
        val confirmParamsFactory = ConfirmStripeIntentParamsFactory.createFactory(
            clientSecret = clientSecret,
            shipping = shipping,
        )

        return when (paymentSelection) {
            is PaymentSelection.Saved -> {
                confirmParamsFactory.create(paymentSelection)
            }
            is PaymentSelection.New -> {
                confirmParamsFactory.create(paymentSelection)
            }
            else -> null
        }
    }

    private suspend fun confirmIntent(params: ConfirmStripeIntentParams): Result<PaymentResult> {
        val paymentResult = paymentLauncher?.confirm(params)
        return if (paymentResult != null) {
            Result.success(paymentResult)
        } else {
            Result.failure(
                IllegalStateException("Attempted to confirm StripeIntent without a PaymentLauncher")
            )
        }
    }

    private suspend fun PaymentLauncher.confirm(params: ConfirmStripeIntentParams): PaymentResult {
        when (params) {
            is ConfirmPaymentIntentParams -> {
                confirm(params)
            }
            is ConfirmSetupIntentParams -> {
                confirm(params)
            }
        }

        return resultChannel.receive()
    }

    private fun onPaymentResult(result: PaymentResult) {
        resultChannel.trySend(result)
    }

    override fun unregisterFromActivity() {
        paymentLauncher?.unregisterPollingAuthenticator()
        paymentLauncher = null
    }
}
