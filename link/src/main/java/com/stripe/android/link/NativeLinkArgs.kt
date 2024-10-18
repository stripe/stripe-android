package com.stripe.android.link

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
internal data class NativeLinkArgs(
    val configuration: LinkConfiguration,
    val publishableKey: String,
    val stripeAccountId: String?
) : Parcelable
