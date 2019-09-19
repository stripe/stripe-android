package com.stripe.android.model.wallets

import android.os.Parcel
import android.os.Parcelable
import com.stripe.android.ObjectBuilder
import com.stripe.android.model.StripeJsonUtils.optString
import com.stripe.android.model.StripeModel
import java.util.Objects
import org.json.JSONObject

abstract class Wallet : StripeModel, Parcelable {

    private val dynamicLast4: String?
    private val walletType: Type

    internal constructor(walletType: Type, builder: Builder<*>) {
        this.walletType = walletType
        dynamicLast4 = builder.mDynamicLast4
    }

    internal constructor(parcel: Parcel) {
        dynamicLast4 = parcel.readString()
        walletType = Objects.requireNonNull<Type>(Type.fromCode(parcel.readString()))
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(dynamicLast4)
        dest.writeString(walletType.code)
    }

    override fun hashCode(): Int {
        return Objects.hash(dynamicLast4, walletType)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        return if (other is Wallet) {
            typedEquals(other)
        } else {
            false
        }
    }

    private fun typedEquals(wallet: Wallet): Boolean {
        return dynamicLast4 == wallet.dynamicLast4 && walletType == wallet.walletType
    }

    internal abstract class Builder<WalletType : Wallet> {
        var mDynamicLast4: String? = null

        fun setDynamicLast4(dynamicLast4: String?): Builder<*> {
            this.mDynamicLast4 = dynamicLast4
            return this
        }

        internal abstract fun build(): WalletType
    }

    internal enum class Type(val code: String) {
        AmexExpressCheckout("amex_express_checkout"),
        ApplePay("apple_pay"),
        GooglePay("google_pay"),
        Masterpass("master_pass"),
        SamsungPay("samsung_pay"),
        VisaCheckout("visa_checkout");

        companion object {
            fun fromCode(code: String?): Type? {
                return values().firstOrNull { it.code == code }
            }
        }
    }

    class Address : StripeModel, Parcelable {
        val city: String?
        val country: String?
        val line1: String?
        val line2: String?
        val postalCode: String?
        val state: String?

        private constructor(builder: Builder) {
            city = builder.mCity
            country = builder.mCountry
            line1 = builder.mLine1
            line2 = builder.mLine2
            postalCode = builder.mPostalCode
            state = builder.mState
        }

        private constructor(parcel: Parcel) {
            city = parcel.readString()
            country = parcel.readString()
            line1 = parcel.readString()
            line2 = parcel.readString()
            postalCode = parcel.readString()
            state = parcel.readString()
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeString(city)
            dest.writeString(country)
            dest.writeString(line1)
            dest.writeString(line2)
            dest.writeString(postalCode)
            dest.writeString(state)
        }

        override fun hashCode(): Int {
            return Objects.hash(city, country, line1, line2, postalCode, state)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }

            return if (other is Address) {
                typedEquals(other)
            } else {
                false
            }
        }

        private fun typedEquals(address: Address): Boolean {
            return (city == address.city &&
                country == address.country &&
                line1 == address.line1 &&
                line2 == address.line2 &&
                postalCode == address.postalCode &&
                state == address.state)
        }

        internal class Builder : ObjectBuilder<Address> {
            var mCity: String? = null
            var mCountry: String? = null
            var mLine1: String? = null
            var mLine2: String? = null
            var mPostalCode: String? = null
            var mState: String? = null

            fun setCity(city: String?): Builder {
                this.mCity = city
                return this
            }

            fun setCountry(country: String?): Builder {
                this.mCountry = country
                return this
            }

            fun setLine1(line1: String?): Builder {
                this.mLine1 = line1
                return this
            }

            fun setLine2(line2: String?): Builder {
                this.mLine2 = line2
                return this
            }

            fun setPostalCode(postalCode: String?): Builder {
                this.mPostalCode = postalCode
                return this
            }

            fun setState(state: String?): Builder {
                this.mState = state
                return this
            }

            override fun build(): Address {
                return Address(this)
            }
        }

        companion object {
            private const val FIELD_CITY = "city"
            private const val FIELD_COUNTRY = "country"
            private const val FIELD_LINE1 = "line1"
            private const val FIELD_LINE2 = "line2"
            private const val FIELD_POSTAL_CODE = "postal_code"
            private const val FIELD_STATE = "state"

            @JvmField
            val CREATOR: Parcelable.Creator<Address> = object : Parcelable.Creator<Address> {
                override fun createFromParcel(parcel: Parcel): Address {
                    return Address(parcel)
                }

                override fun newArray(size: Int): Array<Address?> {
                    return arrayOfNulls(size)
                }
            }

            internal fun fromJson(addressJson: JSONObject?): Address? {
                return if (addressJson == null) {
                    null
                } else Builder()
                    .setCity(optString(addressJson, FIELD_CITY))
                    .setCountry(optString(addressJson, FIELD_COUNTRY))
                    .setLine1(optString(addressJson, FIELD_LINE1))
                    .setLine2(optString(addressJson, FIELD_LINE2))
                    .setPostalCode(optString(addressJson, FIELD_POSTAL_CODE))
                    .setState(optString(addressJson, FIELD_STATE))
                    .build()
            }
        }
    }

    companion object {
        internal const val FIELD_DYNAMIC_LAST4 = "dynamic_last4"
        internal const val FIELD_TYPE = "type"
    }
}
