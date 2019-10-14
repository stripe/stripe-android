package com.stripe.android.model

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Size
import java.util.Currency

/**
 * Model representing a shipping method in the Android SDK.
 */
data class ShippingMethod @JvmOverloads constructor(
    /**
     * Human friendly label specifying the shipping method that can be shown in the UI.
     */
    val label: String,

    /**
     * Identifier for the shipping method.
     */
    val identifier: String,

    /**
     * The cost in minor unit based on [currency]
     */
    val amount: Long,

    /**
     * The currency that the specified amount will be rendered in.
     */
    val currency: Currency,

    /**
     * Human friendly information such as estimated shipping times that can be shown in
     * the UI
     */
    val detail: String? = null
) : StripeModel(), Parcelable {

    @JvmOverloads
    constructor(
        label: String,
        identifier: String,
        amount: Long,
        @Size(min = 0, max = 3) currencyCode: String,
        detail: String? = null
    ) : this(
        label, identifier, amount, Currency.getInstance(currencyCode), detail
    )

    private constructor(parcel: Parcel) : this(
        requireNotNull(parcel.readString()),
        requireNotNull(parcel.readString()),
        parcel.readLong(),
        Currency.getInstance(requireNotNull(parcel.readString())),
        parcel.readString()
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeString(label)
        parcel.writeString(identifier)
        parcel.writeLong(amount)
        parcel.writeString(currency.currencyCode)
        parcel.writeString(detail)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ShippingMethod> =
            object : Parcelable.Creator<ShippingMethod> {
                override fun createFromParcel(parcel: Parcel): ShippingMethod {
                    return ShippingMethod(parcel)
                }

                override fun newArray(size: Int): Array<ShippingMethod?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
