package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("selector")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class DropdownSpec(
    override val api_path: IdentifierSpec,
    @StringRes val label: Int,
    val items: List<DropdownItemSpec>
) : FormItemSpec(), RequiredItemSpec {
    fun transform(
        initialValues: Map<IdentifierSpec, String?> = mapOf()
    ) = createSectionElement(
        SimpleDropdownElement(
            this.api_path,
            DropdownFieldController(
                SimpleDropdownConfig(
                    label,
                    items
                ),
                initialValue = initialValues[api_path]
            )
        )
    )
}
