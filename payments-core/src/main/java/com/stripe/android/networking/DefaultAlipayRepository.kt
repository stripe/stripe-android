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
    ): Result<AlipayAuthResult> {
        if (paymentIntent.paymentMethod?.liveMode == false) {
            return Result.failure(
                IllegalArgumentException(
                    "Attempted to authenticate test mode " +
                        "PaymentIntent with the Alipay SDK.\n" +
                        "The Alipay SDK does not support test mode payments."
                )
            )
        }

        val nextActionData = paymentIntent.nextActionData
        return if (nextActionData is StripeIntent.NextActionData.AlipayRedirect) {
            val output = authenticator.onAuthenticationRequest(nextActionData.data)
            val result = AlipayAuthResult(
                when (output[ALIPAY_RESULT_FIELD]) {
                    AlipayAuthResult.RESULT_CODE_SUCCESS -> {
                        nextActionData.authCompleteUrl?.let { authCompleteUrl ->
                            stripeRepository.retrieveObject(authCompleteUrl, requestOptions)
                        }
                        StripeIntentResult.Outcome.SUCCEEDED
                    }
                    AlipayAuthResult.RESULT_CODE_FAILED -> StripeIntentResult.Outcome.FAILED
                    AlipayAuthResult.RESULT_CODE_CANCELLED -> StripeIntentResult.Outcome.CANCELED
                    else -> StripeIntentResult.Outcome.UNKNOWN
                }
            )
            Result.success(result)
        } else {
            Result.failure(
                RuntimeException("Unable to authenticate Payment Intent with Alipay SDK")
            )
        }
    }

    private companion object {
        private const val ALIPAY_RESULT_FIELD = "resultStatus"
    }
}
