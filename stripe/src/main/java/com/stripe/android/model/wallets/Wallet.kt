package com.stripe.android.model.wallets

import android.os.Parcelable
import com.stripe.android.ObjectBuilder
import com.stripe.android.model.StripeJsonUtils.optString
import com.stripe.android.model.StripeModel
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject

abstract class Wallet internal constructor(
    internal val walletType: Type
) : StripeModel(), Parcelable {
    internal abstract class Builder<WalletType : Wallet> {
        var dynamicLast4: String? = null

        fun setDynamicLast4(dynamicLast4: String?): Builder<*> {
            this.dynamicLast4 = dynamicLast4
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

    @Parcelize
    data class Address internal constructor(
        val city: String?,
        val country: String?,
        val line1: String?,
        val line2: String?,
        val postalCode: String?,
        val state: String?
    ) : StripeModel(), Parcelable {
        internal class Builder : ObjectBuilder<Address> {
            private var city: String? = null
            private var country: String? = null
            private var line1: String? = null
            private var line2: String? = null
            private var postalCode: String? = null
            private var state: String? = null

            fun setCity(city: String?): Builder {
                this.city = city
                return this
            }

            fun setCountry(country: String?): Builder {
                this.country = country
                return this
            }

            fun setLine1(line1: String?): Builder {
                this.line1 = line1
                return this
            }

            fun setLine2(line2: String?): Builder {
                this.line2 = line2
                return this
            }

            fun setPostalCode(postalCode: String?): Builder {
                this.postalCode = postalCode
                return this
            }

            fun setState(state: String?): Builder {
                this.state = state
                return this
            }

            override fun build(): Address {
                return Address(
                    city = city,
                    country = country,
                    line1 = line1,
                    line2 = line2,
                    postalCode = postalCode,
                    state = state
                )
            }
        }

        companion object {
            private const val FIELD_CITY = "city"
            private const val FIELD_COUNTRY = "country"
            private const val FIELD_LINE1 = "line1"
            private const val FIELD_LINE2 = "line2"
            private const val FIELD_POSTAL_CODE = "postal_code"
            private const val FIELD_STATE = "state"

            internal fun fromJson(addressJson: JSONObject?): Address? {
                return if (addressJson == null) {
                    null
                } else {
                    Builder()
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
    }

    companion object {
        internal const val FIELD_DYNAMIC_LAST4 = "dynamic_last4"
        internal const val FIELD_TYPE = "type"
    }
}
