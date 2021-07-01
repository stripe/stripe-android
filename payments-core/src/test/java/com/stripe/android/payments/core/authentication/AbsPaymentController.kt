package com.stripe.android.payments.core.authentication

import android.content.Intent
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import com.stripe.android.AlipayAuthenticator
import com.stripe.android.PaymentController
import com.stripe.android.PaymentIntentResult
import com.stripe.android.SetupIntentResult
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.Source
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.WeChatPayNextAction
import com.stripe.android.networking.ApiRequest
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.view.AuthActivityStarterHost

internal abstract class AbsPaymentController : PaymentController {
    override suspend fun startConfirmAndAuth(
        host: AuthActivityStarterHost,
        confirmStripeIntentParams: ConfirmStripeIntentParams,
        requestOptions: ApiRequest.Options
    ) {
    }

    override suspend fun confirmAndAuthenticateAlipay(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        authenticator: AlipayAuthenticator,
        requestOptions: ApiRequest.Options
    ): PaymentIntentResult {
        TODO("Not yet implemented")
    }

    override suspend fun confirmWeChatPay(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        requestOptions: ApiRequest.Options
    ): WeChatPayNextAction {
        TODO("Not yet implemented")
    }

    override suspend fun startAuth(
        host: AuthActivityStarterHost,
        clientSecret: String,
        requestOptions: ApiRequest.Options,
        type: PaymentController.StripeIntentType
    ) {
    }

    override suspend fun startAuthenticateSource(
        host: AuthActivityStarterHost,
        source: Source,
        requestOptions: ApiRequest.Options
    ) {
    }

    override fun shouldHandlePaymentResult(requestCode: Int, data: Intent?): Boolean {
        return false
    }

    override fun shouldHandleSetupResult(requestCode: Int, data: Intent?): Boolean {
        return false
    }

    override fun shouldHandleSourceResult(requestCode: Int, data: Intent?): Boolean {
        return false
    }

    override suspend fun getPaymentIntentResult(data: Intent): PaymentIntentResult {
        TODO("Not yet implemented")
    }

    override suspend fun getSetupIntentResult(data: Intent): SetupIntentResult {
        TODO("Not yet implemented")
    }

    override suspend fun getAuthenticateSourceResult(data: Intent): Source {
        TODO("Not yet implemented")
    }

    override suspend fun handleNextAction(
        host: AuthActivityStarterHost,
        stripeIntent: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
    }

    override fun registerLaunchersWithActivityResultCaller(
        activityResultCaller: ActivityResultCaller,
        activityResultCallback: ActivityResultCallback<PaymentFlowResult.Unvalidated>
    ) {
    }

    override fun unregisterLaunchers() {
    }
}
