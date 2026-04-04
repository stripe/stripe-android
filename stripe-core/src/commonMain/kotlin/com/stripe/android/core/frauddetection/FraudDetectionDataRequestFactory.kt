package com.stripe.android.core.frauddetection

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface FraudDetectionDataRequestFactory {
    fun create(arg: FraudDetectionData?): FraudDetectionDataRequest
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultFraudDetectionDataRequestFactory(
    private val createParams: (FraudDetectionData?) -> Map<String, Any>
) : FraudDetectionDataRequestFactory {
    override fun create(arg: FraudDetectionData?): FraudDetectionDataRequest {
        return FraudDetectionDataRequest(
            params = createParams(arg),
            guid = arg?.guid.orEmpty()
        )
    }
}
