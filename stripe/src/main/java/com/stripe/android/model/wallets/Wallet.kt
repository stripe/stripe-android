package com.stripe.android.model.wallets

import android.os.Parcelable
import com.stripe.android.model.Address
import com.stripe.android.model.StripeModel
import kotlinx.android.parcel.Parcelize

/**
 * Details of the card wallet.
 *
 * [card.wallet](https://stripe.com/docs/api/payment_methods/object#payment_method_object-card-wallet)
 */
sealed class Wallet(
    internal val walletType: Type
) : StripeModel {

    @Parcelize
    data class AmexExpressCheckoutWallet internal constructor(
        val dynamicLast4: String?
    ) : Wallet(Type.AmexExpressCheckout)

    @Parcelize
    data class ApplePayWallet internal constructor(
        val dynamicLast4: String?
    ) : Wallet(Type.ApplePay)

    @Parcelize
    data class GooglePayWallet internal constructor(
        val dynamicLast4: String?
    ) : Wallet(Type.GooglePay), Parcelable

    @Parcelize
    data class MasterpassWallet internal constructor(
        val billingAddress: Address?,
        val email: String?,
        val name: String?,
        val shippingAddress: Address?
    ) : Wallet(Type.Masterpass)

    @Parcelize
    data class SamsungPayWallet internal constructor(
        val dynamicLast4: String?
    ) : Wallet(Type.SamsungPay)

    @Parcelize
    data class VisaCheckoutWallet internal constructor(
        val billingAddress: Address?,
        val email: String?,
        val name: String?,
        val shippingAddress: Address?,
        val dynamicLast4: String?
    ) : Wallet(Type.VisaCheckout)

    internal enum class Type(val code: String) {
        AmexExpressCheckout("amex_express_checkout"),
        ApplePay("apple_pay"),
        GooglePay("google_pay"),
        Masterpass("master_pass"),
        SamsungPay("samsung_pay"),
        VisaCheckout("visa_checkout");

        internal companion object {
            internal fun fromCode(code: String?): Type? {
                return values().firstOrNull { it.code == code }
            }
        }
    }
}
