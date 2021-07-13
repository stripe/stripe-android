package com.stripe.android.payments.core.authentication.threeds2

import com.stripe.android.PaymentAuthConfig
import com.stripe.android.StripePaymentController
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.ApiRequest
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

    override suspend fun authenticate(
        host: AuthActivityStarterHost,
        authenticatable: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        host.startActivityForResult(
            Stripe3ds2TransactionActivity::class.java,
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
            ).toBundle(),
            StripePaymentController.getRequestCode(authenticatable)
        )
    }
}
