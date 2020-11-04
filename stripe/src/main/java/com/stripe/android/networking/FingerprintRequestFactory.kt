package com.stripe.android.networking

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.stripe.android.Factory1
import com.stripe.android.FingerprintData

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
