package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.ConfirmationToken
import com.stripe.android.model.MandateDataParams
import com.stripe.android.model.PaymentMethod
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ConfirmationTokenJsonParser : ModelJsonParser<ConfirmationToken> {
    override fun parse(json: JSONObject): ConfirmationToken? {
        val objectType = StripeJsonUtils.optString(json, ConfirmationToken.FIELD_OBJECT)
        if (ConfirmationToken.OBJECT_TYPE != objectType) {
            return null
        }

        val id = StripeJsonUtils.optString(json, ConfirmationToken.FIELD_ID)
            ?: return null

        return ConfirmationToken(
            id = id,
            `object` = objectType ?: ConfirmationToken.OBJECT_TYPE,
            created = StripeJsonUtils.optLong(json, ConfirmationToken.FIELD_CREATED) ?: 0L,
            liveMode = StripeJsonUtils.optBoolean(json, ConfirmationToken.FIELD_LIVEMODE) ?: false,
            paymentMethodData = json.optJSONObject(ConfirmationToken.FIELD_PAYMENT_METHOD_DATA)?.let {
                parsePaymentMethodData(it)
            },
            returnUrl = StripeJsonUtils.optString(json, ConfirmationToken.FIELD_RETURN_URL),
            shipping = json.optJSONObject(ConfirmationToken.FIELD_SHIPPING)?.let {
                parseShippingDetails(it)
            },
            setupFutureUsage = StripeJsonUtils.optString(json, ConfirmationToken.FIELD_SETUP_FUTURE_USAGE)?.let {
                parseSetupFutureUsage(it)
            },
            paymentMethodOptions = json.optJSONObject(ConfirmationToken.FIELD_PAYMENT_METHOD_OPTIONS)?.let {
                parsePaymentMethodOptions(it)
            },
            mandateData = json.optJSONObject(ConfirmationToken.FIELD_MANDATE_DATA)?.let {
                // Note: MandateDataParams doesn't have a JSON parser as it's typically only serialized to params
                // For now, we return null as this field is primarily used for outgoing requests
                null
            }
        )
    }

    private fun parseSetupFutureUsage(value: String): ConfirmationToken.SetupFutureUsage? {
        return ConfirmationToken.SetupFutureUsage.entries.find { it.code == value }
    }

    private fun parsePaymentMethodData(json: JSONObject): ConfirmationToken.PaymentMethodData? {
        val typeCode = StripeJsonUtils.optString(json, FIELD_PAYMENT_METHOD_DATA_TYPE) ?: return null
        val type = PaymentMethod.Type.fromCode(typeCode) ?: return null

        return ConfirmationToken.PaymentMethodData(
            type = type,
            billingDetails = json.optJSONObject(FIELD_PAYMENT_METHOD_DATA_BILLING_DETAILS)?.let {
                PaymentMethodJsonParser.BillingDetails().parse(it)
            },
            card = json.optJSONObject(FIELD_PAYMENT_METHOD_DATA_CARD)?.let {
                parsePaymentMethodDataCard(it)
            },
            usBankAccount = json.optJSONObject(FIELD_PAYMENT_METHOD_DATA_US_BANK_ACCOUNT)?.let {
                parsePaymentMethodDataUSBankAccount(it)
            },
            sepaDebit = json.optJSONObject(FIELD_PAYMENT_METHOD_DATA_SEPA_DEBIT)?.let {
                parsePaymentMethodDataSepaDebit(it)
            },
            metadata = StripeJsonUtils.jsonObjectToStringMap(json.optJSONObject(FIELD_PAYMENT_METHOD_DATA_METADATA))
        )
    }

    private fun parsePaymentMethodDataCard(json: JSONObject): ConfirmationToken.PaymentMethodData.Card {
        return ConfirmationToken.PaymentMethodData.Card(
            cvcToken = StripeJsonUtils.optString(json, FIELD_CVC_TOKEN),
            encryptedData = StripeJsonUtils.optString(json, FIELD_ENCRYPTED_DATA)
        )
    }

    private fun parsePaymentMethodDataUSBankAccount(json: JSONObject): ConfirmationToken.PaymentMethodData.USBankAccount {
        return ConfirmationToken.PaymentMethodData.USBankAccount(
            accountHolderType = StripeJsonUtils.optString(json, FIELD_ACCOUNT_HOLDER_TYPE)?.let { value ->
                PaymentMethod.USBankAccount.USBankAccountHolderType.entries.find { it.value == value }
            },
            accountType = StripeJsonUtils.optString(json, FIELD_ACCOUNT_TYPE)?.let { value ->
                PaymentMethod.USBankAccount.USBankAccountType.entries.find { it.value == value }
            },
            financialConnectionsAccount = StripeJsonUtils.optString(json, FIELD_FINANCIAL_CONNECTIONS_ACCOUNT)
        )
    }

    private fun parsePaymentMethodDataSepaDebit(json: JSONObject): ConfirmationToken.PaymentMethodData.SepaDebit {
        return ConfirmationToken.PaymentMethodData.SepaDebit(
            iban = StripeJsonUtils.optString(json, FIELD_IBAN)
        )
    }

    private fun parseShippingDetails(json: JSONObject): ConfirmationToken.ShippingDetails? {
        val address = json.optJSONObject(FIELD_SHIPPING_ADDRESS)?.let {
            AddressJsonParser().parse(it)
        } ?: return null

        val name = StripeJsonUtils.optString(json, FIELD_SHIPPING_NAME) ?: return null

        return ConfirmationToken.ShippingDetails(
            address = address,
            name = name,
            phone = StripeJsonUtils.optString(json, FIELD_SHIPPING_PHONE)
        )
    }

    private fun parsePaymentMethodOptions(json: JSONObject): ConfirmationToken.PaymentMethodOptions {
        return ConfirmationToken.PaymentMethodOptions(
            card = json.optJSONObject(FIELD_PAYMENT_METHOD_OPTIONS_CARD)?.let {
                parsePaymentMethodOptionsCard(it)
            },
            usBankAccount = json.optJSONObject(FIELD_PAYMENT_METHOD_OPTIONS_US_BANK_ACCOUNT)?.let {
                parsePaymentMethodOptionsUSBankAccount(it)
            },
            sepaDebit = json.optJSONObject(FIELD_PAYMENT_METHOD_OPTIONS_SEPA_DEBIT)?.let {
                parsePaymentMethodOptionsSepaDebit(it)
            }
        )
    }

    private fun parsePaymentMethodOptionsCard(json: JSONObject): ConfirmationToken.PaymentMethodOptions.Card {
        return ConfirmationToken.PaymentMethodOptions.Card(
            cvcToken = StripeJsonUtils.optString(json, FIELD_CVC_TOKEN),
            network = StripeJsonUtils.optString(json, FIELD_NETWORK),
            setupFutureUsage = StripeJsonUtils.optString(json, FIELD_SETUP_FUTURE_USAGE)?.let {
                parseSetupFutureUsage(it)
            }
        )
    }

    private fun parsePaymentMethodOptionsUSBankAccount(json: JSONObject): ConfirmationToken.PaymentMethodOptions.USBankAccount {
        return ConfirmationToken.PaymentMethodOptions.USBankAccount(
            verificationMethod = StripeJsonUtils.optString(json, FIELD_VERIFICATION_METHOD)
        )
    }

    private fun parsePaymentMethodOptionsSepaDebit(json: JSONObject): ConfirmationToken.PaymentMethodOptions.SepaDebit {
        return ConfirmationToken.PaymentMethodOptions.SepaDebit(
            setupFutureUsage = StripeJsonUtils.optString(json, FIELD_SETUP_FUTURE_USAGE)?.let {
                parseSetupFutureUsage(it)
            }
        )
    }

    companion object {
        fun fromJson(json: JSONObject?): ConfirmationToken? {
            return json?.let { ConfirmationTokenJsonParser().parse(it) }
        }

        // Private constants
        // PaymentMethodData fields
        private const val FIELD_PAYMENT_METHOD_DATA_TYPE = "type"
        private const val FIELD_PAYMENT_METHOD_DATA_BILLING_DETAILS = "billing_details"
        private const val FIELD_PAYMENT_METHOD_DATA_CARD = "card"
        private const val FIELD_PAYMENT_METHOD_DATA_US_BANK_ACCOUNT = "us_bank_account"
        private const val FIELD_PAYMENT_METHOD_DATA_SEPA_DEBIT = "sepa_debit"
        private const val FIELD_PAYMENT_METHOD_DATA_METADATA = "metadata"
        
        // Card fields
        private const val FIELD_CVC_TOKEN = "cvc_token"
        private const val FIELD_ENCRYPTED_DATA = "encrypted_data"
        
        // USBankAccount fields
        private const val FIELD_ACCOUNT_HOLDER_TYPE = "account_holder_type"
        private const val FIELD_ACCOUNT_TYPE = "account_type"
        private const val FIELD_FINANCIAL_CONNECTIONS_ACCOUNT = "financial_connections_account"
        
        // SepaDebit fields
        private const val FIELD_IBAN = "iban"
        
        // ShippingDetails fields
        private const val FIELD_SHIPPING_ADDRESS = "address"
        private const val FIELD_SHIPPING_NAME = "name"
        private const val FIELD_SHIPPING_PHONE = "phone"
        
        // PaymentMethodOptions fields
        private const val FIELD_PAYMENT_METHOD_OPTIONS_CARD = "card"
        private const val FIELD_PAYMENT_METHOD_OPTIONS_US_BANK_ACCOUNT = "us_bank_account"
        private const val FIELD_PAYMENT_METHOD_OPTIONS_SEPA_DEBIT = "sepa_debit"
        
        // Common fields
        private const val FIELD_NETWORK = "network"
        private const val FIELD_SETUP_FUTURE_USAGE = "setup_future_usage"
        private const val FIELD_VERIFICATION_METHOD = "verification_method"
    }
}