package com.stripe.android.paymentsheet.elements

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

/**
 * This represents a section in a form that contains other elements
 */
@Parcelize
internal data class SectionSpec(
    override val identifier: IdentifierSpec,
    val fields: List<SectionFieldSpec>,
    @StringRes val title: Int? = null,
) : FormItemSpec(), RequiredItemSpec, Parcelable {
    constructor(
        identifier: IdentifierSpec,
        field: SectionFieldSpec,
        title: Int? = null,
    ) : this(identifier, listOf(field), title)
}
