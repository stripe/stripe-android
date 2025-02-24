package com.stripe.android.test.e2e

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.stripe.android.core.ApiVersion

sealed class Request {
    @JsonClass(generateAdapter = true)
    data class CreatePaymentIntentParams(
        @field:Json(name = "create_params") val createParams: CreateParams,
        @field:Json(name = "account") val account: String? = null,
        @field:Json(name = "version") val version: String = ApiVersion.API_VERSION_CODE
    )

    @JsonClass(generateAdapter = true)
    data class CreateSetupIntentParams(
        @field:Json(name = "create_params") val createParams: CreateParams,
        @field:Json(name = "account") val account: String? = null,
        @field:Json(name = "version") val version: String = ApiVersion.API_VERSION_CODE
    )

    @JsonClass(generateAdapter = true)
    data class CreateParams(
        @field:Json(name = "payment_method_types") val paymentMethodTypes: List<String>,
    )

    @JsonClass(generateAdapter = true)
    data class CreateEphemeralKeyParams(
        @field:Json(name = "account") val account: String,
    )
}
