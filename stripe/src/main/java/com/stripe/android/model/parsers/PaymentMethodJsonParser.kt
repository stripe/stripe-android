package com.stripe.android.model.parsers

import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeJsonUtils
import org.json.JSONObject

internal class PaymentMethodJsonParser : ModelJsonParser<PaymentMethod> {
    override fun parse(json: JSONObject): PaymentMethod {
        val type =
            PaymentMethod.Type.fromCode(StripeJsonUtils.optString(json, FIELD_TYPE))
        val builder = PaymentMethod.Builder()
            .setId(StripeJsonUtils.optString(json, FIELD_ID))
            .setType(type)
            .setCreated(StripeJsonUtils.optLong(json, FIELD_CREATED))
            .setBillingDetails(
                json.optJSONObject(FIELD_BILLING_DETAILS)?.let {
                    BillingDetails().parse(it)
                }
            )
            .setCustomerId(StripeJsonUtils.optString(json, FIELD_CUSTOMER))
            .setLiveMode(json.optBoolean(FIELD_LIVEMODE))
            .setMetadata(StripeJsonUtils.optHash(json, FIELD_METADATA))

        when (type) {
            PaymentMethod.Type.Card ->
                builder.setCard(
                    json.optJSONObject(FIELD_CARD)?.let {
                        CardJsonParser().parse(it)
                    }
                )
            PaymentMethod.Type.CardPresent ->
                builder.setCardPresent(PaymentMethod.CardPresent.EMPTY)
            PaymentMethod.Type.Ideal ->
                builder.setIdeal(
                    json.optJSONObject(FIELD_IDEAL)?.let {
                        IdealJsonParser().parse(it)
                    }
                )
            PaymentMethod.Type.Fpx ->
                builder.setFpx(
                    json.optJSONObject(FIELD_FPX)?.let {
                        FpxJsonParser().parse(it)
                    }
                )
            PaymentMethod.Type.SepaDebit ->
                builder.setSepaDebit(
                    json.optJSONObject(FIELD_SEPA_DEBIT)?.let {
                        SepaDebitJsonParser().parse(it)
                    }
                )
        }

        return builder.build()
    }

    internal class BillingDetails : ModelJsonParser<PaymentMethod.BillingDetails> {
        override fun parse(json: JSONObject): PaymentMethod.BillingDetails {
            return PaymentMethod.BillingDetails(
                address = json.optJSONObject(FIELD_ADDRESS)?.let {
                    AddressJsonParser().parse(it)
                },
                email = StripeJsonUtils.optString(json, FIELD_EMAIL),
                name = StripeJsonUtils.optString(json, FIELD_NAME),
                phone = StripeJsonUtils.optString(json, FIELD_PHONE)
            )
        }

        private companion object {
            private const val FIELD_ADDRESS = "address"
            private const val FIELD_EMAIL = "email"
            private const val FIELD_NAME = "name"
            private const val FIELD_PHONE = "phone"
        }
    }

    internal class CardJsonParser : ModelJsonParser<PaymentMethod.Card> {
        override fun parse(json: JSONObject): PaymentMethod.Card {
            return PaymentMethod.Card(
                brand = StripeJsonUtils.optString(json, FIELD_BRAND),
                checks = json.optJSONObject(FIELD_CHECKS)?.let {
                    ChecksJsonParser().parse(it)
                },
                country = StripeJsonUtils.optString(json, FIELD_COUNTRY),
                expiryMonth = StripeJsonUtils.optInteger(json, FIELD_EXP_MONTH),
                expiryYear = StripeJsonUtils.optInteger(json, FIELD_EXP_YEAR),
                funding = StripeJsonUtils.optString(json, FIELD_FUNDING),
                last4 = StripeJsonUtils.optString(json, FIELD_LAST4),
                threeDSecureUsage = json.optJSONObject(FIELD_THREE_D_SECURE_USAGE)?.let {
                    ThreeDSecureUsageJsonParser().parse(it)
                },
                wallet = json.optJSONObject(FIELD_WALLET)?.let {
                    WalletJsonParser().parse(it)
                }
            )
        }

        internal class ChecksJsonParser : ModelJsonParser<PaymentMethod.Card.Checks> {
            override fun parse(json: JSONObject): PaymentMethod.Card.Checks {
                return PaymentMethod.Card.Checks(
                    addressLine1Check = StripeJsonUtils.optString(json, FIELD_ADDRESS_LINE1_CHECK),
                    addressPostalCodeCheck = StripeJsonUtils.optString(
                        json, FIELD_ADDRESS_POSTAL_CODE_CHECK
                    ),
                    cvcCheck = StripeJsonUtils.optString(json, FIELD_CVC_CHECK)
                )
            }

            private companion object {
                private const val FIELD_ADDRESS_LINE1_CHECK = "address_line1_check"
                private const val FIELD_ADDRESS_POSTAL_CODE_CHECK = "address_postal_code_check"
                private const val FIELD_CVC_CHECK = "cvc_check"
            }
        }

        internal class ThreeDSecureUsageJsonParser : ModelJsonParser<PaymentMethod.Card.ThreeDSecureUsage> {
            override fun parse(json: JSONObject): PaymentMethod.Card.ThreeDSecureUsage {
                return PaymentMethod.Card.ThreeDSecureUsage(
                    isSupported = StripeJsonUtils.optBoolean(json, FIELD_IS_SUPPORTED)
                )
            }

            private companion object {
                private const val FIELD_IS_SUPPORTED = "supported"
            }
        }

        private companion object {
            private const val FIELD_BRAND = "brand"
            private const val FIELD_CHECKS = "checks"
            private const val FIELD_COUNTRY = "country"
            private const val FIELD_EXP_MONTH = "exp_month"
            private const val FIELD_EXP_YEAR = "exp_year"
            private const val FIELD_FUNDING = "funding"
            private const val FIELD_LAST4 = "last4"
            private const val FIELD_THREE_D_SECURE_USAGE = "three_d_secure_usage"
            private const val FIELD_WALLET = "wallet"
        }
    }

    internal class IdealJsonParser : ModelJsonParser<PaymentMethod.Ideal> {
        override fun parse(json: JSONObject): PaymentMethod.Ideal {
            return PaymentMethod.Ideal(
                bank = StripeJsonUtils.optString(json, FIELD_BANK),
                bankIdentifierCode = StripeJsonUtils.optString(json, FIELD_BIC)
            )
        }

        private companion object {
            private const val FIELD_BANK = "bank"
            private const val FIELD_BIC = "bic"
        }
    }

    internal class FpxJsonParser : ModelJsonParser<PaymentMethod.Fpx> {
        override fun parse(json: JSONObject): PaymentMethod.Fpx {
            return PaymentMethod.Fpx(
                bank = StripeJsonUtils.optString(json, FIELD_BANK),
                accountHolderType = StripeJsonUtils.optString(json, FIELD_ACCOUNT_HOLDER_TYPE)
            )
        }

        private companion object {
            private const val FIELD_ACCOUNT_HOLDER_TYPE = "account_holder_type"
            private const val FIELD_BANK = "bank"
        }
    }

    internal class SepaDebitJsonParser : ModelJsonParser<PaymentMethod.SepaDebit> {
        override fun parse(json: JSONObject): PaymentMethod.SepaDebit {
            return PaymentMethod.SepaDebit(
                StripeJsonUtils.optString(json, FIELD_BANK_CODE),
                StripeJsonUtils.optString(json, FIELD_BRANCH_CODE),
                StripeJsonUtils.optString(json, FIELD_COUNTRY),
                StripeJsonUtils.optString(json, FIELD_FINGERPRINT),
                StripeJsonUtils.optString(json, FIELD_LAST4)
            )
        }

        private companion object {
            private const val FIELD_BANK_CODE = "bank_code"
            private const val FIELD_BRANCH_CODE = "branch_code"
            private const val FIELD_COUNTRY = "country"
            private const val FIELD_FINGERPRINT = "fingerprint"
            private const val FIELD_LAST4 = "last4"
        }
    }

    private companion object {
        private const val FIELD_ID = "id"
        private const val FIELD_BILLING_DETAILS = "billing_details"
        private const val FIELD_CREATED = "created"
        private const val FIELD_CUSTOMER = "customer"
        private const val FIELD_LIVEMODE = "livemode"
        private const val FIELD_METADATA = "metadata"

        // types
        private const val FIELD_TYPE = "type"
        private const val FIELD_CARD = "card"
        private const val FIELD_CARD_PRESENT = "card_present"
        private const val FIELD_FPX = "fpx"
        private const val FIELD_IDEAL = "ideal"
        private const val FIELD_SEPA_DEBIT = "sepa_debit"
    }
}
