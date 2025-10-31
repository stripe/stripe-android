package com.stripe.android.challenge.confirmation

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.StripeIntentResult
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.core.authentication.PaymentNextActionHandler
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

/**
 * [PaymentNextActionHandler] for handling [StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge]
 * through a JavaScript-based WebView implementation.
 */
class IntentConfirmationChallengeNextActionHandler @Inject constructor(
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(PRODUCT_USAGE) private val productUsageTokens: Set<String>,
    @UIContext private val uiContext: CoroutineContext
) : PaymentNextActionHandler<StripeIntent>() {
    /**
     * [intentConfirmationChallengeActivityContractNextActionLauncher] is mutable and might be updated during
     * through [onNewActivityResultCaller]
     */
    @VisibleForTesting
    internal var intentConfirmationChallengeActivityContractNextActionLauncher:
        ActivityResultLauncher<IntentConfirmationChallengeActivityContract.Args>? = null

    private val intentConfirmationChallengeNextActionStarterFactory = { host: AuthActivityStarterHost ->
        intentConfirmationChallengeActivityContractNextActionLauncher?.let {
            IntentConfirmationChallengeNextActionStarter.Modern(it)
        } ?: IntentConfirmationChallengeNextActionStarter.Legacy(host)
    }

    override fun onNewActivityResultCaller(
        activityResultCaller: ActivityResultCaller,
        activityResultCallback: ActivityResultCallback<PaymentFlowResult.Unvalidated>
    ) {
        intentConfirmationChallengeActivityContractNextActionLauncher = activityResultCaller.registerForActivityResult(
            IntentConfirmationChallengeActivityContract()
        ) { result ->
            activityResultCallback.onActivityResult(
                when (result) {
                    is IntentConfirmationChallengeActivityResult.Failed -> {
                        PaymentFlowResult.Unvalidated(
                            flowOutcome = StripeIntentResult.Outcome.FAILED,
                            exception = StripeException.create(result.error)
                        )
                    }
                    is IntentConfirmationChallengeActivityResult.Success -> {
                        PaymentFlowResult.Unvalidated(
                            clientSecret = result.clientSecret,
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
        val intentConfirmationChallengeNextActionStarter = intentConfirmationChallengeNextActionStarterFactory(host)
        intentConfirmationChallengeNextActionStarter.start(
            IntentConfirmationChallengeActivityContract.Args(
                publishableKey = publishableKeyProvider(),
                intent = actionable,
                productUsage = productUsageTokens
            )
        )
    }
}
