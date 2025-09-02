package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JsonIgnoreUnknownKeys
internal data class CryptoCustomerResponse(
    val id: String
)
