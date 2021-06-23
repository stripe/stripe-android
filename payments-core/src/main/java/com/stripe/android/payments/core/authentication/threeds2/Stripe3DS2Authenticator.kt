package com.stripe.android.payments.core.authentication.threeds2

import com.stripe.android.PaymentAuthConfig
import com.stripe.android.StripePaymentController
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.ApiRequest
import com.stripe.android.payments.core.authentication.IntentAuthenticator
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import com.stripe.android.view.AuthActivityStarterHost

/**
 * [IntentAuthenticator] authenticating through Stripe's 3ds2 SDK.
 */
internal class Stripe3DS2Authenticator(
    private val config: PaymentAuthConfig,
    private val enableLogging: Boolean
) : IntentAuthenticator {

    override suspend fun authenticate(
        host: AuthActivityStarterHost,
        stripeIntent: StripeIntent,
        threeDs1ReturnUrl: String?,
        requestOptions: ApiRequest.Options
    ) {
        host.startActivityForResult(
            Stripe3ds2TransactionActivity::class.java,
            Stripe3ds2TransactionContract.Args(
                SdkTransactionId.create(),
                config.stripe3ds2Config,
                stripeIntent,
                stripeIntent.nextActionData as StripeIntent.NextActionData.SdkData.Use3DS2,
                threeDs1ReturnUrl,
                requestOptions,
                enableLogging = enableLogging,
                host.statusBarColor
            ).toBundle(),
            StripePaymentController.getRequestCode(stripeIntent)
        )
    }
}
