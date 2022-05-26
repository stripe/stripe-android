package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
@Parcelize
data class NameSpec(
    override val apiPath: IdentifierSpec = IdentifierSpec.Name,

    @SerialName("label_translation_id")
    val labelTranslationId: TranslationId = TranslationId.AddressName
) : FormItemSpec(), RequiredItemSpec {
    @IgnoredOnParcel
    private val simpleTextSpec =
        SimpleTextSpec(apiPath, labelTranslationId.resourceId, Capitalization.Words, KeyboardType.Text)

    fun transform(initialValues: Map<IdentifierSpec, String?>) =
        simpleTextSpec.transform(initialValues)
}
