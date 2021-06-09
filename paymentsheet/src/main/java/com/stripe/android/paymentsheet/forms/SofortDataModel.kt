package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.R

val sofortParams: MutableMap<String, Any?> = mutableMapOf(
    "country" to null,
)

val sofortParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "sofort",
    "billing_details" to billingParams,
    "sofort" to sofortParams
)

internal val sofort = FormDataObject(
    R.string.stripe_paymentsheet_add_payment_method_title,
    listOf(
        Section(
            R.string.address_label_name,
            listOf(Field.NameInput)
        ),
        Section(
            R.string.email,
            listOf(Field.EmailInput)
        )
    ),
    sofortParamKey,
)

