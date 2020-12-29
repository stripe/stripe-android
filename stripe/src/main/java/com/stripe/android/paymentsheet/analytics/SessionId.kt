package com.stripe.android.paymentsheet.analytics

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
internal class SessionId private constructor(
    val value: String
) : Parcelable {
    internal constructor() : this(
        UUID.randomUUID().toString()
    )
}
