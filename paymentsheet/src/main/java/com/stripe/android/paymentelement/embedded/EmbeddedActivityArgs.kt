package com.stripe.android.paymentelement.embedded

import android.os.Bundle
import android.os.Parcelable
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class EmbeddedActivityArgs(
    val paymentMethodMetadata: PaymentMethodMetadata,
    val configuration: EmbeddedPaymentElement.Configuration,
    val paymentElementCallbackIdentifier: String,
    val statusBarColor: Int?,
    val selection: PaymentSelection?,
    val previousNewSelections: Bundle,
    val customerState: CustomerState?,
    val promotions: List<PaymentMethodMessagePromotion>?,
    val launchMode: EmbeddedLaunchMode,
) : Parcelable
