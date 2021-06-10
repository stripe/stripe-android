package com.stripe.android.paymentsheet.forms

val sofortParams: MutableMap<String, Any?> = mutableMapOf(
    "country" to null,
)

val sofortParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "sofort",
    "billing_details" to billingParams,
    "sofort" to sofortParams
)

internal val sofort = FormDataObject(
    listOf(
        Section(
            Field.NameInput
        ),
        Section(
            Field.EmailInput
        )
    ),
    sofortParamKey,
)

