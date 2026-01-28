package com.stripe.android.crypto.onramp.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.link.LinkAppearance
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * Configuration options required to initialize the Onramp flow.
 *
 * @property merchantDisplayName The display name to use for the merchant.
 * @property publishableKey The publishable key from the API dashboard to enable requests.
 * @property appearance Appearance settings for the PaymentSheet UI.
 * @property cryptoCustomerId The unique customer ID for crypto onramp.
 */
@Parcelize
@Poko
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class OnrampConfiguration(
    internal val merchantDisplayName: String,
    internal val publishableKey: String,
    internal val appearance: LinkAppearance,
    internal val cryptoCustomerId: String? = null,
) : Parcelable
