package com.stripe.android.core.frauddetection

import android.content.Context
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun createFraudDetectionDataRequestFactory(
    context: Context
): FraudDetectionDataRequestFactory {
    val paramsFactory = FraudDetectionDataRequestParamsFactory(context)
    return DefaultFraudDetectionDataRequestFactory(paramsFactory::createParams)
}
