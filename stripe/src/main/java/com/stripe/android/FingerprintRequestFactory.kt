package com.stripe.android

import android.content.Context
import androidx.annotation.VisibleForTesting

internal class FingerprintRequestFactory @VisibleForTesting internal constructor(
    private val telemetryClientUtil: TelemetryClientUtil
) : Factory0<FingerprintRequest> {

    internal constructor(context: Context) : this(TelemetryClientUtil(context))

    override fun create(): FingerprintRequest {
        return FingerprintRequest(
            telemetryClientUtil.createTelemetryMap(),
            telemetryClientUtil.hashedUid
        )
    }
}
