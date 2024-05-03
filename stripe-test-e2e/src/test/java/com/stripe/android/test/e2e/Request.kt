package com.stripe.android.test.e2e

import com.squareup.moshi.Json
import com.stripe.android.core.ApiVersion

sealed class Request {
    data class CreatePaymentIntentParams(
        @field:Json(name = "create_params") val createParams: CreateParams,
        @field:Json(name = "account") val account: String? = null,
        @field:Json(name = "version") val version: String = ApiVersion.API_VERSION_CODE
    )

    data class CreateSetupIntentParams(
        @field:Json(name = "create_params") val createParams: CreateParams,
        @field:Json(name = "account") val account: String? = null,
        @field:Json(name = "version") val version: String = ApiVersion.API_VERSION_CODE
    )

    data class CreateParams(
        @field:Json(name = "payment_method_types") val paymentMethodTypes: List<String>,
    )

    data class CreateEphemeralKeyParams(
        @field:Json(name = "account") val account: String,
    )
}
