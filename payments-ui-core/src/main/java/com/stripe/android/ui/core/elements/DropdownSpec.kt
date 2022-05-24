package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
@Parcelize
data class DropdownSpec(
    override val api_path: IdentifierSpec,
    val label_translation_id: TranslationId,
    val items: List<DropdownItemSpec>
) : FormItemSpec(), RequiredItemSpec {
    fun transform(
        initialValues: Map<IdentifierSpec, String?> = mapOf()
    ) = createSectionElement(
        SimpleDropdownElement(
            this.api_path,
            DropdownFieldController(
                SimpleDropdownConfig(
                    label_translation_id.resourceId,
                    items
                ),
                initialValue = initialValues[api_path]
            )
        )
    )
}
