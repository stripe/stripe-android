package com.stripe.android.paymentsheet.flowcontroller

import android.os.Parcelable
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.SavedSelection
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class InitData(
    val config: PaymentSheet.Configuration?,
    val paymentIntent: PaymentIntent,
    // the allowed payment method types
    val paymentMethodTypes: List<PaymentMethod.Type>,
    // the customer's existing payment methods
    val paymentMethods: List<PaymentMethod>,
    val savedSelection: SavedSelection,
    val isGooglePayReady: Boolean
) : Parcelable
