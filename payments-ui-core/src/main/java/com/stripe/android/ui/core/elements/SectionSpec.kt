package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes

/**
 * This represents a section in a form that contains other elements
 */
internal data class SectionSpec(
    override val identifier: IdentifierSpec,
    val fields: List<SectionFieldSpec>,
    @StringRes val title: Int? = null,
) : FormItemSpec(), RequiredItemSpec {
    constructor(
        identifier: IdentifierSpec,
        field: SectionFieldSpec,
        title: Int? = null,
    ) : this(identifier, listOf(field), title)
}
