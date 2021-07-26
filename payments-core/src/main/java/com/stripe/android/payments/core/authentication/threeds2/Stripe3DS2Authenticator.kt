package com.stripe.android.payments.core.authentication.threeds2

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.VisibleForTesting
import com.stripe.android.PaymentAuthConfig
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.ApiRequest
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.core.authentication.PaymentAuthenticator
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import com.stripe.android.view.AuthActivityStarterHost

/**
 * [PaymentAuthenticator] authenticating through Stripe's 3ds2 SDK.
 */
internal class Stripe3DS2Authenticator(
    private val config: PaymentAuthConfig,
    private val enableLogging: Boolean,
    private val threeDs1IntentReturnUrlMap: MutableMap<String, String>
) : PaymentAuthenticator<StripeIntent> {

    /**
     * [stripe3ds2CompletionLauncher] is mutable and might be updated during
     * through [onNewActivityResultCaller]
     */
    @VisibleForTesting
    internal var stripe3ds2CompletionLauncher:
        ActivityResultLauncher<Stripe3ds2TransactionContract.Args>? = null
    private val stripe3ds2CompletionStarterFactory =
        { host: AuthActivityStarterHost ->
            stripe3ds2CompletionLauncher?.let {
                Stripe3ds2TransactionStarter.Modern(it)
            } ?: Stripe3ds2TransactionStarter.Legacy(host)
        }

    override fun onNewActivityResultCaller(
        activityResultCaller: ActivityResultCaller,
        activityResultCallback: ActivityResultCallback<PaymentFlowResult.Unvalidated>
    ) {
        stripe3ds2CompletionLauncher = activityResultCaller.registerForActivityResult(
            Stripe3ds2TransactionContract(),
            activityResultCallback
        )
    }

    override fun onLauncherInvalidated() {
        stripe3ds2CompletionLauncher?.unregister()
        stripe3ds2CompletionLauncher = null
    }

    override suspend fun authenticate(
        host: AuthActivityStarterHost,
        authenticatable: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        stripe3ds2CompletionStarterFactory(host).start(
            Stripe3ds2TransactionContract.Args(
                SdkTransactionId.create(),
                config.stripe3ds2Config,
                authenticatable,
                authenticatable.nextActionData as StripeIntent.NextActionData.SdkData.Use3DS2,
                threeDs1ReturnUrl = authenticatable.id?.let {
                    threeDs1IntentReturnUrlMap[it]
                },
                requestOptions,
                enableLogging = enableLogging,
                host.statusBarColor
            )
        )
    }
}
