package com.stripe.android.paymentsheet.specifications

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.specifications.FormItemSpec.SaveForFutureUseSpec
import com.stripe.android.paymentsheet.specifications.FormItemSpec.SetupIntentHiddenFields

internal val bancontactParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "bancontact",
    "billing_details" to billingParams
)

internal val bancontactNameSection =
    FormItemSpec.SectionSpec(IdentifierSpec("name"), SectionFieldSpec.Name)
internal val bancontactEmailSection =
    FormItemSpec.SectionSpec(IdentifierSpec("email"), SectionFieldSpec.Email)
internal val bancontactMandate = FormItemSpec.MandateTextSpec(
    IdentifierSpec("mandate"),
    R.string.sofort_mandate,
    Color.Gray
)
internal val bancontactSaveForFutureUse = SaveForFutureUseSpec(
    listOf(
        bancontactEmailSection, bancontactMandate
    )
)

val bancontact = FormSpec(
    LayoutSpec(
        listOf(
            bancontactNameSection,
            bancontactEmailSection,
            bancontactMandate,
            bancontactSaveForFutureUse,
            SetupIntentHiddenFields(
                bancontactSaveForFutureUse.identifierRequiredForFutureUse.plus(
                    bancontactSaveForFutureUse
                )
            )
        )
    ),
    bancontactParamKey,
)
