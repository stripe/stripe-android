package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject

/**
 * The Payment Method Options model.
 * PaymentIntent PMO: https://stripe.com/docs/api/payment_intents/object#payment_intent_object-payment_method_options
 * SetupIntent PMO: https://stripe.com/docs/api/setup_intents/object#setup_intent_object-payment_method_options
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
@Parcelize
data class PaymentMethodOptions(
    @SerialName("setup_future_usage")
    val setupFutureUsage: SetupFutureUsage? = null,

    @SerialName("verification_method")
    val verificationMethod: VerificationMethod? = null
) : StripeModel {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Serializable
    enum class SetupFutureUsage(val value: String) {
        @SerialName("on_session")
        OnSession("on_session"),

        @SerialName("off_session")
        OffSession("off_session"),

        @SerialName("none")
        None("none")
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Serializable
    enum class VerificationMethod(val value: String) {
        @SerialName("automatic")
        Automatic("automatic"),

        @SerialName("instant")
        Instant("instant"),

        @SerialName("microdeposits")
        Microdeposits("microdeposits")
    }
}

internal fun Map<String, PaymentMethodOptions>.toJson(): JSONObject {
    return JSONObject(Json.encodeToString(this))
}
