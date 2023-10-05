package com.stripe.android.payments.paymentlauncher

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.injection.UIContext
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.core.authentication.PaymentAuthenticatorRegistry
import com.stripe.android.payments.core.injection.DaggerPaymentLauncherComponent
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.PaymentLauncherComponent
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

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
    context: Context,
    @Named(ENABLE_LOGGING) private val enableLogging: Boolean,
    @IOContext ioContext: CoroutineContext,
    @UIContext uiContext: CoroutineContext,
    paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
    @Named(PRODUCT_USAGE) private val productUsage: Set<String>
) : PaymentLauncher {
    private val paymentLauncherComponent: PaymentLauncherComponent =
        DaggerPaymentLauncherComponent.builder()
            .context(context)
            .enableLogging(enableLogging)
            .ioContext(ioContext)
            .uiContext(uiContext)
            .analyticsRequestFactory(paymentAnalyticsRequestFactory)
            .publishableKeyProvider(publishableKeyProvider)
            .stripeAccountIdProvider(stripeAccountIdProvider)
            .productUsage(productUsage)
            .build()

    val authenticatorRegistry: PaymentAuthenticatorRegistry by lazy {
        paymentLauncherComponent.authenticatorRegistry
    }

    override fun confirm(params: ConfirmPaymentIntentParams) {
        hostActivityLauncher.launch(
            PaymentLauncherContract.Args.IntentConfirmationArgs(
                publishableKey = publishableKeyProvider(),
                stripeAccountId = stripeAccountIdProvider(),
                enableLogging = enableLogging,
                productUsage = productUsage,
                confirmStripeIntentParams = params,
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
                setupIntentClientSecret = clientSecret,
                statusBarColor = statusBarColor,
            )
        )
    }
}
