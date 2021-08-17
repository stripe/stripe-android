package com.stripe.android.payments.core.authentication

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import com.stripe.android.PaymentRelayContract
import com.stripe.android.PaymentRelayStarter
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.model.Source
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.core.authentication.threeds2.Stripe3ds2TransactionViewModelFactory
import com.stripe.android.payments.core.injection.AuthenticationComponent
import com.stripe.android.payments.core.injection.DaggerAuthenticationComponent
import com.stripe.android.payments.core.injection.Injectable
import com.stripe.android.payments.core.injection.Injector
import com.stripe.android.payments.core.injection.IntentAuthenticatorMap
import com.stripe.android.payments.core.injection.WeakMapInjectorRegistry
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Default registry to provide look ups for [PaymentAuthenticator].
 * Should be only accessed through [DefaultPaymentAuthenticatorRegistry.createInstance].
 */
@Singleton
internal class DefaultPaymentAuthenticatorRegistry @Inject internal constructor(
    private val noOpIntentAuthenticator: NoOpIntentAuthenticator,
    private val sourceAuthenticator: SourceAuthenticator,
    @IntentAuthenticatorMap
    private val paymentAuthenticatorMap:
        Map<Class<out StripeIntent.NextActionData>,
            @JvmSuppressWildcards PaymentAuthenticator<StripeIntent>>
) : PaymentAuthenticatorRegistry, Injector {
    @VisibleForTesting
    internal val allAuthenticators = setOf(
        listOf(noOpIntentAuthenticator, sourceAuthenticator),
        paymentAuthenticatorMap.values
    ).flatten()

    /**
     * [AuthenticationComponent] instance is hold to inject into [Activity]s and [ViewModel]s
     * started by the [PaymentAuthenticator]s.
     */
    lateinit var authenticationComponent: AuthenticationComponent

    /**
     * [paymentRelayLauncher] is mutable and might be updated through [onNewActivityResultCaller]
     */
    internal var paymentRelayLauncher: ActivityResultLauncher<PaymentRelayStarter.Args>? = null

    /**
     * [paymentBrowserAuthLauncher] is mutable and might be updated through
     * [onNewActivityResultCaller]
     */
    internal var paymentBrowserAuthLauncher: ActivityResultLauncher<PaymentBrowserAuthContract.Args>? =
        null

    @Suppress("UNCHECKED_CAST")
    override fun <Authenticatable> getAuthenticator(
        authenticatable: Authenticatable
    ): PaymentAuthenticator<Authenticatable> {
        return when (authenticatable) {
            is StripeIntent -> {
                if (!authenticatable.requiresAction()) {
                    return noOpIntentAuthenticator as PaymentAuthenticator<Authenticatable>
                }
                return (
                    authenticatable.nextActionData?.let {
                        paymentAuthenticatorMap
                            .getOrElse(it::class.java) { noOpIntentAuthenticator }
                    } ?: run {
                        noOpIntentAuthenticator
                    }
                    ) as PaymentAuthenticator<Authenticatable>
            }
            is Source -> {
                sourceAuthenticator as PaymentAuthenticator<Authenticatable>
            }
            else -> {
                error("No suitable PaymentAuthenticator for $authenticatable")
            }
        }
    }

    override fun onNewActivityResultCaller(
        activityResultCaller: ActivityResultCaller,
        activityResultCallback: ActivityResultCallback<PaymentFlowResult.Unvalidated>
    ) {
        allAuthenticators.forEach {
            it.onNewActivityResultCaller(activityResultCaller, activityResultCallback)
        }
        paymentRelayLauncher = activityResultCaller.registerForActivityResult(
            PaymentRelayContract(),
            activityResultCallback
        )
        paymentBrowserAuthLauncher = activityResultCaller.registerForActivityResult(
            PaymentBrowserAuthContract(),
            activityResultCallback
        )
    }

    override fun onLauncherInvalidated() {
        allAuthenticators.forEach {
            it.onLauncherInvalidated()
        }
        paymentRelayLauncher?.unregister()
        paymentBrowserAuthLauncher?.unregister()
        paymentRelayLauncher = null
        paymentBrowserAuthLauncher = null
    }

    override fun inject(injectable: Injectable) {
        when (injectable) {
            is Stripe3ds2TransactionViewModelFactory -> authenticationComponent.inject(injectable)
        }
    }

    companion object {
        fun createInstance(
            context: Context,
            stripeRepository: StripeRepository,
            analyticsRequestExecutor: AnalyticsRequestExecutor,
            analyticsRequestFactory: AnalyticsRequestFactory,
            enableLogging: Boolean,
            workContext: CoroutineContext,
            uiContext: CoroutineContext,
            threeDs1IntentReturnUrlMap: MutableMap<String, String>
        ): PaymentAuthenticatorRegistry {
            val injectorKey = WeakMapInjectorRegistry.nextKey()
            val component = DaggerAuthenticationComponent.builder()
                .context(context)
                .stripeRepository(stripeRepository)
                .analyticsRequestExecutor(analyticsRequestExecutor)
                .analyticsRequestFactory(analyticsRequestFactory)
                .enableLogging(enableLogging)
                .workContext(workContext)
                .uiContext(uiContext)
                .threeDs1IntentReturnUrlMap(threeDs1IntentReturnUrlMap)
                .injectorKey(injectorKey)
                .build()
            val registry = component.registry
            registry.authenticationComponent = component
            WeakMapInjectorRegistry.register(registry, injectorKey)
            return registry
        }
    }
}
