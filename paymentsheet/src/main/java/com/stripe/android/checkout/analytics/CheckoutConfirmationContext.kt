package com.stripe.android.checkout.analytics

import android.os.Parcelable
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class CheckoutConfirmationContext(
    val paymentSelection: PaymentSelection,
) : Parcelable
