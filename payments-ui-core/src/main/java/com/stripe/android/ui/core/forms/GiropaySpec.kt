package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.SimpleTextSpec
import com.stripe.android.ui.core.elements.billingParams

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val GiropayParamKey: MutableMap<String, Any?> = mutableMapOf(
    "type" to "giropay",
    "billing_details" to billingParams,
)

internal val giropayNameSection = SectionSpec(
    IdentifierSpec.Generic("name_section"),
    SimpleTextSpec.NAME
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val GiropayForm = LayoutSpec.create(giropayNameSection)
