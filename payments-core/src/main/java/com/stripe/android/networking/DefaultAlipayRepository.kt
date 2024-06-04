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

        if (nextActionData is StripeIntent.NextActionData.AlipayRedirect) {
            val output = authenticator.onAuthenticationRequest(nextActionData.data)
            val result = output[ALIPAY_RESULT_FIELD]

            if (result == AlipayAuthResult.RESULT_CODE_SUCCESS) {
                pingAlipayEndpointBeforeRetrievingPaymentIntentStatus(nextActionData, requestOptions)
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
        redirect: StripeIntent.NextActionData.AlipayRedirect,
        requestOptions: ApiRequest.Options,
    ) {
        // Alipay requires us to hit an endpoint before retrieving the PaymentIntent to ensure the
        // status is up-to-date.
        if (redirect.authCompleteUrl != null) {
            stripeRepository.retrieveObject(redirect.authCompleteUrl, requestOptions)
        }
    }

    private companion object {
        private const val ALIPAY_RESULT_FIELD = "resultStatus"
    }
}
