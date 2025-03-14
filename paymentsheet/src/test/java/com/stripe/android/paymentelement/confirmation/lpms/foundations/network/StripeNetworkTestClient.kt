package com.stripe.android.paymentelement.confirmation.lpms.foundations.network

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.networking.StripeRepository
import javax.inject.Inject

internal class StripeNetworkTestClient @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val requestOptions: ApiRequest.Options,
) {
    suspend fun createPaymentIntent(
        country: MerchantCountry,
        amount: Int,
        currency: String,
        paymentMethodType: PaymentMethod.Type,
        paymentMethodId: String? = null,
        createWithSetupFutureUsage: Boolean = false,
    ): Result<String> {
        val result = executeFuelPostRequest(
            url = createUrl(PAYMENT_INTENT_PATH),
            request = CreatePaymentIntentRequest(
                createParams = CreatePaymentIntentRequest.CreateParams(
                    amount = amount,
                    currency = currency,
                    paymentMethodTypes = listOf(paymentMethodType.code),
                    confirm = false,
                    paymentMethodId = paymentMethodId,
                    setupFutureUsage = CreatePaymentIntentRequest.CreateParams.SetupFutureUsage.OffSession.takeIf {
                        createWithSetupFutureUsage
                    },
                ),
                country = country,
                version = STRIPE_VERSION,
            ),
            requestSerializer = CreatePaymentIntentRequest.serializer(),
            responseDeserializer = CreatePaymentIntentRequest.Response.serializer(),
        )

        return result.mapCatching { response ->
            response.intentClientSecret
        }
    }

    suspend fun retrievePaymentIntent(clientSecret: String): Result<PaymentIntent> {
        return stripeRepository.retrievePaymentIntent(
            clientSecret = clientSecret,
            options = requestOptions,
        )
    }

    suspend fun createSetupIntent(
        country: MerchantCountry,
        paymentMethodType: PaymentMethod.Type,
        paymentMethodId: String? = null,
    ): Result<String> {
        val result = executeFuelPostRequest(
            url = createUrl(SETUP_INTENT_PATH),
            request = CreateSetupIntentRequest(
                createParams = CreateSetupIntentRequest.CreateParams(
                    paymentMethodTypes = listOf(paymentMethodType.code),
                    paymentMethodId = paymentMethodId,
                    confirm = false,
                ),
                country = country,
                version = STRIPE_VERSION,
            ),
            requestSerializer = CreateSetupIntentRequest.serializer(),
            responseDeserializer = CreateSetupIntentRequest.Response.serializer(),
        )

        return result.map { response ->
            response.intentClientSecret
        }
    }

    suspend fun retrieveSetupIntent(clientSecret: String): Result<SetupIntent> {
        return stripeRepository.retrieveSetupIntent(
            clientSecret = clientSecret,
            options = requestOptions,
        )
    }

    suspend fun confirmPaymentIntent(confirmParams: ConfirmPaymentIntentParams): Result<PaymentIntent> {
        return stripeRepository.confirmPaymentIntent(
            confirmPaymentIntentParams = confirmParams
                .withShouldUseStripeSdk(shouldUseStripeSdk = true)
                .withReturnUrl(),
            options = requestOptions,
        )
    }

    suspend fun confirmSetupIntent(confirmParams: ConfirmSetupIntentParams): Result<SetupIntent> {
        return stripeRepository.confirmSetupIntent(
            confirmSetupIntentParams = confirmParams
                .withShouldUseStripeSdk(shouldUseStripeSdk = true)
                .withReturnUrl(),
            options = requestOptions,
        )
    }

    private fun createUrl(path: String): String {
        return STRIPE_CI_TEST_BACKEND_URL + path
    }

    private fun <T : ConfirmStripeIntentParams> T.withReturnUrl() = apply {
        // We don't need a real return URL since we won't be launching into the payment flow
        returnUrl = DEFAULT_RETURN_URL
    }

    private companion object {
        const val STRIPE_VERSION = "2020-03-02"

        const val PAYMENT_INTENT_PATH = "create_payment_intent"
        const val SETUP_INTENT_PATH = "create_setup_intent"

        const val DEFAULT_RETURN_URL = "stripesdk://android_lpm_tests"
    }
}
