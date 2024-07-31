package com.stripe.android.payments.core.authentication

import android.content.Context
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.VisibleForTesting
import com.stripe.android.PaymentRelayContract
import com.stripe.android.PaymentRelayStarter
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.model.StripeModel
import com.stripe.android.model.Source
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.DaggerNextActionHandlerComponent
import com.stripe.android.payments.core.injection.INCLUDE_PAYMENT_SHEET_NEXT_ACTION_HANDLERS
import com.stripe.android.payments.core.injection.IntentAuthenticatorMap
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

private typealias NextActionHandlerKey = Class<out StripeIntent.NextActionData>
private typealias NextActionHandler = @JvmSuppressWildcards PaymentNextActionHandler<StripeIntent>

/**
 * Default registry to provide look ups for [PaymentNextActionHandler].
 * Should be only accessed through [DefaultPaymentNextActionHandlerRegistry.createInstance].
 */
@Singleton
internal class DefaultPaymentNextActionHandlerRegistry @Inject internal constructor(
    private val noOpIntentNextActionHandler: NoOpIntentNextActionHandler,
    private val sourceNextActionHandler: SourceNextActionHandler,
    @IntentAuthenticatorMap private val paymentNextActionHandlers: Map<NextActionHandlerKey, NextActionHandler>,
    @Named(INCLUDE_PAYMENT_SHEET_NEXT_ACTION_HANDLERS) private val includePaymentSheetNextActionHandlers: Boolean,
    applicationContext: Context,
) : PaymentNextActionHandlerRegistry {

    private val paymentSheetNextActionHandlers: Map<NextActionHandlerKey, NextActionHandler> by lazy {
        paymentSheetNextActionHandlers(includePaymentSheetNextActionHandlers, applicationContext)
    }

    @VisibleForTesting
    internal val allNextActionHandlers: Set<PaymentNextActionHandler<out StripeModel>>
        get() = buildSet {
            add(noOpIntentNextActionHandler)
            add(sourceNextActionHandler)
            addAll(paymentNextActionHandlers.values)
            addAll(paymentSheetNextActionHandlers.values)
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
    override fun <Actionable> getNextActionHandler(
        actionable: Actionable
    ): PaymentNextActionHandler<Actionable> {
        return when (actionable) {
            is StripeIntent -> {
                if (!actionable.requiresAction()) {
                    return noOpIntentNextActionHandler as PaymentNextActionHandler<Actionable>
                }

                val allNextActionHandlers = paymentNextActionHandlers + paymentSheetNextActionHandlers
                val nextActionHandler = actionable.nextActionData?.let {
                    allNextActionHandlers[it::class.java]
                } ?: noOpIntentNextActionHandler

                return nextActionHandler as PaymentNextActionHandler<Actionable>
            }
            is Source -> {
                sourceNextActionHandler as PaymentNextActionHandler<Actionable>
            }
            else -> {
                error("No suitable PaymentNextActionHandler for $actionable")
            }
        }
    }

    override fun onNewActivityResultCaller(
        activityResultCaller: ActivityResultCaller,
        activityResultCallback: ActivityResultCallback<PaymentFlowResult.Unvalidated>
    ) {
        allNextActionHandlers.forEach {
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
        allNextActionHandlers.forEach {
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
            includePaymentSheetNextActionHandlers: Boolean,
        ): PaymentNextActionHandlerRegistry {
            val component = DaggerNextActionHandlerComponent.builder()
                .context(context)
                .analyticsRequestFactory(paymentAnalyticsRequestFactory)
                .enableLogging(enableLogging)
                .workContext(workContext)
                .uiContext(uiContext)
                .threeDs1IntentReturnUrlMap(threeDs1IntentReturnUrlMap)
                .publishableKeyProvider(publishableKeyProvider)
                .productUsage(productUsage)
                .isInstantApp(isInstantApp)
                .includePaymentSheetNextActionHandlers(includePaymentSheetNextActionHandlers)
                .build()
            return component.registry
        }
    }
}

@Suppress("TooGenericExceptionCaught")
private fun paymentSheetNextActionHandlers(
    includePaymentSheetNextActionHandlers: Boolean,
    applicationContext: Context
): Map<NextActionHandlerKey, NextActionHandler> {
    return try {
        if (includePaymentSheetNextActionHandlers) {
            val className = "com.stripe.android.paymentsheet.PaymentSheetNextActionHandlers"
            val nextActionHandlersObject = Class.forName(className).getDeclaredField("INSTANCE").get(null)
            val getMethod = nextActionHandlersObject.javaClass.getDeclaredMethod("get")
            @Suppress("UNCHECKED_CAST")
            return getMethod.invoke(nextActionHandlersObject) as Map<NextActionHandlerKey, NextActionHandler>
        } else {
            emptyMap()
        }
    } catch (e: Exception) {
        ErrorReporter.createFallbackInstance(applicationContext)
            .report(
                // [PAYMENT_SHEET_AUTHENTICATORS_NOT_FOUND] will not be changed to avoid skewed metrics
                errorEvent = ErrorReporter.UnexpectedErrorEvent.PAYMENT_SHEET_AUTHENTICATORS_NOT_FOUND,
                stripeException = StripeException.create(e),
            )
        emptyMap()
    }
}
