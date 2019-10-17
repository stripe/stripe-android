package com.stripe.android.model.wallets

import com.stripe.android.model.StripeJsonUtils.optString
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject

@Parcelize
data class VisaCheckoutWallet private constructor(
    val billingAddress: Address?,
    val email: String?,
    val name: String?,
    val shippingAddress: Address?,
    val dynamicLast4: String?
) : Wallet(Type.VisaCheckout) {

    internal class Builder : Wallet.Builder<VisaCheckoutWallet>() {
        private var billingAddress: Address? = null
        private var email: String? = null
        private var name: String? = null
        private var shippingAddress: Address? = null

        fun setBillingAddress(billingAddress: Address?): Builder {
            this.billingAddress = billingAddress
            return this
        }

        fun setEmail(email: String?): Builder {
            this.email = email
            return this
        }

        fun setName(name: String?): Builder {
            this.name = name
            return this
        }

        fun setShippingAddress(shippingAddress: Address?): Builder {
            this.shippingAddress = shippingAddress
            return this
        }

        public override fun build(): VisaCheckoutWallet {
            return VisaCheckoutWallet(
                billingAddress = billingAddress,
                email = email,
                name = name,
                shippingAddress = shippingAddress,
                dynamicLast4 = dynamicLast4
            )
        }
    }

    companion object {
        private const val FIELD_BILLING_ADDRESS = "billing_address"
        private const val FIELD_EMAIL = "email"
        private const val FIELD_NAME = "name"
        private const val FIELD_SHIPPING_ADDRESS = "shipping_address"

        internal fun fromJson(wallet: JSONObject): Builder {
            return Builder()
                .setBillingAddress(Address.fromJson(wallet.optJSONObject(FIELD_BILLING_ADDRESS)))
                .setEmail(optString(wallet, FIELD_EMAIL))
                .setName(optString(wallet, FIELD_NAME))
                .setShippingAddress(Address.fromJson(wallet.optJSONObject(FIELD_SHIPPING_ADDRESS)))
        }
    }
}
