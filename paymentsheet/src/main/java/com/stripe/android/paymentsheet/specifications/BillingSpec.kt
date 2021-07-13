package com.stripe.android.paymentsheet.specifications

internal val addressParams: MutableMap<String, Any?> = mutableMapOf(
    "city" to null,
    "country" to null,
    "line1" to null,
    "line2" to null,
    "postal_code" to null,
    "state" to null,
)

internal val billingParams: MutableMap<String, Any?> = mutableMapOf(
    "address" to addressParams,
    "name" to null,
    "email" to null,
    "phone" to null,
)

internal val billingUS = FormSpec(
    LayoutSpec(
        listOf(
            FormItemSpec.SectionSpec(IdentifierSpec("name"), SectionFieldSpec.Name),
            FormItemSpec.SectionSpec(
                IdentifierSpec("billingSection"),
                SectionFieldSpec.GenericText(IdentifierSpec("city"))
            ),
            FormItemSpec.SectionSpec(
                IdentifierSpec("country"), SectionFieldSpec.Country()
            ),
        )
    ),
    sofortParamKey,
)
