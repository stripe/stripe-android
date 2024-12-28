package com.stripe.android.link.confirmation

import androidx.activity.result.ActivityResultCaller
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Confirms a Payment Intent or Setup Intent using [PaymentLauncher].
 */
@Singleton
internal class ConfirmationManager @Inject constructor(
    private val paymentLauncherFactory: StripePaymentLauncherAssistedFactory,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(STRIPE_ACCOUNT_ID) private val stripeAccountIdProvider: () -> String?
) {
    private var paymentLauncher: PaymentLauncher? = null
    private var completionCallback: PaymentConfirmationCallback? = null

    /**
     * Initialize the [PaymentLauncher] used to confirm a StripeIntent.
     * Must be called every time LinkActivity is recreated, so it registers the
     * ActivityResultCallback.
     */
    fun setupPaymentLauncher(activityResultCaller: ActivityResultCaller) {
        paymentLauncher = paymentLauncherFactory.create(
            publishableKey = publishableKeyProvider,
            stripeAccountId = stripeAccountIdProvider,
            statusBarColor = null,
            includePaymentSheetNextHandlers = false,
            hostActivityLauncher = activityResultCaller.registerForActivityResult(
                PaymentLauncherContract(),
                ::onPaymentResult
            )
        )
    }

    fun invalidatePaymentLauncher() {
        paymentLauncher = null
    }

    /**
     * Confirms a StripeIntent using the given [confirmStripeIntentParams], and calls [onResult]
     * when the confirmation fails or is completed.
     */
    fun confirmStripeIntent(
        confirmStripeIntentParams: ConfirmStripeIntentParams,
        onResult: PaymentConfirmationCallback
    ) {
        completionCallback = onResult
        runCatching {
            requireNotNull(paymentLauncher)
        }.fold(
            onSuccess = {
                when (confirmStripeIntentParams) {
                    is ConfirmPaymentIntentParams -> {
                        it.confirm(confirmStripeIntentParams)
                    }
                    is ConfirmSetupIntentParams -> {
                        it.confirm(confirmStripeIntentParams)
                    }
                }
            },
            onFailure = {
                onResult(Result.failure(it))
            }
        )
    }

    fun handleNextAction(
        clientSecret: String,
        stripeIntent: StripeIntent,
        onResult: PaymentConfirmationCallback
    ) {
        completionCallback = onResult
        runCatching {
            requireNotNull(paymentLauncher)
        }.fold(
            onSuccess = {
                when (stripeIntent) {
                    is PaymentIntent -> {
                        it.handleNextActionForPaymentIntent(clientSecret)
                    }
                    is SetupIntent -> {
                        it.handleNextActionForSetupIntent(clientSecret)
                    }
                }
            },
            onFailure = {
                onResult(Result.failure(it))
            }
        )
    }

    private fun onPaymentResult(paymentResult: InternalPaymentResult) =
        completionCallback?.let {
            it(Result.success(paymentResult))
        }
}

typealias PaymentConfirmationCallback = (Result<InternalPaymentResult>) -> Unit
