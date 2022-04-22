package com.stripe.android.paymentsheet.model

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import kotlinx.parcelize.Parcelize

internal sealed class PaymentSelection : Parcelable {
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
            val last4: String = (
                (paymentMethodCreateParams.toParamMap()["card"] as? Map<*, *>)!!
                ["number"] as String
                )
                .takeLast(4)
        }

        @Parcelize
        data class GenericPaymentMethod(
            @StringRes val labelResource: Int,
            @DrawableRes val iconResource: Int,
            override val paymentMethodCreateParams: PaymentMethodCreateParams,
            override val customerRequestedSave: CustomerRequestedSave
        ) : New()
    }
}
