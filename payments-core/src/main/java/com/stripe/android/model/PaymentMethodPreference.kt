package com.stripe.android.model

import com.stripe.android.core.model.StripeModel
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * This class contains the intent but also some of the settings/configuration/preferences
 * about the behaviors/features of the UI.
 */
@Parcelize
@Poko
class PaymentMethodPreference(
    val intent: StripeIntent,
    val formUI: String? = null
) : StripeModel
