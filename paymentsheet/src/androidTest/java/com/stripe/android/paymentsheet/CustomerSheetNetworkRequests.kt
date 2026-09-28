package com.stripe.android.paymentsheet

import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query

internal const val TEST_CBC_CARD_NUMBER = "4000002500001001"

internal fun createPaymentMethodsRequest(): RequestMatcher {
    return RequestMatchers.composite(
        host("api.stripe.com"),
        method("POST"),
        path("/v1/payment_methods"),
    )
}

internal fun retrieveSetupIntentRequest(): RequestMatcher {
    return RequestMatchers.composite(
        host("api.stripe.com"),
        method("GET"),
        path("/v1/setup_intents/seti_12345"),
    )
}

internal fun confirmSetupIntentRequest(): RequestMatcher {
    return RequestMatchers.composite(
        host("api.stripe.com"),
        method("POST"),
        path("/v1/setup_intents/seti_12345/confirm")
    )
}

internal fun retrieveSetupIntentParams(): RequestMatcher {
    return RequestMatchers.composite(
        query("client_secret", "seti_12345_secret_12345"),
    )
}

internal fun billingDetailsParams(): RequestMatcher {
    return RequestMatchers.composite(
        bodyPart("billing_details[address][postal_code]", CustomerSheetPage.ZIP_CODE),
        bodyPart("billing_details[address][country]", CustomerSheetPage.COUNTRY)
    )
}

internal fun fullBillingDetailsParams(): RequestMatcher {
    return RequestMatchers.composite(
        bodyPart("billing_details[name]", CustomerSheetPage.NAME),
        bodyPart("billing_details[phone]", "+1${CustomerSheetPage.PHONE_NUMBER}"),
        bodyPart("billing_details[email]", CustomerSheetPage.EMAIL),
        bodyPart("billing_details[address][line1]", CustomerSheetPage.ADDRESS_LINE_ONE),
        bodyPart("billing_details[address][line2]", CustomerSheetPage.ADDRESS_LINE_TWO),
        bodyPart("billing_details[address][city]", CustomerSheetPage.CITY),
        bodyPart("billing_details[address][state]", CustomerSheetPage.STATE),
        billingDetailsParams()
    )
}

internal fun cardDetailsParams(
    cardNumber: String = CustomerSheetPage.CARD_NUMBER
): RequestMatcher {
    return RequestMatchers.composite(
        bodyPart("type", "card"),
        bodyPart("card[number]", cardNumber),
        bodyPart("card[exp_month]", CustomerSheetPage.EXPIRY_MONTH),
        bodyPart("card[exp_year]", CustomerSheetPage.EXPIRY_YEAR),
        bodyPart("card[cvc]", CustomerSheetPage.CVC),
    )
}

internal fun cardBrandChoiceParams(): RequestMatcher {
    return RequestMatchers.composite(
        bodyPart(
            "card[networks][preferred]",
            "cartes_bancaires"
        )
    )
}

internal fun confirmSetupIntentParams(): RequestMatcher {
    return RequestMatchers.composite(
        bodyPart("payment_method", "pm_12345"),
        bodyPart("client_secret", "seti_12345_secret_12345"),
        clientAttributionMetadataParams(),
    )
}

internal fun clientAttributionMetadataParams(): RequestMatcher {
    return RequestMatchers.composite(
        bodyPart(
            "client_attribution_metadata[elements_session_config_id]",
            "e961790f-43ed-4fcc-a534-74eeca28d042"
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
