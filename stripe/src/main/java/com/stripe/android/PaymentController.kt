package com.stripe.android

import android.content.Intent
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.Source
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.ApiRequest
import com.stripe.android.view.AuthActivityStarter

internal interface PaymentController {
    /**
     * Confirm the Stripe Intent and resolve any next actions
     */
    fun startConfirmAndAuth(
        host: AuthActivityStarter.Host,
        confirmStripeIntentParams: ConfirmStripeIntentParams,
        requestOptions: ApiRequest.Options
    )

    fun startConfirm(
        confirmStripeIntentParams: ConfirmStripeIntentParams,
        requestOptions: ApiRequest.Options,
        callback: ApiResultCallback<StripeIntent>
    )

    fun startAuth(
        host: AuthActivityStarter.Host,
        clientSecret: String,
        requestOptions: ApiRequest.Options,
        type: StripeIntentType
    )

    fun startAuthenticateSource(
        host: AuthActivityStarter.Host,
        source: Source,
        requestOptions: ApiRequest.Options
    )

    /**
     * Decide whether [handlePaymentResult] should be called.
     */
    fun shouldHandlePaymentResult(requestCode: Int, data: Intent?): Boolean

    /**
     * Decide whether [handleSetupResult] should be called.
     */
    fun shouldHandleSetupResult(requestCode: Int, data: Intent?): Boolean

    fun shouldHandleSourceResult(requestCode: Int, data: Intent?): Boolean

    /**
     * If payment authentication triggered an exception, get the exception object and pass to
     * [ApiResultCallback.onError].
     *
     * Otherwise, get the PaymentIntent's client_secret from {@param data} and use to retrieve
     * the PaymentIntent object with updated status.
     *
     * @param data the result Intent
     */
    fun handlePaymentResult(
        data: Intent,
        callback: ApiResultCallback<PaymentIntentResult>
    )

    /**
     * If setup authentication triggered an exception, get the exception object and pass to
     * [ApiResultCallback.onError].
     *
     * Otherwise, get the SetupIntent's client_secret from {@param data} and use to retrieve the
     * SetupIntent object with updated status.
     *
     * @param data the result Intent
     */
    fun handleSetupResult(
        data: Intent,
        callback: ApiResultCallback<SetupIntentResult>
    )

    fun handleSourceResult(
        data: Intent,
        callback: ApiResultCallback<Source>
    )

    /**
     * Determine which authentication mechanism should be used, or bypass authentication
     * if it is not needed.
     */
    fun handleNextAction(
        host: AuthActivityStarter.Host,
        stripeIntent: StripeIntent,
        requestOptions: ApiRequest.Options
    )

    fun authenticateAlipay(
        intent: StripeIntent,
        stripeAccountId: String?,
        authenticator: AlipayAuthenticator,
        callback: ApiResultCallback<PaymentIntentResult>
    )

    enum class StripeIntentType {
        PaymentIntent,
        SetupIntent
    }
}
