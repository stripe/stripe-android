package com.stripe.android.model

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

/**
 * This class contains the intent but also some of the settings/configuration/preferences
 * about the behaviors/features of the UI.
 */
@Parcelize
data class PaymentMethodPreference(
    val intent: StripeIntent,
    val formUI: String? = null
) : StripeModel
