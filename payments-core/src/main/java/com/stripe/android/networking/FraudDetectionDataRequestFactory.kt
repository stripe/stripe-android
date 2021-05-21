package com.stripe.android.networking

import android.content.Context
import androidx.annotation.VisibleForTesting

internal fun interface FraudDetectionDataRequestFactory {
    fun create(arg: FraudDetectionData?): FraudDetectionDataRequest
}

internal class DefaultFraudDetectionDataRequestFactory @VisibleForTesting internal constructor(
    private val fraudDetectionDataRequestParamsFactory: FraudDetectionDataRequestParamsFactory
) : FraudDetectionDataRequestFactory {

    internal constructor(context: Context) : this(
        fraudDetectionDataRequestParamsFactory = FraudDetectionDataRequestParamsFactory(context)
    )

    override fun create(arg: FraudDetectionData?): FraudDetectionDataRequest {
        return FraudDetectionDataRequest(
            params = fraudDetectionDataRequestParamsFactory.createParams(arg),
            guid = arg?.guid.orEmpty()
        )
    }
}
