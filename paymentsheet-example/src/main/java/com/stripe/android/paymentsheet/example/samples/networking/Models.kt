package com.stripe.android.paymentsheet.example.samples.networking

import android.util.Log
import com.github.kittinunf.fuel.core.Deserializable
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.awaitResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.samples.model.CartProduct
import com.stripe.android.paymentsheet.example.samples.model.CartState
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.github.kittinunf.result.Result as ApiResult

@Serializable
data class ExamplePublishableKeyResponse(
    @SerialName("publishable_key")
    val publishableKey: String,
)

@Serializable
data class ExampleCheckoutRequest(
    @SerialName("hot_dog_count")
    val hotDogCount: Int,
    @SerialName("salad_count")
    val saladCount: Int,
    @SerialName("is_subscribing")
    val isSubscribing: Boolean,
)

fun CartState.toCheckoutRequest(): ExampleCheckoutRequest {
    return ExampleCheckoutRequest(
        hotDogCount = countOf(CartProduct.Id.HotDog),
        saladCount = countOf(CartProduct.Id.Salad),
        isSubscribing = isSubscription,
    )
}

@Serializable
data class ExampleCheckoutResponse(
    @SerialName("publishableKey")
    val publishableKey: String,
    @SerialName("paymentIntent")
    val paymentIntent: String = "",
    @SerialName("customer")
    val customer: String? = null,
    @SerialName("ephemeralKey")
    val ephemeralKey: String? = null,
    @SerialName("subtotal")
    val subtotal: Long = 0,
    @SerialName("tax")
    val tax: Long = 0,
    @SerialName("total")
    val total: Long = 0,
) {

    internal fun makeCustomerConfig() = if (customer != null && ephemeralKey != null) {
        PaymentSheet.CustomerConfiguration(
            id = customer,
            ephemeralKeySecret = ephemeralKey
        )
    } else {
        null
    }
}

@Serializable
data class ExampleUpdateRequest(
    @SerialName("hot_dog_count")
    val hotDogCount: Int,
    @SerialName("salad_count")
    val saladCount: Int,
    @SerialName("is_subscribing")
    val isSubscribing: Boolean,
)

fun CartState.toUpdateRequest(): ExampleUpdateRequest {
    return ExampleUpdateRequest(
        hotDogCount = countOf(CartProduct.Id.HotDog),
        saladCount = countOf(CartProduct.Id.Salad),
        isSubscribing = isSubscription,
    )
}

@Serializable
data class ExampleUpdateResponse(
    @SerialName("subtotal")
    val subtotal: Long,
    @SerialName("tax")
    val tax: Long,
    @SerialName("total")
    val total: Long,
)

@Serializable
data class ExampleCreateAndConfirmIntentRequest(
    @SerialName("payment_method_id")
    val paymentMethodId: String,
    @SerialName("should_save_payment_method")
    val shouldSavePaymentMethod: Boolean,
    @SerialName("currency")
    val currency: String,
    @SerialName("hot_dog_count")
    val hotDogCount: Int,
    @SerialName("salad_count")
    val saladCount: Int,
    @SerialName("is_subscribing")
    val isSubscribing: Boolean,
    @SerialName("return_url")
    val returnUrl: String,
    @SerialName("customer_id")
    val customerId: String?,
)

fun CartState.toCreateIntentRequest(
    paymentMethodId: String,
    shouldSavePaymentMethod: Boolean,
    returnUrl: String,
): ExampleCreateAndConfirmIntentRequest {
    return ExampleCreateAndConfirmIntentRequest(
        paymentMethodId = paymentMethodId,
        shouldSavePaymentMethod = shouldSavePaymentMethod,
        currency = "usd",
        hotDogCount = countOf(CartProduct.Id.HotDog),
        saladCount = countOf(CartProduct.Id.Salad),
        isSubscribing = isSubscription,
        returnUrl = returnUrl,
        customerId = customerId,
    )
}

@Serializable
data class ExampleCreateAndConfirmIntentResponse(
    @SerialName("intentClientSecret")
    val clientSecret: String,
)

@Serializable
data class ExampleCreateAndConfirmErrorResponse(
    @SerialName("error")
    val error: String? = null,
) {
    companion object {

        @Suppress("TooGenericExceptionCaught")
        fun deserialize(response: Response): ExampleCreateAndConfirmErrorResponse {
            return try {
                val body = response.body().asString("application/json")
                Json.decodeFromString(
                    serializer(),
                    body
                )
            } catch (ex: Exception) {
                Log.e("STRIPE", ex.toString())
                ExampleCreateAndConfirmErrorResponse(
                    error = "Something went wrong"
                )
            }
        }
    }
}

@Serializable
data class ExampleCustomerSheetRequest(
    @SerialName("customer_type")
    val customerType: String
)

@Serializable
data class ExampleCustomerSheetResponse(
    @SerialName("publishableKey")
    val publishableKey: String,
    @SerialName("customerEphemeralKeySecret")
    val customerEphemeralKeySecret: String,
    @SerialName("customerId")
    val customerId: String
)

@Serializable
data class PlaygroundCustomerSheetRequest(
    @SerialName("customer")
    val customerId: String,
    @SerialName("mode")
    val mode: String,
    @SerialName("merchant_country_code")
    val merchantCountryCode: String,
    @SerialName("currency")
    val currency: String,
)

@Serializable
data class PlaygroundCustomerSheetResponse(
    @SerialName("customerId")
    val customerId: String,
    @SerialName("intentClientSecret")
    val clientSecret: String,
    @SerialName("publishableKey")
    val publishableKey: String,
    @SerialName("customerEphemeralKeySecret")
    val customerEphemeralKeySecret: String,
    @SerialName("amount")
    val amount: String,
    @SerialName("paymentMethodTypes")
    val paymentMethodTypes: String,
)

@Serializable
data class ExampleCreateSetupIntentRequest(
    @SerialName("customer_id")
    val customerId: String,
)

@Serializable
data class ExampleCreateSetupIntentResponse(
    @SerialName("client_secret")
    val clientSecret: String,
)

/**
 * Awaits the [ApiResult] and deserializes it into the desired type [T].
 */
suspend fun <T : Any> Request.awaitModel(
    serializer: DeserializationStrategy<T>
): ApiResult<T, FuelError> {
    val deserializer = object : Deserializable<T> {

        override fun deserialize(response: Response): T {
            val body = response.body().asString("application/json")
            return Json.decodeFromString(serializer, body)
        }
    }

    return awaitResult(deserializer)
}
