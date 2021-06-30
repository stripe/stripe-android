package com.stripe.android.paymentsheet.specifications

import com.stripe.android.paymentsheet.specifications.FormItemSpec.SaveForFutureUseSpec

internal val bancontactParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "bancontact",
    "billing_details" to billingParams
)

internal val bancontactNameSection =
    FormItemSpec.SectionSpec(IdentifierSpec("name"), SectionFieldSpec.Name)
internal val bancontactEmailSection =
    FormItemSpec.SectionSpec(IdentifierSpec("email"), SectionFieldSpec.Email)
val bancontact = FormSpec(
    LayoutSpec(
        listOf(
            bancontactNameSection,
            bancontactEmailSection,
            SaveForFutureUseSpec(
                listOf(
                    bancontactEmailSection
                )
            )
        )
    ),
    bancontactParamKey,
)
