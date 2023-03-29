package com.stripe.android.paymentsheet.example.samples.networking

import androidx.annotation.Keep
import com.github.kittinunf.fuel.core.Deserializable
import com.github.kittinunf.fuel.core.Response
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.samples.model.CartProduct
import com.stripe.android.paymentsheet.example.samples.model.CartState
import kotlinx.serialization.Serializable

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

    object Deserializer : Deserializable<ExampleCheckoutResponse> {

        override fun deserialize(response: Response): ExampleCheckoutResponse {
            val body = response.body().asString("application/json")
            return Gson().fromJson(body, ExampleCheckoutResponse::class.java)
        }
    }
}
