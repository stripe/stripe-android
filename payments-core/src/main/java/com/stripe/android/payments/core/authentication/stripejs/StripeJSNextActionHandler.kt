package com.stripe.android.payments.core.authentication.stripejs

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.VisibleForTesting
import com.stripe.android.StripeIntentResult
import com.stripe.android.StripeIntentResult.Outcome.Companion.CANCELED
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.core.authentication.PaymentNextActionHandler
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * [PaymentNextActionHandler] for handling [StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge]
 * through a JavaScript-based WebView implementation.
 */
@Singleton
internal class StripeJSNextActionHandler @Inject constructor(
    @Named(ENABLE_LOGGING) private val enableLogging: Boolean,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @UIContext private val uiContext: CoroutineContext
) : PaymentNextActionHandler<StripeIntent>() {

    /**
     * [stripeJsNextActionLauncher] is mutable and might be updated during
     * through [onNewActivityResultCaller]
     */
    @VisibleForTesting
    internal var stripeJsNextActionLauncher: ActivityResultLauncher<StripeJsNextActionContract.Args>? = null

    private val stripeJsNextActionStarterFactory = { host: AuthActivityStarterHost ->
        stripeJsNextActionLauncher?.let {
            StripeJsNextActionStarter.Modern(it)
        } ?: StripeJsNextActionStarter.Legacy(host)
    }

    override fun onNewActivityResultCaller(
        activityResultCaller: ActivityResultCaller,
        activityResultCallback: ActivityResultCallback<PaymentFlowResult.Unvalidated>
    ) {
        stripeJsNextActionLauncher = activityResultCaller.registerForActivityResult(
            StripeJsNextActionContract()
        ) { result ->
            activityResultCallback.onActivityResult(
                when (result) {
                    is StripeJsNextActionActivityResult.Completed -> {
                        PaymentFlowResult.Unvalidated(
                            clientSecret = result.clientSecret,
                        )
                    }
                    is StripeJsNextActionActivityResult.Canceled -> {
                        PaymentFlowResult.Unvalidated(flowOutcome = CANCELED)
                    }
                    is StripeJsNextActionActivityResult.Failed -> {
                        PaymentFlowResult.Unvalidated(
                            flowOutcome = StripeIntentResult.Outcome.FAILED,
                            exception = StripeException.create(result.error)
                        )
                    }
                }
            )
        }
    }

    override suspend fun performNextActionOnResumed(
        host: AuthActivityStarterHost,
        actionable: StripeIntent,
        requestOptions: ApiRequest.Options
    ) = withContext(uiContext) {
        val nextActionData = actionable.nextActionData
//        require(nextActionData is StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge) {
//            "StripeJSNextActionHandler can only handle IntentConfirmationChallenge, but received: $nextActionData"
//        }

        val stripeJsNextActionStarter = stripeJsNextActionStarterFactory(host)
        stripeJsNextActionStarter.start(
            StripeJsNextActionContract.Args(
                publishableKey = publishableKeyProvider(),
                intent = actionable
            )
        )
    }
}
