package com.stripe.android.elements

import android.os.Parcelable
import com.stripe.android.elements.PaymentElement.Configuration.GooglePayConfiguration.ButtonType
import com.stripe.android.elements.PaymentElement.Configuration.GooglePayConfiguration.Display
import kotlinx.parcelize.Parcelize

// TODO-codex: define Display and ButtonType mappers
@Parcelize
data class CheckoutGooglePayConfiguration(
    val display: Display,
    val label: String?,
    val buttonType: ButtonType,
    val additionalEnabledNetworks: List<String>,
): Parcelable