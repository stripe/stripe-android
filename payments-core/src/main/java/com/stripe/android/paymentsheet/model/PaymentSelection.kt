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

    sealed class New : PaymentSelection() {
        abstract val paymentMethodCreateParams: PaymentMethodCreateParams
        abstract val shouldSavePaymentMethod: Boolean

        @Parcelize
        data class Card(
            override val paymentMethodCreateParams: PaymentMethodCreateParams,
            val brand: CardBrand,
            override val shouldSavePaymentMethod: Boolean
        ) : New()

        @Parcelize
        data class GenericPaymentMethod(
            @StringRes val labelResource: Int,
            @DrawableRes val iconResource: Int,
            override val paymentMethodCreateParams: PaymentMethodCreateParams,
            override val shouldSavePaymentMethod: Boolean
        ) : New()
    }
}
