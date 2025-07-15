package com.stripe.android.crypto.onramp

import android.os.Parcelable
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parcelize

/**
 * Configuration options required to initialize the Onramp flow.
 *
 * @property publishableKey Your Stripe publishable API key.
 * @property paymentSheetAppearance Appearance settings for the PaymentSheet UI.
 */
@Parcelize
data class OnrampConfiguration(
    val publishableKey: String,
    val paymentSheetAppearance: PaymentSheet.Appearance
) : Parcelable
