package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.StripeJsonUtils.optBoolean
import com.stripe.android.core.model.StripeJsonUtils.optLong
import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.CardBrand
import com.stripe.android.model.LinkBankPaymentMethod
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object PaymentMethodLikeObjectJsonParser : ModelJsonParser<LinkBankPaymentMethod> {

    override fun parse(json: JSONObject): LinkBankPaymentMethod? {
        val paymentMethodId = optString(json, "id") ?: return null
        val allowRedisplay = optString(json, "allow_redisplay")
        val created = optLong(json, "created")
        val customer = optString(json, "customer")
        val livemode = optBoolean(json, "livemode")
        val billingDetails = json.optJSONObject("billing_details")
        val card = json.optJSONObject("card")

        return LinkBankPaymentMethod(
            id = paymentMethodId,
            allowRedisplay = allowRedisplay,
            billingDetails = billingDetails?.let { parseBillingDetails(it) },
            card = card?.let { parseCard(it) },
            created = created,
            customer = customer,
            livemode = livemode,
        )
    }

    private fun parseBillingDetails(json: JSONObject): LinkBankPaymentMethod.BillingDetails {
        val name = optString(json, "name")
        val email = optString(json, "email")
        val phone = optString(json, "phone")

        val address = json.optJSONObject("address")?.let { address ->
            LinkBankPaymentMethod.Address(
                line1 = optString(address, "line1"),
                line2 = optString(address, "line2"),
                city = optString(address, "city"),
                postalCode = optString(address, "postal_code"),
                state = optString(address, "state"),
                country = optString(address, "country"),
            )
        }

        return LinkBankPaymentMethod.BillingDetails(
            address = address,
            email = email,
            name = name,
            phone = phone,
        )
    }

    private fun parseCard(json: JSONObject): LinkBankPaymentMethod.Card {
        return LinkBankPaymentMethod.Card(
            brand = CardBrand.fromCode(optString(json, "brand")),
            checks = json.optJSONObject("checks")?.let {
                LinkBankPaymentMethod.Card.Checks(
                    addressLine1Check = optString(it, "address_line1_check"),
                    addressPostalCodeCheck = optString(it, "address_postal_code_check"),
                    cvcCheck = optString(it, "cvc_check"),
                )
            },
            country = optString(json, "country"),
            expiryMonth = StripeJsonUtils.optInteger(json, "exp_month"),
            expiryYear = StripeJsonUtils.optInteger(json, "exp_year"),
            fingerprint = optString(json, "fingerprint"),
            funding = optString(json, "funding"),
            last4 = optString(json, "last4"),
            threeDSecureUsage = json.optJSONObject("three_d_secure_usage")?.let {
                LinkBankPaymentMethod.Card.ThreeDSecureUsage(
                    isSupported = optBoolean(it, "supported"),
                )
            },
            networks = json.optJSONObject("networks")?.let {
                val available = StripeJsonUtils.jsonArrayToList(json.optJSONArray("available"))
                    .orEmpty()
                    .map { it.toString() }
                    .toSet()
                LinkBankPaymentMethod.Card.Networks(
                    available = available,
                    preferred = optString(json, "preferred")
                )
            },
        )
    }
}
