package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class DropdownSpec(
    override val identifier: IdentifierSpec,
    @StringRes val label: Int,
    val items: List<DropdownItemSpec>
) : SectionFieldSpec(identifier) {
    fun transform(initialValue: String?): SectionFieldElement =
        SimpleDropdownElement(
            this.identifier,
            DropdownFieldController(
                SimpleDropdownConfig(
                    label,
                    items
                ),
                initialValue = initialValue
            )
        )
}
