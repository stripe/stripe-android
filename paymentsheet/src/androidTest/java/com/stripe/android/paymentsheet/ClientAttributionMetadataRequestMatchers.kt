package com.stripe.android.paymentsheet

import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers
import com.stripe.android.networktesting.RequestMatchers.bodyPart

internal fun clientAttributionMetadataParamsInPaymentMethodData(): RequestMatcher {
    return RequestMatchers.composite(
        bodyPart(
            "payment_method_data[client_attribution_metadata][elements_session_config_id]",
            "e961790f-43ed-4fcc-a534-74eeca28d042"
        ),
        bodyPart(
            "payment_method_data[client_attribution_metadata][payment_intent_creation_flow]",
            "standard"
        ),
        bodyPart(
            "payment_method_data[client_attribution_metadata][payment_method_selection_flow]",
            "automatic"
        ),
        bodyPart(
            "payment_method_data[client_attribution_metadata][merchant_integration_source]",
            "elements"
        ),
        bodyPart(
            "payment_method_data[client_attribution_metadata][merchant_integration_subtype]",
            "mobile"
        ),
        bodyPart(
            "payment_method_data[client_attribution_metadata][merchant_integration_version]",
            "stripe-android/${StripeSdkVersion.VERSION_NAME}"
        ),
        bodyPart(
            "payment_method_data[client_attribution_metadata][client_session_id]",
            AnalyticsRequestFactory.sessionId.toString()
        ),
    )
}

internal fun topLevelClientAttributionMetadataParams(): RequestMatcher {
    return RequestMatchers.composite(
        bodyPart(
            "client_attribution_metadata[elements_session_config_id]",
            "e961790f-43ed-4fcc-a534-74eeca28d042"
        ),
        bodyPart(
            "client_attribution_metadata[payment_intent_creation_flow]",
            "standard"
        ),
        bodyPart(
            "client_attribution_metadata[payment_method_selection_flow]",
            "automatic"
        ),
        bodyPart(
            "client_attribution_metadata[merchant_integration_source]",
            "elements"
        ),
        bodyPart(
            "client_attribution_metadata[merchant_integration_subtype]",
            "mobile"
        ),
        bodyPart(
            "client_attribution_metadata[merchant_integration_version]",
            "stripe-android/${StripeSdkVersion.VERSION_NAME}"
        ),
        bodyPart(
            "client_attribution_metadata[client_session_id]",
            AnalyticsRequestFactory.sessionId.toString()
        ),
    )
}

internal fun clientAttributionMetadataParamsForDeferredIntent(): RequestMatcher {
    return RequestMatchers.composite(
        bodyPart(
            "client_attribution_metadata[elements_session_config_id]",
            "e961790f-43ed-4fcc-a534-74eeca28d042"
        ),
        bodyPart(
            "client_attribution_metadata[payment_intent_creation_flow]",
            "deferred"
        ),
        bodyPart(
            "client_attribution_metadata[payment_method_selection_flow]",
            "merchant_specified"
        ),
        bodyPart(
            "client_attribution_metadata[merchant_integration_source]",
            "elements"
        ),
        bodyPart(
            "client_attribution_metadata[merchant_integration_subtype]",
            "mobile"
        ),
        bodyPart(
            "client_attribution_metadata[merchant_integration_version]",
            "stripe-android/${StripeSdkVersion.VERSION_NAME}"
        ),
        bodyPart(
            "client_attribution_metadata[client_session_id]",
            AnalyticsRequestFactory.sessionId.toString()
        ),
    )
}
