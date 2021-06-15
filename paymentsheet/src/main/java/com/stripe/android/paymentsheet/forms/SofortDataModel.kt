package com.stripe.android.paymentsheet.forms

internal val sofortParams: MutableMap<String, Any?> = mutableMapOf(
    "country" to null,
)

internal val sofortParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "sofort",
    "billing_details" to billingParams,
    "sofort" to sofortParams
)

val sofort = FormDataObject(
    VisualFieldLayout(
        listOf(
            Section(
                Field.NameInput
            ),
            Section(
                Field.EmailInput
            ),
            Section(
                Field.CountryInput
            )
        )
    ),
    sofortParamKey,
)

