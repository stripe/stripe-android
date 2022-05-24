package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("selector")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Parcelize
data class DropdownSpec(
    override val api_path: IdentifierSpec,
    val label: StringRepository.TranslationId,
    val items: List<DropdownItemSpec>
) : FormItemSpec(), RequiredItemSpec {
    fun transform(
        initialValues: Map<IdentifierSpec, String?> = mapOf()
    ) = createSectionElement(
        SimpleDropdownElement(
            this.api_path,
            DropdownFieldController(
                SimpleDropdownConfig(
                    label.resourceId,
                    items
                ),
                initialValue = initialValues[api_path]
            )
        )
    )
}
