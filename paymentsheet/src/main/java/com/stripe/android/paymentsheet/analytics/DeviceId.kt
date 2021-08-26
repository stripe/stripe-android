package com.stripe.android.paymentsheet.analytics

import java.util.UUID

internal data class DeviceId internal constructor(
    val value: String
) {
    constructor() : this(
        UUID.randomUUID().toString()
    )
}
