package com.stripe.android.stripecardscan.framework.api.dto

import androidx.annotation.RestrictTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class ModelDetailsRequest(
    @SerialName("platform") val platform: String,
    @SerialName("model_class") val modelClass: String,
    @SerialName("model_framework_version") val modelFrameworkVersion: Int,
    @SerialName("cached_model_hash") val cachedModelHash: String?,
    @SerialName("cached_model_hash_algorithm") val cachedModelHashAlgorithm: String?,
    @SerialName("beta_opt_in") val betaOptIn: Boolean?
)

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class ModelDetailsResponse(
    @SerialName("model_url") val url: String?,
    @SerialName("model_version") val modelVersion: String,
    @SerialName("model_hash") val hash: String,
    @SerialName("model_hash_algorithm") val hashAlgorithm: String,
    @SerialName("query_again_after_ms") val queryAgainAfterMs: Long? = 0
)
