package com.stripe.android.payments.paymentlauncher

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.injection.DaggerPaymentLauncherComponent
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.PUBLISHABLE_KEY
import com.stripe.android.payments.core.injection.PaymentLauncherComponent
import com.stripe.android.payments.core.injection.STRIPE_ACCOUNT_ID
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
    context: Context,
    @Named(ENABLE_LOGGING) private val enableLogging: Boolean,
    @IOContext ioContext: CoroutineContext,
    @UIContext uiContext: CoroutineContext,
    stripeRepository: StripeRepository,
    paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
    @Named(PRODUCT_USAGE) private val productUsage: Set<String>
) : PaymentLauncher, Injector {
    private val paymentLauncherComponent: PaymentLauncherComponent =
        DaggerPaymentLauncherComponent.builder()
            .context(context)
            .enableLogging(enableLogging)
            .ioContext(ioContext)
            .uiContext(uiContext)
            .stripeRepository(stripeRepository)
            .analyticsRequestFactory(paymentAnalyticsRequestFactory)
            .publishableKeyProvider(publishableKeyProvider)
            .stripeAccountIdProvider(stripeAccountIdProvider)
            .productUsage(productUsage)
            .build()

    @InjectorKey
    private val injectorKey: String =
        WeakMapInjectorRegistry.nextKey(requireNotNull(PaymentLauncher::class.simpleName))

    init {
        WeakMapInjectorRegistry.register(this, injectorKey)
    }

    override fun inject(injectable: Injectable<*>) {
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
                publishableKey = publishableKeyProvider(),
                stripeAccountId = stripeAccountIdProvider(),
                enableLogging = enableLogging,
                productUsage = productUsage,
                confirmStripeIntentParams = params
            )
        )
    }

    override fun confirm(params: ConfirmSetupIntentParams) {
        hostActivityLauncher.launch(
            PaymentLauncherContract.Args.IntentConfirmationArgs(
                injectorKey = injectorKey,
                publishableKey = publishableKeyProvider(),
                stripeAccountId = stripeAccountIdProvider(),
                enableLogging = enableLogging,
                productUsage = productUsage,
                confirmStripeIntentParams = params
            )
        )
    }

    override fun handleNextActionForPaymentIntent(clientSecret: String) {
        hostActivityLauncher.launch(
            PaymentLauncherContract.Args.PaymentIntentNextActionArgs(
                injectorKey = injectorKey,
                publishableKey = publishableKeyProvider(),
                stripeAccountId = stripeAccountIdProvider(),
                enableLogging = enableLogging,
                productUsage = productUsage,
                paymentIntentClientSecret = clientSecret
            )
        )
    }

    override fun handleNextActionForSetupIntent(clientSecret: String) {
        hostActivityLauncher.launch(
            PaymentLauncherContract.Args.SetupIntentNextActionArgs(
                injectorKey = injectorKey,
                publishableKey = publishableKeyProvider(),
                stripeAccountId = stripeAccountIdProvider(),
                enableLogging = enableLogging,
                productUsage = productUsage,
                setupIntentClientSecret = clientSecret
            )
        )
    }
}
