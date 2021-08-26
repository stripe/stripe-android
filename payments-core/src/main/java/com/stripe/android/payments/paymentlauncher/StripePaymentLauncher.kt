package com.stripe.android.payments.paymentlauncher

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.injection.DaggerPaymentLauncherComponent
import com.stripe.android.payments.core.injection.ENABLE_LOGGING
import com.stripe.android.payments.core.injection.IOContext
import com.stripe.android.payments.core.injection.Injectable
import com.stripe.android.payments.core.injection.Injector
import com.stripe.android.payments.core.injection.InjectorKey
import com.stripe.android.payments.core.injection.PUBLISHABLE_KEY
import com.stripe.android.payments.core.injection.PaymentLauncherComponent
import com.stripe.android.payments.core.injection.PaymentLauncherModule
import com.stripe.android.payments.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.payments.core.injection.UIContext
import com.stripe.android.payments.core.injection.WeakMapInjectorRegistry
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import javax.inject.Named
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

/**
 * Implementation of [PaymentLauncher], start an [PaymentLauncherConfirmationActivity] to confirm and
 * handle next actions for intents.
 */
internal class StripePaymentLauncher @AssistedInject internal constructor(
    @Assisted(PUBLISHABLE_KEY) publishableKeyProvider: Provider<String>,
    @Assisted(STRIPE_ACCOUNT_ID) stripeAccountIdProvider: Provider<String?>,
    @Assisted private val hostActivityLauncher: ActivityResultLauncher<PaymentLauncherContract.Args>,
    context: Context,
    @Named(ENABLE_LOGGING) enableLogging: Boolean,
    @IOContext ioContext: CoroutineContext,
    @UIContext uiContext: CoroutineContext,
    stripeRepository: StripeRepository,
    analyticsRequestFactory: AnalyticsRequestFactory
) : PaymentLauncher, Injector {
    private val paymentLauncherComponent: PaymentLauncherComponent =
        DaggerPaymentLauncherComponent.builder()
            .context(context)
            .enableLogging(enableLogging)
            .ioContext(ioContext)
            .uiContext(uiContext)
            .stripeRepository(stripeRepository)
            .analyticsRequestFactory(analyticsRequestFactory)
            .paymentLauncherModule(
                PaymentLauncherModule(
                    publishableKeyProvider,
                    stripeAccountIdProvider
                )
            )
            .build()

    @InjectorKey
    private val injectorKey: Int = WeakMapInjectorRegistry.nextKey()

    init {
        WeakMapInjectorRegistry.register(this, injectorKey)
    }

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
