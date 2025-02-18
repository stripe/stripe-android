package com.stripe.android.paymentsheet

import com.stripe.android.core.utils.urlEncode
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
        bodyPart(urlEncode("billing_details[address][postal_code]"), CustomerSheetPage.ZIP_CODE),
        bodyPart(urlEncode("billing_details[address][country]"), CustomerSheetPage.COUNTRY)
    )
}

internal fun fullBillingDetailsParams(): RequestMatcher {
    return RequestMatchers.composite(
        bodyPart(urlEncode("billing_details[name]"), urlEncode(CustomerSheetPage.NAME)),
        bodyPart(urlEncode("billing_details[phone]"), urlEncode("+1${CustomerSheetPage.PHONE_NUMBER}")),
        bodyPart(urlEncode("billing_details[email]"), urlEncode(CustomerSheetPage.EMAIL)),
        bodyPart(urlEncode("billing_details[address][line1]"), urlEncode(CustomerSheetPage.ADDRESS_LINE_ONE)),
        bodyPart(urlEncode("billing_details[address][line2]"), urlEncode(CustomerSheetPage.ADDRESS_LINE_TWO)),
        bodyPart(urlEncode("billing_details[address][city]"), urlEncode(CustomerSheetPage.CITY)),
        bodyPart(urlEncode("billing_details[address][state]"), urlEncode(CustomerSheetPage.STATE)),
        billingDetailsParams()
    )
}

internal fun cardDetailsParams(
    cardNumber: String = CustomerSheetPage.CARD_NUMBER
): RequestMatcher {
    return RequestMatchers.composite(
        bodyPart("type", "card"),
        bodyPart(urlEncode("card[number]"), cardNumber),
        bodyPart(urlEncode("card[exp_month]"), CustomerSheetPage.EXPIRY_MONTH),
        bodyPart(urlEncode("card[exp_year]"), CustomerSheetPage.EXPIRY_YEAR),
        bodyPart(urlEncode("card[cvc]"), CustomerSheetPage.CVC),
    )
}

internal fun cardBrandChoiceParams(): RequestMatcher {
    return RequestMatchers.composite(
        bodyPart(
            urlEncode("card[networks][preferred]"),
            "cartes_bancaires"
        )
    )
}

internal fun confirmSetupIntentParams(): RequestMatcher {
    return RequestMatchers.composite(
        bodyPart("payment_method", "pm_12345"),
        bodyPart("client_secret", "seti_12345_secret_12345"),
    )
}
