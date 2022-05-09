package com.stripe.android.paymentsheet.model

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.ui.getCardBrandIcon
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class PaymentSelection : Parcelable {
    @Parcelize
    object GooglePay : PaymentSelection()

    @Parcelize
    data class Saved(
        val paymentMethod: PaymentMethod
    ) : PaymentSelection()

    enum class CustomerRequestedSave {
        RequestReuse,
        RequestNoReuse,
        NoRequest
    }

    sealed class New : PaymentSelection() {
        abstract val paymentMethodCreateParams: PaymentMethodCreateParams
        abstract val customerRequestedSave: CustomerRequestedSave

        @Parcelize
        data class Card(
            override val paymentMethodCreateParams: PaymentMethodCreateParams,
            val brand: CardBrand,
            override val customerRequestedSave: CustomerRequestedSave
        ) : New() {
            @IgnoredOnParcel
            val last4: String = (
                (paymentMethodCreateParams.toParamMap()["card"] as? Map<*, *>)!!
                ["number"] as String
                )
                .takeLast(4)
        }

        @Parcelize
        data class Link(
            val paymentDetails: ConsumerPaymentDetails.PaymentDetails,
            override val paymentMethodCreateParams: PaymentMethodCreateParams
        ) : New() {
            @IgnoredOnParcel
            override val customerRequestedSave = CustomerRequestedSave.NoRequest

            @IgnoredOnParcel
            @DrawableRes
            val iconResource = when (paymentDetails) {
                is ConsumerPaymentDetails.Card -> paymentDetails.brand.getCardBrandIcon()
            }

            @IgnoredOnParcel
            val label = when (paymentDetails) {
                is ConsumerPaymentDetails.Card -> paymentDetails.last4
            }
        }

        @Parcelize
        data class GenericPaymentMethod(
            val labelResource: String,
            @DrawableRes val iconResource: Int,
            override val paymentMethodCreateParams: PaymentMethodCreateParams,
            override val customerRequestedSave: CustomerRequestedSave,
        ) : New()
    }
}
