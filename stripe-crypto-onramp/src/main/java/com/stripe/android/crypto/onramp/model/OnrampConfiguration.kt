package com.stripe.android.crypto.onramp.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.elements.Appearance
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class OnrampConfiguration(
    val paymentSheetAppearance: Appearance
) : Parcelable
