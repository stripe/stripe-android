package com.stripe.android.networking

import com.stripe.android.AlipayAuthenticator
import com.stripe.android.StripeIntentResult
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.AlipayAuthResult
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent

internal class DefaultAlipayRepository(
    private val stripeRepository: StripeRepository
) : AlipayRepository {

    override suspend fun authenticate(
        paymentIntent: PaymentIntent,
        authenticator: AlipayAuthenticator,
        requestOptions: ApiRequest.Options
    ): AlipayAuthResult {
        if (paymentIntent.paymentMethod?.liveMode == false) {
            throw IllegalArgumentException(
                "Attempted to authenticate test mode " +
                    "PaymentIntent with the Alipay SDK.\n" +
                    "The Alipay SDK does not support test mode payments."
            )
        }

        val nextActionData = paymentIntent.nextActionData

        if (
            nextActionData is StripeIntent.NextActionData.AlipayRedirect &&
            nextActionData.type is StripeIntent.NextActionData.AlipayRedirect.Type.WithNativeData
        ) {
            val output = authenticator.onAuthenticationRequest(nextActionData.type.data)
            val result = output[ALIPAY_RESULT_FIELD]

            if (result == AlipayAuthResult.RESULT_CODE_SUCCESS) {
                pingAlipayEndpointBeforeRetrievingPaymentIntentStatus(nextActionData.type, requestOptions)
            }

            val outcome = when (output[ALIPAY_RESULT_FIELD]) {
                AlipayAuthResult.RESULT_CODE_SUCCESS -> StripeIntentResult.Outcome.SUCCEEDED
                AlipayAuthResult.RESULT_CODE_FAILED -> StripeIntentResult.Outcome.FAILED
                AlipayAuthResult.RESULT_CODE_CANCELLED -> StripeIntentResult.Outcome.CANCELED
                else -> StripeIntentResult.Outcome.UNKNOWN
            }

            return AlipayAuthResult(outcome)
        } else {
            throw RuntimeException("Unable to authenticate Payment Intent with Alipay SDK")
        }
    }

    private suspend fun pingAlipayEndpointBeforeRetrievingPaymentIntentStatus(
        withNativeData: StripeIntent.NextActionData.AlipayRedirect.Type.WithNativeData,
        requestOptions: ApiRequest.Options,
    ) {
        val authCompleteUrl = withNativeData.authCompleteUrl

        // Alipay requires us to hit an endpoint before retrieving the PaymentIntent to ensure the
        // status is up-to-date.
        if (authCompleteUrl != null) {
            stripeRepository.retrieveObject(authCompleteUrl, requestOptions)
        }
    }

    private companion object {
        private const val ALIPAY_RESULT_FIELD = "resultStatus"
    }
}
