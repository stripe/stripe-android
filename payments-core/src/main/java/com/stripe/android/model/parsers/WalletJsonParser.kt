package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.wallets.Wallet
import org.json.JSONObject

internal class WalletJsonParser : ModelJsonParser<Wallet> {
    override fun parse(json: JSONObject): Wallet? {
        val walletType = Wallet.Type
            .fromCode(optString(json, FIELD_TYPE)) ?: return null

        val walletTypeJson = json.optJSONObject(walletType.code) ?: return null

        val dynamicLast4 = optString(json, FIELD_DYNAMIC_LAST4)

        return when (walletType) {
            Wallet.Type.AmexExpressCheckout -> Wallet.AmexExpressCheckoutWallet(dynamicLast4)
            Wallet.Type.ApplePay -> Wallet.ApplePayWallet(dynamicLast4)
            Wallet.Type.SamsungPay -> Wallet.SamsungPayWallet(dynamicLast4)
            Wallet.Type.GooglePay -> Wallet.GooglePayWallet(dynamicLast4)
            Wallet.Type.Masterpass -> parseMasterpassWallet(walletTypeJson)
            Wallet.Type.VisaCheckout -> parseVisaCheckoutWallet(walletTypeJson, dynamicLast4)
        }
    }

    private fun parseMasterpassWallet(
        json: JSONObject
    ): Wallet.MasterpassWallet {
        return Wallet.MasterpassWallet(
            billingAddress = json.optJSONObject(FIELD_BILLING_ADDRESS)?.let {
                AddressJsonParser().parse(it)
            },
            email = optString(json, FIELD_EMAIL),
            name = optString(json, FIELD_NAME),
            shippingAddress = json.optJSONObject(FIELD_SHIPPING_ADDRESS)?.let {
                AddressJsonParser().parse(it)
            }
        )
    }

    private fun parseVisaCheckoutWallet(
        json: JSONObject,
        dynamicLast4: String?
    ): Wallet.VisaCheckoutWallet {
        return Wallet.VisaCheckoutWallet(
            billingAddress = json.optJSONObject(FIELD_BILLING_ADDRESS)?.let {
                AddressJsonParser().parse(it)
            },
            email = optString(json, FIELD_EMAIL),
            name = optString(json, FIELD_NAME),
            shippingAddress = json.optJSONObject(FIELD_SHIPPING_ADDRESS)?.let {
                AddressJsonParser().parse(it)
            },
            dynamicLast4 = dynamicLast4
        )
    }

    private companion object {
        private const val FIELD_TYPE = "type"
        private const val FIELD_DYNAMIC_LAST4 = "dynamic_last4"

        private const val FIELD_BILLING_ADDRESS = "billing_address"
        private const val FIELD_EMAIL = "email"
        private const val FIELD_NAME = "name"
        private const val FIELD_SHIPPING_ADDRESS = "shipping_address"
    }
}
