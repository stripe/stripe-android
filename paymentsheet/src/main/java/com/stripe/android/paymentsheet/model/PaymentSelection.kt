package com.stripe.android.paymentsheet.model

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import com.stripe.android.link.LinkPaymentDetails
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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object GooglePay : PaymentSelection()

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Saved(
        val paymentMethod: PaymentMethod
    ) : PaymentSelection()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class CustomerRequestedSave {
        RequestReuse,
        RequestNoReuse,
        NoRequest
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed class New : PaymentSelection() {
        abstract val paymentMethodCreateParams: PaymentMethodCreateParams
        abstract val customerRequestedSave: CustomerRequestedSave

        @Parcelize
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class USBankAccount(
            val labelResource: String,
            @DrawableRes val iconResource: Int,
            val bankName: String,
            val last4: String,
            val financialConnectionsSessionId: String,
            val intentId: String,
            override val paymentMethodCreateParams: PaymentMethodCreateParams,
            override val customerRequestedSave: CustomerRequestedSave
        ) : New()

        @Parcelize
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Link(val linkPaymentDetails: LinkPaymentDetails) : New() {
            @IgnoredOnParcel
            override val customerRequestedSave = CustomerRequestedSave.NoRequest

            @IgnoredOnParcel
            private val paymentDetails = linkPaymentDetails.paymentDetails

            @IgnoredOnParcel
            override val paymentMethodCreateParams = linkPaymentDetails.paymentMethodCreateParams

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
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class GenericPaymentMethod(
            val labelResource: String,
            @DrawableRes val iconResource: Int,
            override val paymentMethodCreateParams: PaymentMethodCreateParams,
            override val customerRequestedSave: CustomerRequestedSave,
        ) : New()
    }
}
