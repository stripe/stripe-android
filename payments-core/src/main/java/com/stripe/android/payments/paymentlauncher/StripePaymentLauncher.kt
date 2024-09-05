package com.stripe.android.payments.paymentlauncher

import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.payments.core.injection.INCLUDE_PAYMENT_SHEET_NEXT_ACTION_HANDLERS
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import javax.inject.Named

/**
 * Implementation of [PaymentLauncher], start an [PaymentLauncherConfirmationActivity] to confirm and
 * handle next actions for intents.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StripePaymentLauncher @AssistedInject internal constructor(
    @Assisted(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Assisted(STRIPE_ACCOUNT_ID) private val stripeAccountIdProvider: () -> String?,
    @Assisted private val hostActivityLauncher: ActivityResultLauncher<PaymentLauncherContract.Args>,
    @Assisted(STATUS_BAR_COLOR) private val statusBarColor: Int?,
    @Assisted(INCLUDE_PAYMENT_SHEET_NEXT_ACTION_HANDLERS) private val includePaymentSheetNextHandlers: Boolean,
    @Named(ENABLE_LOGGING) private val enableLogging: Boolean,
    @Named(PRODUCT_USAGE) private val productUsage: Set<String>,
) : PaymentLauncher {
    override fun confirm(params: ConfirmPaymentIntentParams) {
        hostActivityLauncher.launch(
            PaymentLauncherContract.Args.IntentConfirmationArgs(
                publishableKey = publishableKeyProvider(),
                stripeAccountId = stripeAccountIdProvider(),
                enableLogging = enableLogging,
                productUsage = productUsage,
                confirmStripeIntentParams = params,
                includePaymentSheetNextHandlers = includePaymentSheetNextHandlers,
                statusBarColor = statusBarColor,
            )
        )
    }

    override fun confirm(params: ConfirmSetupIntentParams) {
        hostActivityLauncher.launch(
            PaymentLauncherContract.Args.IntentConfirmationArgs(
                publishableKey = publishableKeyProvider(),
                stripeAccountId = stripeAccountIdProvider(),
                enableLogging = enableLogging,
                productUsage = productUsage,
                includePaymentSheetNextHandlers = includePaymentSheetNextHandlers,
                confirmStripeIntentParams = params,
                statusBarColor = statusBarColor,
            )
        )
    }

    override fun handleNextActionForPaymentIntent(clientSecret: String) {
        hostActivityLauncher.launch(
            PaymentLauncherContract.Args.PaymentIntentNextActionArgs(
                publishableKey = publishableKeyProvider(),
                stripeAccountId = stripeAccountIdProvider(),
                enableLogging = enableLogging,
                productUsage = productUsage,
                includePaymentSheetNextHandlers = includePaymentSheetNextHandlers,
                paymentIntentClientSecret = clientSecret,
                statusBarColor = statusBarColor,
            )
        )
    }

    override fun handleNextActionForSetupIntent(clientSecret: String) {
        hostActivityLauncher.launch(
            PaymentLauncherContract.Args.SetupIntentNextActionArgs(
                publishableKey = publishableKeyProvider(),
                stripeAccountId = stripeAccountIdProvider(),
                enableLogging = enableLogging,
                productUsage = productUsage,
                includePaymentSheetNextHandlers = includePaymentSheetNextHandlers,
                setupIntentClientSecret = clientSecret,
                statusBarColor = statusBarColor,
            )
        )
    }
}
