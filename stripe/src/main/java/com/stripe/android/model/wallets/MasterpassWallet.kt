package com.stripe.android.model.wallets

import android.os.Parcel
import android.os.Parcelable
import com.stripe.android.model.StripeJsonUtils.optString
import java.util.Objects
import org.json.JSONObject

class MasterpassWallet : Wallet {
    val billingAddress: Address?
    val email: String?
    val name: String?
    val shippingAddress: Address?

    private constructor(builder: Builder) : super(Type.Masterpass, builder) {
        billingAddress = builder.mBillingAddress
        email = builder.mEmail
        name = builder.mName
        shippingAddress = builder.mShippingAddress
    }

    override fun hashCode(): Int {
        return Objects.hash(billingAddress, email, name, shippingAddress)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        return if (other is MasterpassWallet) {
            typedEquals(other)
        } else {
            false
        }
    }

    private fun typedEquals(wallet: MasterpassWallet): Boolean {
        return (billingAddress == wallet.billingAddress &&
            email == wallet.email &&
            name == wallet.name &&
            shippingAddress == wallet.shippingAddress)
    }

    internal class Builder : Wallet.Builder<MasterpassWallet>() {
        var mBillingAddress: Address? = null
        var mEmail: String? = null
        var mName: String? = null
        var mShippingAddress: Address? = null

        fun setBillingAddress(billingAddress: Address?): Builder {
            this.mBillingAddress = billingAddress
            return this
        }

        fun setEmail(email: String?): Builder {
            this.mEmail = email
            return this
        }

        fun setName(name: String?): Builder {
            this.mName = name
            return this
        }

        fun setShippingAddress(shippingAddress: Address?): Builder {
            this.mShippingAddress = shippingAddress
            return this
        }

        public override fun build(): MasterpassWallet {
            return MasterpassWallet(this)
        }
    }

    private constructor(parcel: Parcel) : super(parcel) {
        billingAddress = parcel.readParcelable(Address::class.java.classLoader)
        email = parcel.readString()
        name = parcel.readString()
        shippingAddress = parcel.readParcelable(Address::class.java.classLoader)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeParcelable(billingAddress, flags)
        dest.writeString(email)
        dest.writeString(name)
        dest.writeParcelable(shippingAddress, flags)
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

        @JvmField
        val CREATOR: Parcelable.Creator<MasterpassWallet> =
            object : Parcelable.Creator<MasterpassWallet> {
                override fun createFromParcel(parcel: Parcel): MasterpassWallet {
                    return MasterpassWallet(parcel)
                }

                override fun newArray(size: Int): Array<MasterpassWallet?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
