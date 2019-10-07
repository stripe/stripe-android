package com.stripe.android

import android.os.Parcel
import android.os.Parcelable
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.ShippingMethod
import java.util.Objects

/**
 * A data class representing the state of the associated [PaymentSession].
 */
class PaymentSessionData : Parcelable {

    /**
     * The cart total value, excluding shipping and tax items.
     */
    var cartTotal: Long = 0L
        internal set

    /**
     * Whether the payment data is ready for making a charge. This can be used to
     * set a buy button to enabled for prompt a user to fill in more information.
     */
    var isPaymentReadyToCharge: Boolean = false

    /**
     * The current value of the shipping items in the associated [PaymentSession]
     */
    var shippingTotal: Long = 0L
        internal set

    /**
     * Where the items being purchased should be shipped.
     */
    var shippingInformation: ShippingInformation? = null

    /**
     * How the items being purchased should be shipped.
     */
    var shippingMethod: ShippingMethod? = null

    /**
     * @return the selected payment method for the associated [PaymentSession]
     */
    var paymentMethod: PaymentMethod? = null
        internal set

    constructor()

    /**
     * Function that looks at the [PaymentSessionConfig] and determines whether the data is
     * ready to charge.
     *
     * @param config specifies what data is required
     * @return whether the data is ready to charge
     */
    fun updateIsPaymentReadyToCharge(config: PaymentSessionConfig): Boolean {
        isPaymentReadyToCharge = paymentMethod != null &&
            (!config.isShippingInfoRequired || shippingInformation != null) &&
            (!config.isShippingMethodRequired || shippingMethod != null)
        return isPaymentReadyToCharge
    }

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other is PaymentSessionData -> typedEquals(other)
            else -> false
        }
    }

    private fun typedEquals(data: PaymentSessionData): Boolean {
        return cartTotal == data.cartTotal &&
            isPaymentReadyToCharge == data.isPaymentReadyToCharge &&
            shippingTotal == data.shippingTotal &&
            shippingInformation == data.shippingInformation &&
            shippingMethod == data.shippingMethod &&
            paymentMethod == data.paymentMethod
    }

    override fun hashCode(): Int {
        return Objects.hash(
            cartTotal, isPaymentReadyToCharge, paymentMethod, shippingTotal, shippingInformation,
            shippingMethod
        )
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeLong(cartTotal)
        parcel.writeInt(if (isPaymentReadyToCharge) 1 else 0)
        parcel.writeParcelable(paymentMethod, i)
        parcel.writeParcelable(shippingInformation, i)
        parcel.writeParcelable(shippingMethod, i)
        parcel.writeLong(shippingTotal)
    }

    private constructor(parcel: Parcel) {
        cartTotal = parcel.readLong()
        isPaymentReadyToCharge = parcel.readInt() == 1
        paymentMethod = parcel.readParcelable(PaymentMethod::class.java.classLoader)
        shippingInformation = parcel.readParcelable(ShippingInformation::class.java.classLoader)
        shippingMethod = parcel.readParcelable(ShippingMethod::class.java.classLoader)
        shippingTotal = parcel.readLong()
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PaymentSessionData> =
            object : Parcelable.Creator<PaymentSessionData> {
                override fun createFromParcel(parcel: Parcel): PaymentSessionData {
                    return PaymentSessionData(parcel)
                }

                override fun newArray(size: Int): Array<PaymentSessionData?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
