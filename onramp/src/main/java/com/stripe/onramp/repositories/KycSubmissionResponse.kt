package com.stripe.onramp.repositories

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import com.stripe.android.core.model.parsers.ModelJsonParser
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

/**
 * Response model for KYC submission.
 */
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class KycSubmissionResponse(
    val success: Boolean = true
) : StripeModel {

    internal companion object {
        /**
         * JSON parser for [KycSubmissionResponse].
         */
        val JsonParser: ModelJsonParser<KycSubmissionResponse> = object : ModelJsonParser<KycSubmissionResponse> {
            override fun parse(json: JSONObject): KycSubmissionResponse {
                return KycSubmissionResponse(
                    success = json.optBoolean("success", true)
                )
            }
        }
    }
}
