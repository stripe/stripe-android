package com.stripe.android.ui.core.elements

import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

/**
 * This represents a section in a form that contains other elements
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class SectionSpec(
    override val api_path: IdentifierSpec,
    val fields: List<SectionFieldSpec>,
    @StringRes val title: Int? = null,
) : FormItemSpec(), RequiredItemSpec, Parcelable {
    constructor(
        identifier: IdentifierSpec,
        field: SectionFieldSpec,
        title: Int? = null,
    ) : this(identifier, listOf(field), title)
}
