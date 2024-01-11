package com.stripe.android.testing

import android.content.Intent
import com.stripe.android.AlipayAuthenticator
import com.stripe.android.PaymentController
import com.stripe.android.PaymentIntentResult
import com.stripe.android.SetupIntentResult
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.Source
import com.stripe.android.model.WeChatPayNextAction

abstract class AbsPaymentController : PaymentController {

    override suspend fun confirmAndAuthenticateAlipay(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        authenticator: AlipayAuthenticator,
        requestOptions: ApiRequest.Options
    ): Result<PaymentIntentResult> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun confirmWeChatPay(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        requestOptions: ApiRequest.Options
    ): Result<WeChatPayNextAction> {
        return Result.failure(NotImplementedError())
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

    override suspend fun getPaymentIntentResult(data: Intent): Result<PaymentIntentResult> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun getSetupIntentResult(data: Intent): Result<SetupIntentResult> {
        return Result.failure(NotImplementedError())
    }

    override suspend fun getAuthenticateSourceResult(data: Intent): Result<Source> {
        return Result.failure(NotImplementedError())
    }
}
