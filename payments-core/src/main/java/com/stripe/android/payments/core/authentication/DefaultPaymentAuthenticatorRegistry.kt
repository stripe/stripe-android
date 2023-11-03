package com.stripe.android.payments.core.authentication

import android.content.Context
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.VisibleForTesting
import com.stripe.android.PaymentRelayContract
import com.stripe.android.PaymentRelayStarter
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.core.model.StripeModel
import com.stripe.android.model.Source
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.core.injection.DaggerAuthenticationComponent
import com.stripe.android.payments.core.injection.INCLUDE_PAYMENT_SHEET_AUTHENTICATORS
import com.stripe.android.payments.core.injection.IntentAuthenticatorMap
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

private typealias AuthenticatorKey = Class<out StripeIntent.NextActionData>
private typealias Authenticator = @JvmSuppressWildcards PaymentAuthenticator<StripeIntent>

/**
 * Default registry to provide look ups for [PaymentAuthenticator].
 * Should be only accessed through [DefaultPaymentAuthenticatorRegistry.createInstance].
 */
@Singleton
internal class DefaultPaymentAuthenticatorRegistry @Inject internal constructor(
    private val noOpIntentAuthenticator: NoOpIntentAuthenticator,
    private val sourceAuthenticator: SourceAuthenticator,
    @IntentAuthenticatorMap private val paymentAuthenticators: Map<AuthenticatorKey, Authenticator>,
    @Named(INCLUDE_PAYMENT_SHEET_AUTHENTICATORS) private val includePaymentSheetAuthenticators: Boolean,
) : PaymentAuthenticatorRegistry {

    private val paymentSheetAuthenticators: Map<AuthenticatorKey, Authenticator> by lazy {
        paymentSheetAuthenticators(includePaymentSheetAuthenticators)
    }

    @VisibleForTesting
    internal val allAuthenticators: Set<PaymentAuthenticator<out StripeModel>>
        get() = buildSet {
            add(noOpIntentAuthenticator)
            add(sourceAuthenticator)
            addAll(paymentAuthenticators.values)
            addAll(paymentSheetAuthenticators.values)
        }

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

                val allAuthenticators = paymentAuthenticators + paymentSheetAuthenticators
                val authenticator = authenticatable.nextActionData?.let {
                    allAuthenticators[it::class.java]
                } ?: noOpIntentAuthenticator

                return authenticator as PaymentAuthenticator<Authenticatable>
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

    companion object {
        fun createInstance(
            context: Context,
            paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
            enableLogging: Boolean,
            workContext: CoroutineContext,
            uiContext: CoroutineContext,
            threeDs1IntentReturnUrlMap: MutableMap<String, String>,
            publishableKeyProvider: () -> String,
            productUsage: Set<String>,
            isInstantApp: Boolean,
            includePaymentSheetAuthenticators: Boolean,
        ): PaymentAuthenticatorRegistry {
            val component = DaggerAuthenticationComponent.builder()
                .context(context)
                .analyticsRequestFactory(paymentAnalyticsRequestFactory)
                .enableLogging(enableLogging)
                .workContext(workContext)
                .uiContext(uiContext)
                .threeDs1IntentReturnUrlMap(threeDs1IntentReturnUrlMap)
                .publishableKeyProvider(publishableKeyProvider)
                .productUsage(productUsage)
                .isInstantApp(isInstantApp)
                .includePaymentSheetAuthenticators(includePaymentSheetAuthenticators)
                .build()
            return component.registry
        }
    }
}

private fun paymentSheetAuthenticators(
    includePaymentSheetAuthenticators: Boolean
): Map<AuthenticatorKey, Authenticator> {
    return try {
        if (includePaymentSheetAuthenticators) {
            val className = "com.stripe.android.paymentsheet.PaymentSheetAuthenticators"
            val authenticatorsObject = Class.forName(className).getDeclaredField("INSTANCE").get(null)
            val getMethod = authenticatorsObject.javaClass.getDeclaredMethod("get")
            @Suppress("UNCHECKED_CAST")
            return getMethod.invoke(authenticatorsObject) as Map<AuthenticatorKey, Authenticator>
        } else {
            emptyMap()
        }
    } catch (e: Exception) {
        emptyMap()
    }
}
