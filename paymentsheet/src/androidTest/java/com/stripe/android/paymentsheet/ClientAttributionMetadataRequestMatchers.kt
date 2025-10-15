package com.stripe.android.paymentsheet

import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers
import com.stripe.android.networktesting.RequestMatchers.bodyPart

internal fun clientAttributionMetadataParamsInPaymentMethodData(): RequestMatcher {
    return RequestMatchers.composite(
        bodyPart(
            urlEncode("payment_method_data[client_attribution_metadata][elements_session_config_id]"),
            urlEncode("elements_session_123")
        ),
        bodyPart(
            urlEncode("payment_method_data[client_attribution_metadata][payment_intent_creation_flow]"),
            urlEncode("standard")
        ),
        bodyPart(
            urlEncode("payment_method_data[client_attribution_metadata][payment_method_selection_flow]"),
            urlEncode("automatic")
        ),
        bodyPart(
            urlEncode("payment_method_data[client_attribution_metadata][merchant_integration_source]"),
            urlEncode("elements")
        ),
        bodyPart(
            urlEncode("payment_method_data[client_attribution_metadata][merchant_integration_subtype]"),
            urlEncode("mobile")
        ),
        bodyPart(
            urlEncode("payment_method_data[client_attribution_metadata][merchant_integration_version]"),
            urlEncode("stripe-android/${StripeSdkVersion.VERSION_NAME}")
        ),
        bodyPart(
            urlEncode("payment_method_data[client_attribution_metadata][client_session_id]"),
            urlEncode(AnalyticsRequestFactory.sessionId.toString())
        ),
    )
}

internal fun topLevelClientAttributionMetadataParams(): RequestMatcher {
    return RequestMatchers.composite(
        bodyPart(
            urlEncode("client_attribution_metadata[elements_session_config_id]"),
            urlEncode("elements_session_123")
        ),
        bodyPart(
            urlEncode("client_attribution_metadata[payment_intent_creation_flow]"),
            urlEncode("standard")
        ),
        bodyPart(
            urlEncode("client_attribution_metadata[payment_method_selection_flow]"),
            urlEncode("automatic")
        ),
        bodyPart(
            urlEncode("client_attribution_metadata[merchant_integration_source]"),
            urlEncode("elements")
        ),
        bodyPart(
            urlEncode("client_attribution_metadata[merchant_integration_subtype]"),
            urlEncode("mobile")
        ),
        bodyPart(
            urlEncode("client_attribution_metadata[merchant_integration_version]"),
            urlEncode("stripe-android/${StripeSdkVersion.VERSION_NAME}")
        ),
        bodyPart(
            urlEncode("client_attribution_metadata[client_session_id]"),
            urlEncode(AnalyticsRequestFactory.sessionId.toString())
        ),
    )
}

internal fun clientAttributionMetadataParamsForDeferredIntent(): RequestMatcher {
    return RequestMatchers.composite(
        bodyPart(
            urlEncode("client_attribution_metadata[elements_session_config_id]"),
            urlEncode("elements_session_123")
        ),
        bodyPart(
            urlEncode("client_attribution_metadata[payment_intent_creation_flow]"),
            urlEncode("deferred")
        ),
        bodyPart(
            urlEncode("client_attribution_metadata[payment_method_selection_flow]"),
            urlEncode("merchant_specified")
        ),
        bodyPart(
            urlEncode("client_attribution_metadata[merchant_integration_source]"),
            urlEncode("elements")
        ),
        bodyPart(
            urlEncode("client_attribution_metadata[merchant_integration_subtype]"),
            urlEncode("mobile")
        ),
        bodyPart(
            urlEncode("client_attribution_metadata[merchant_integration_version]"),
            urlEncode("stripe-android/${StripeSdkVersion.VERSION_NAME}")
        ),
        bodyPart(
            urlEncode("client_attribution_metadata[client_session_id]"),
            urlEncode(AnalyticsRequestFactory.sessionId.toString())
        ),
    )
}
