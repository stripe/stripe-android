package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.ConfirmationToken
import com.stripe.android.model.PaymentMethod
import org.json.JSONObject

/**
 * Parser for [ConfirmationToken.PaymentMethodPreview] JSON objects from the Stripe API.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PaymentMethodPreviewJsonParser : ModelJsonParser<ConfirmationToken.PaymentMethodPreview> {
    override fun parse(json: JSONObject): ConfirmationToken.PaymentMethodPreview? {
        val type = StripeJsonUtils.optString(json, FIELD_TYPE)?.let { code ->
            PaymentMethod.Type.fromCode(code)
        } ?: return null // Return null if type is missing

        return ConfirmationToken.PaymentMethodPreview(
            allowRedisplay = StripeJsonUtils.optString(json, FIELD_ALLOW_REDISPLAY)?.let { allowRedisplayRawValue ->
                PaymentMethod.AllowRedisplay.entries.find { entry ->
                    allowRedisplayRawValue == entry.value
                }
            },
            billingDetails = json.optJSONObject(FIELD_BILLING_DETAILS)?.let {
                PaymentMethodJsonParser.BillingDetails().parse(it)
            },
            customerId = StripeJsonUtils.optString(json, FIELD_CUSTOMER),
            type = type,
            allResponseFields = json.toString()
        )
    }

    private companion object {
        const val FIELD_ALLOW_REDISPLAY = "allow_redisplay"
        const val FIELD_BILLING_DETAILS = "billing_details"
        const val FIELD_CUSTOMER = "customer"
        const val FIELD_TYPE = "type"
    }
}
