package com.stripe.android.model.wallets

import com.stripe.android.model.StripeJsonUtils.optString
import com.stripe.android.model.wallets.Wallet.FIELD_DYNAMIC_LAST4
import org.json.JSONObject

internal class WalletFactory {

    fun create(walletJson: JSONObject?): Wallet? {
        if (walletJson == null) {
            return null
        }

        val walletType = Wallet.Type
            .fromCode(optString(walletJson, Wallet.FIELD_TYPE)) ?: return null

        return create(walletType, walletJson)
    }

    private fun create(walletType: Wallet.Type, walletJson: JSONObject): Wallet? {
        val walletTypeJson = walletJson.optJSONObject(walletType.code) ?: return null

        val walletBuilder: Wallet.Builder<out Wallet>
        when (walletType) {
            Wallet.Type.AmexExpressCheckout -> {
                walletBuilder = AmexExpressCheckoutWallet.fromJson()
            }
            Wallet.Type.ApplePay -> {
                walletBuilder = ApplePayWallet.fromJson()
            }
            Wallet.Type.GooglePay -> {
                walletBuilder = GooglePayWallet.fromJson()
            }
            Wallet.Type.Masterpass -> {
                walletBuilder = MasterpassWallet.fromJson(walletTypeJson)
            }
            Wallet.Type.SamsungPay -> {
                walletBuilder = SamsungPayWallet.fromJson()
            }
            Wallet.Type.VisaCheckout -> {
                walletBuilder = VisaCheckoutWallet.fromJson(walletTypeJson)
            }
        }

        return walletBuilder
            .setDynamicLast4(optString(walletJson, FIELD_DYNAMIC_LAST4))
            .build()
    }
}
