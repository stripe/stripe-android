package com.stripe.android

import android.content.Context
import androidx.annotation.VisibleForTesting

internal class FingerprintRequestFactory @VisibleForTesting internal constructor(
    private val fingerprintRequestParamsFactory: FingerprintRequestParamsFactory
) : Factory1<String?, FingerprintRequest> {

    internal constructor(context: Context) : this(
        fingerprintRequestParamsFactory = FingerprintRequestParamsFactory(context)
    )

    override fun create(arg: String?): FingerprintRequest {
        return FingerprintRequest(
            params = fingerprintRequestParamsFactory.createParams(),
            guid = arg.orEmpty()
        )
    }
}
