package com.stripe.android.core.frauddetection

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface FraudDetectionDataRequestFactory {
    fun create(arg: FraudDetectionData?): FraudDetectionDataRequest
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultFraudDetectionDataRequestFactory @VisibleForTesting internal constructor(
    private val fraudDetectionDataRequestParamsFactory: FraudDetectionDataRequestParamsFactory
) : FraudDetectionDataRequestFactory {

    constructor(context: Context) : this(
        fraudDetectionDataRequestParamsFactory = FraudDetectionDataRequestParamsFactory(context)
    )

    override fun create(arg: FraudDetectionData?): FraudDetectionDataRequest {
        return FraudDetectionDataRequest(
            params = fraudDetectionDataRequestParamsFactory.createParams(arg),
            guid = arg?.guid.orEmpty()
        )
    }
}
