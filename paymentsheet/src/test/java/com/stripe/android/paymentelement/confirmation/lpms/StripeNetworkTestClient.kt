package com.stripe.android.paymentelement.confirmation.lpms

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Deserializable
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.awaitResult
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.networking.StripeRepository
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import javax.inject.Inject
import com.github.kittinunf.result.Result as ApiResult

internal class StripeNetworkTestClient @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val requestOptions: ApiRequest.Options,
) {
    suspend fun createPaymentIntent(
        amount: Int,
        currency: String,
        paymentMethodTypes: List<String>,
        paymentMethodId: String? = null,
        createWithSetupFutureUsage: Boolean = false,
    ): Result<String> {
        val result = executeFuelRequest(
            path = PAYMENT_INTENT_PATH,
            request = CreatePaymentIntentRequest(
                createParams = CreatePaymentIntentRequest.CreateParams(
                    amount = amount,
                    currency = currency,
                    paymentMethodTypes = paymentMethodTypes,
                    confirm = false,
                    paymentMethodId = paymentMethodId,
                    paymentMethodOptions = CreatePaymentIntentRequest.CreateParams.PaymentMethodOptions(
                        card = CreatePaymentIntentRequest.CreateParams.PaymentMethodOptions.Card(
                            setupFutureUsage = CreatePaymentIntentRequest
                                .CreateParams
                                .PaymentMethodOptions
                                .SetupFutureUsage
                                .OffSession
                        )
                    ).takeIf {
                        createWithSetupFutureUsage
                    },
                ),
                account = null,
                version = STRIPE_VERSION,
            ),
            requestSerializer = CreatePaymentIntentRequest.serializer(),
            responseDeserializer = CreatePaymentIntentResponse.serializer(),
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
        paymentMethodTypes: List<String>,
        paymentMethodId: String? = null,
    ): Result<String> {
        val result = executeFuelRequest(
            path = SETUP_INTENT_PATH,
            request = CreateSetupIntentRequest(
                createParams = CreateSetupIntentRequest.CreateParams(
                    paymentMethodTypes = paymentMethodTypes,
                    paymentMethodId = paymentMethodId,
                    confirm = false,
                ),
                account = null,
                version = STRIPE_VERSION,
            ),
            requestSerializer = CreateSetupIntentRequest.serializer(),
            responseDeserializer = CreateSetupIntentResponse.serializer(),
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
            confirmPaymentIntentParams = confirmParams.withShouldUseStripeSdk(shouldUseStripeSdk = true),
            options = requestOptions,
        )
    }

    suspend fun confirmSetupIntent(confirmParams: ConfirmSetupIntentParams): Result<SetupIntent> {
        return stripeRepository.confirmSetupIntent(
            confirmSetupIntentParams = confirmParams.withShouldUseStripeSdk(shouldUseStripeSdk = true),
            options = requestOptions,
        )
    }

    private suspend fun <T : Any, R : Any> executeFuelRequest(
        path: String,
        request: T,
        requestSerializer: SerializationStrategy<T>,
        responseDeserializer: DeserializationStrategy<R>
    ): Result<R> {
        return Fuel.post(BACKEND_URL + path)
            .jsonBody(
                Json.encodeToString(
                    requestSerializer,
                    request,
                )
            )
            .suspendable()
            .awaitModel(responseDeserializer)
            .fold(
                success = { response ->
                    Result.success(response)
                },
                failure = { error ->
                    Result.failure(error)
                }
            )
    }

    private suspend fun <T : Any> Request.awaitModel(
        serializer: DeserializationStrategy<T>
    ): ApiResult<T, FuelError> {
        val deserializer = object : Deserializable<T> {

            override fun deserialize(response: Response): T {
                val body = response.body().asString(CONTENT_TYPE)
                return Json.decodeFromString(serializer, body)
            }
        }

        return awaitResult(deserializer)
    }

    private companion object {
        const val CONTENT_TYPE = "application/json"

        const val STRIPE_VERSION = "2020-03-02"
        const val BACKEND_URL = "https://stp-mobile-ci-test-backend-e1b3.stripedemos.com/"

        const val PAYMENT_INTENT_PATH = "create_payment_intent"
        const val SETUP_INTENT_PATH = "create_setup_intent"
    }
}
