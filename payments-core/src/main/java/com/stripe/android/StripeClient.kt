package com.stripe.android

import android.os.Parcelable
import com.stripe.android.core.ApiKeyValidator
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@Parcelize
@Poko
class StripeClient(
    val publishableKey: String,
    val stripeAccountId: String? = null,
) : Parcelable {
    init {
        ApiKeyValidator.get().requireValid(publishableKey)
    }
}
