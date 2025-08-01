package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.ModelJsonParser.Companion.jsonArrayToList
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.CvcCheck
import org.json.JSONObject

private const val FIELD_PAYMENT_DETAILS = "redacted_payment_details"
private const val FIELD_TYPE = "type"
private const val FIELD_ID = "id"
private const val FIELD_CARD_DETAILS = "card_details"
private const val FIELD_CARD_LAST_4 = "last4"
private const val FIELD_BANK_ACCOUNT_DETAILS = "bank_account_details"
private const val FIELD_BANK_ACCOUNT_LAST_4 = "last4"
private const val FIELD_BANK_ACCOUNT_BANK_NAME = "bank_name"

private const val FIELD_BILLING_ADDRESS = "billing_address"
private const val FIELD_BILLING_EMAIL_ADDRESS = "billing_email_address"
private const val FIELD_ADDRESS_COUNTRY_CODE = "country_code"
private const val FIELD_ADDRESS_POSTAL_CODE = "postal_code"
private const val FIELD_ADDRESS_NAME = "name"
private const val FIELD_ADDRESS_LINE_1 = "line_1"
private const val FIELD_ADDRESS_LINE_2 = "line_2"
private const val FIELD_ADDRESS_LOCALITY = "locality"
private const val FIELD_ADDRESS_ADMINISTRATIVE_AREA = "administrative_area"

private const val FIELD_CARD_EXPIRY_YEAR = "exp_year"
private const val FIELD_CARD_EXPIRY_MONTH = "exp_month"
private const val FIELD_CARD_BRAND = "brand"
private const val FIELD_CARD_NETWORKS = "networks"
private const val FIELD_CARD_CHECKS = "checks"
private const val FIELD_CARD_CVC_CHECK = "cvc_check"

private const val FIELD_BANK_ACCOUNT_BANK_ICON_CODE = "bank_icon_code"

private const val FIELD_IS_DEFAULT = "is_default"
private const val FIELD_NICKNAME = "nickname"
private const val FIELD_FUNDING = "funding"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ConsumerPaymentDetailsJsonParser : ModelJsonParser<ConsumerPaymentDetails> {

    override fun parse(json: JSONObject): ConsumerPaymentDetails {
        val paymentDetails = json.optJSONArray(FIELD_PAYMENT_DETAILS)?.let { paymentDetailsArray ->
            (0 until paymentDetailsArray.length())
                .map { index -> paymentDetailsArray.getJSONObject(index) }
                .mapNotNull { parsePaymentDetails(it) }
        } // Response with a single object might have it not in a list
            ?: json.optJSONObject(FIELD_PAYMENT_DETAILS)?.let {
                parsePaymentDetails(it)
            }?.let { listOf(it) } ?: emptyList()
        return ConsumerPaymentDetails(paymentDetails)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun parsePaymentDetails(json: JSONObject): ConsumerPaymentDetails.PaymentDetails? =
        optString(json, FIELD_TYPE)?.let { type ->
            val id = json.getString(FIELD_ID)
            val isDefault = json.optBoolean(FIELD_IS_DEFAULT)
            val nickname = optString(json, FIELD_NICKNAME)?.takeIf { it.isNotBlank() }

            when (type.lowercase()) {
                "card" -> {
                    val cardDetails = json.getJSONObject(FIELD_CARD_DETAILS)
                    val checks = cardDetails.optJSONObject(FIELD_CARD_CHECKS)
                    val networks = jsonArrayToList(cardDetails.optJSONArray(FIELD_CARD_NETWORKS))

                    ConsumerPaymentDetails.Card(
                        id = id,
                        expiryYear = cardDetails.getInt(FIELD_CARD_EXPIRY_YEAR),
                        expiryMonth = cardDetails.getInt(FIELD_CARD_EXPIRY_MONTH),
                        brand = CardBrand.fromCode(cardBrandFix(cardDetails.getString(FIELD_CARD_BRAND))),
                        networks = networks,
                        last4 = cardDetails.getString(FIELD_CARD_LAST_4),
                        cvcCheck = CvcCheck.fromCode(checks?.getString(FIELD_CARD_CVC_CHECK)),
                        funding = cardDetails.getString(FIELD_FUNDING),
                        billingAddress = parseBillingAddress(json),
                        billingEmailAddress = optString(json, FIELD_BILLING_EMAIL_ADDRESS),
                        isDefault = isDefault,
                        nickname = nickname,
                    )
                }
                "bank_account" -> {
                    val bankAccountDetails = json.getJSONObject(FIELD_BANK_ACCOUNT_DETAILS)
                    ConsumerPaymentDetails.BankAccount(
                        id = id,
                        last4 = bankAccountDetails.getString(FIELD_BANK_ACCOUNT_LAST_4),
                        bankName = optString(bankAccountDetails, FIELD_BANK_ACCOUNT_BANK_NAME),
                        bankIconCode = optString(bankAccountDetails, FIELD_BANK_ACCOUNT_BANK_ICON_CODE),
                        isDefault = isDefault,
                        nickname = nickname,
                        billingAddress = parseBillingAddress(json),
                        billingEmailAddress = optString(json, FIELD_BILLING_EMAIL_ADDRESS)
                    )
                }
                else -> null
            }
        }

    private fun parseBillingAddress(json: JSONObject) =
        json.optJSONObject(FIELD_BILLING_ADDRESS)?.let { address ->
            ConsumerPaymentDetails.BillingAddress(
                name = optString(address, FIELD_ADDRESS_NAME),
                line1 = optString(address, FIELD_ADDRESS_LINE_1),
                line2 = optString(address, FIELD_ADDRESS_LINE_2),
                locality = optString(address, FIELD_ADDRESS_LOCALITY),
                postalCode = optString(address, FIELD_ADDRESS_POSTAL_CODE),
                administrativeArea = optString(address, FIELD_ADDRESS_ADMINISTRATIVE_AREA),
                countryCode = optString(address, FIELD_ADDRESS_COUNTRY_CODE)?.let { CountryCode(it) },
            )
        }

    /**
     * Fixes the incorrect brand enum values returned from the server in this service.
     */
    private fun cardBrandFix(original: String) = original.lowercase().let {
        when (it) {
            "american_express" -> "amex"
            "diners_club" -> "diners"
            else -> it
        }
    }
}
