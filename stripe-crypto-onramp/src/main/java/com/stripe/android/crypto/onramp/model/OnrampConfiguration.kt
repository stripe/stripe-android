package com.stripe.android.crypto.onramp.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.link.model.LinkAppearance
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * Configuration options required to initialize the Onramp flow.
 *
 * @property appearance Appearance settings for the PaymentSheet UI.
 */
@Parcelize
@Poko
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class OnrampConfiguration(
    val appearance: LinkAppearance
) : Parcelable
