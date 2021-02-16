package com.stripe.android.paymentsheet.analytics

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
internal data class SessionId internal constructor(
    val value: String
) : Parcelable {
    internal constructor() : this(
        UUID.randomUUID().toString()
    )
}
