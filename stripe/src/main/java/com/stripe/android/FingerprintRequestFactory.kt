package com.stripe.android

import android.content.Context
import androidx.annotation.VisibleForTesting

internal class FingerprintRequestFactory @VisibleForTesting internal constructor(
    private val fingerprintRequestParamsFactory: FingerprintRequestParamsFactory
) : Factory1<FingerprintData?, FingerprintRequest> {

    internal constructor(context: Context) : this(
        fingerprintRequestParamsFactory = FingerprintRequestParamsFactory(context)
    )

    override fun create(arg: FingerprintData?): FingerprintRequest {
        return FingerprintRequest(
            params = fingerprintRequestParamsFactory.createParams(arg),
            guid = arg?.guid.orEmpty()
        )
    }
}
