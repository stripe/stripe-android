package com.stripe.android.crypto.onramp.model

import android.os.Parcelable
import com.stripe.android.paymentsheet.PaymentSheet
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * Configuration options required to initialize the Onramp flow.
 *
 * @property publishableKey Your Stripe publishable API key.
 * @property paymentSheetAppearance Appearance settings for the PaymentSheet UI.
 */
@Parcelize
@Poko
internal class OnrampConfiguration(
    val publishableKey: String,
    val paymentSheetAppearance: PaymentSheet.Appearance
) : Parcelable
