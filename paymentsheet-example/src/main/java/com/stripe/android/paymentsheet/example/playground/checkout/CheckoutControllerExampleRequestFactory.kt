package com.stripe.android.paymentsheet.example.playground.checkout

import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundDefinitions
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundSettings
import com.stripe.android.paymentsheet.example.playground.checkout.settings.values
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal data class CheckoutControllerExampleRequest(
    val endpoint: String,
    val body: JsonObject,
)

internal object CheckoutControllerExampleRequestFactory {
    fun create(settings: CheckoutPlaygroundSettings.Snapshot): CheckoutControllerExampleRequest {
        return CheckoutControllerExampleRequest(
            endpoint = "checkout_session",
            body = buildJsonObject {
                CheckoutPlaygroundDefinitions.root.values()
                    .filter { definition -> definition.isApplicable(settings) }
                    .forEach { definition ->
                        definition.updateRequest(
                            request = this,
                            settings = settings,
                        )
                    }
                put("mode", "unified")
                put("use_one_time_price", true)
            },
        )
    }
}
