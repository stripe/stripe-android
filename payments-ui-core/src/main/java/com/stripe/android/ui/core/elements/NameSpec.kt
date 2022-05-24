package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
@Parcelize
data class NameSpec(
    override val api_path: IdentifierSpec = IdentifierSpec.Name,
    val label: TranslationId = TranslationId.AddressName
) : FormItemSpec(), RequiredItemSpec {
    @IgnoredOnParcel
    private val simpleTextSpec =
        SimpleTextSpec(api_path, label.resourceId, Capitalization.words, KeyboardType.text)

    fun transform(initialValues: Map<IdentifierSpec, String?>) =
        simpleTextSpec.transform(initialValues)
}
