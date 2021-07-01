package com.stripe.android.payments.core.authentication

import android.content.Context
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import com.stripe.android.PaymentBrowserAuthStarter
import com.stripe.android.PaymentRelayStarter
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.core.injection.DaggerAuthenticationComponent
import com.stripe.android.payments.core.injection.IntentAuthenticatorMap
import com.stripe.android.view.AuthActivityStarterHost
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Default registry to provide look ups for [IntentAuthenticator].
 * Should be only accessed through [DefaultIntentAuthenticatorRegistry.createInstance].
 */
internal class DefaultIntentAuthenticatorRegistry @Inject internal constructor(
    private val noOpIntentAuthenticator: NoOpIntentAuthenticator,
    @IntentAuthenticatorMap
    private val intentAuthenticatorMap:
        Map<Class<out StripeIntent.NextActionData>, @JvmSuppressWildcards IntentAuthenticator>
) : IntentAuthenticatorRegistry {

    override fun getAuthenticator(stripeIntent: StripeIntent): IntentAuthenticator {
        if (!stripeIntent.requiresAction()) {
            return noOpIntentAuthenticator
        }

        return stripeIntent.nextActionData?.let {
            intentAuthenticatorMap
                .getOrElse(it::class.java) { noOpIntentAuthenticator }
        } ?: run {
            noOpIntentAuthenticator
        }
    }

    override fun onNewActivityResultCaller(
        activityResultCaller: ActivityResultCaller,
        activityResultCallback: ActivityResultCallback<PaymentFlowResult.Unvalidated>
    ) {
        intentAuthenticatorMap.values.forEach {
            it.onNewActivityResultCaller(activityResultCaller, activityResultCallback)
        }
    }

    override fun onLauncherInvalidated() {
        intentAuthenticatorMap.values.forEach {
            it.onLauncherInvalidated()
        }
    }

    companion object {
        /**
         * Create an instance of [IntentAuthenticatorRegistry] with dagger.
         */
        fun createInstance(
            context: Context,
            stripeRepository: StripeRepository,
            paymentRelayStarterFactory: (AuthActivityStarterHost) -> PaymentRelayStarter,
            paymentBrowserAuthStarterFactory: (AuthActivityStarterHost) -> PaymentBrowserAuthStarter,
            analyticsRequestExecutor: AnalyticsRequestExecutor,
            analyticsRequestFactory: AnalyticsRequestFactory,
            enableLogging: Boolean,
            workContext: CoroutineContext,
            uiContext: CoroutineContext,
            returnUrlSupplier: () -> String?
        ) = DaggerAuthenticationComponent.builder()
            .context(context)
            .stripeRepository(stripeRepository)
            .paymentRelayStarterFactory(paymentRelayStarterFactory)
            .paymentBrowserAuthStarterFactory(paymentBrowserAuthStarterFactory)
            .analyticsRequestExecutor(analyticsRequestExecutor)
            .analyticsRequestFactory(analyticsRequestFactory)
            .enableLogging(enableLogging)
            .workContext(workContext)
            .uiContext(uiContext)
            .returnUrlSupplier(returnUrlSupplier)
            .build()
            .registry
    }
}
