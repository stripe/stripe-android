package com.stripe.android.ui.core.elements

import android.content.Context
import kotlinx.parcelize.Parcelize

@Parcelize
internal object CardDetailsSpec : SectionFieldSpec(IdentifierSpec.Generic("card_details")) {
    fun transform(
        context: Context,
        initialValues: Map<IdentifierSpec, String?>
    ): SectionFieldElement = CardDetailsElement(
        IdentifierSpec.Generic("credit_detail"),
        context,
        initialValues
    )
}
