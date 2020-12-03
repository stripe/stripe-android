package com.stripe.android.networking

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.stripe.android.FingerprintData

internal interface FingerprintRequestFactory {
    fun create(arg: FingerprintData?): FingerprintRequest

    class Default @VisibleForTesting internal constructor(
        private val fingerprintRequestParamsFactory: FingerprintRequestParamsFactory
    ) : FingerprintRequestFactory {

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
}
