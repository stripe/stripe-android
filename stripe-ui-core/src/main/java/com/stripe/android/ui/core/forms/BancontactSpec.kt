package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.SimpleTextSpec
import com.stripe.android.ui.core.elements.StaticTextSpec
import com.stripe.android.ui.core.elements.billingParams

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val BancontactParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "bancontact",
    "billing_details" to billingParams
)

internal val bancontactNameSection = SectionSpec(
    IdentifierSpec.Generic("name_section"),
    SimpleTextSpec.NAME
)
internal val bancontactEmailSection =
    SectionSpec(IdentifierSpec.Generic("email_section"), EmailSpec)
internal val bancontactMandate = StaticTextSpec(
    IdentifierSpec.Generic("mandate"),
    R.string.sepa_mandate,
    R.color.mandate_text_color
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val BancontactForm = LayoutSpec.create(
    bancontactNameSection,
    bancontactEmailSection,
    SaveForFutureUseSpec(
        listOf(
            bancontactEmailSection, bancontactMandate
        )
    ),
    bancontactMandate,
)
