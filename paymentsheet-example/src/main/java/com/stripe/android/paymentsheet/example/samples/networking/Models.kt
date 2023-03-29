package com.stripe.android.paymentsheet.example.samples.networking

import androidx.annotation.Keep
import com.github.kittinunf.fuel.core.Deserializable
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.awaitResult
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.samples.model.CartProduct
import com.stripe.android.paymentsheet.example.samples.model.CartState
import kotlinx.serialization.Serializable
import com.github.kittinunf.result.Result as ApiResult

@Serializable
@Keep
data class ExampleCheckoutRequest(
    @SerializedName("hot_dog_count")
    val hotDogCount: Int,
    @SerializedName("salad_count")
    val saladCount: Int,
    @SerializedName("is_subscribing")
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
@Keep
data class ExampleCheckoutResponse(
    @SerializedName("publishableKey")
    val publishableKey: String,
    @SerializedName("paymentIntent")
    val paymentIntent: String,
    @SerializedName("customer")
    val customer: String? = null,
    @SerializedName("ephemeralKey")
    val ephemeralKey: String? = null,
    @SerializedName("subtotal")
    val subtotal: Float,
    @SerializedName("tax")
    val tax: Float,
    @SerializedName("total")
    val total: Float,
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
@Keep
data class ExampleUpdateRequest(
    @SerializedName("hot_dog_count")
    val hotDogCount: Int,
    @SerializedName("salad_count")
    val saladCount: Int,
    @SerializedName("is_subscribing")
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
@Keep
data class ExampleUpdateResponse(
    @SerializedName("subtotal")
    val subtotal: Float,
    @SerializedName("tax")
    val tax: Float,
    @SerializedName("total")
    val total: Float,
)

@Serializable
@Keep
data class ExampleCreateAndConfirmIntentRequest(
    @SerializedName("payment_method_id")
    val paymentMethodId: String,
    @SerializedName("should_save_payment_method")
    val shouldSavePaymentMethod: Boolean,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("hot_dog_count")
    val hotDogCount: Int,
    @SerializedName("salad_count")
    val saladCount: Int,
    @SerializedName("is_subscribing")
    val isSubscribing: Boolean,
    @SerializedName("return_url")
    val returnUrl: String,
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
    )
}

@Serializable
@Keep
data class ExampleCreateAndConfirmIntentResponse(
    @SerializedName("intentClientSecret")
    val clientSecret: String,
)

/**
 * Awaits the [ApiResult] and deserializes it into the desired type [T].
 */
suspend inline fun <reified T : Any> Request.awaitModel(): ApiResult<T, FuelError> {
    val deserializer = object : Deserializable<T> {

        override fun deserialize(response: Response): T {
            val body = response.body().asString("application/json")
            return Gson().fromJson(body, T::class.java)
        }
    }

    return awaitResult(deserializer)
}
