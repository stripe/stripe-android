package com.stripe.android

import android.content.Context
import androidx.annotation.VisibleForTesting

internal class FingerprintRequestFactory @VisibleForTesting internal constructor(
    private val telemetryClientUtil: TelemetryClientUtil,
    private val uidSupplier: Supplier<StripeUid>
) : Factory0<FingerprintRequest> {

    internal constructor(context: Context) : this(
        telemetryClientUtil = TelemetryClientUtil(context),
        uidSupplier = UidSupplier(context)
    )

    override fun create(): FingerprintRequest {
        val guid = uidSupplier.get().value.takeUnless {
            it.isBlank()
        }?.let {
            StripeTextUtils.shaHashInput(it).orEmpty()
        }.orEmpty()

        return FingerprintRequest(
            params = telemetryClientUtil.createTelemetryMap(),
            guid = guid
        )
    }
}
