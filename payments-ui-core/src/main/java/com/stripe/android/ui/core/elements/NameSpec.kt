package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
data class NameSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = DEFAULT_API_PATH,

    @SerialName("translation_id")
    val labelTranslationId: TranslationId = DEFAULT_LABEL_TRANSLATION_ID
) : FormItemSpec() {
    @Transient
    private val simpleTextSpec =
        SimpleTextSpec(
            apiPath,
            labelTranslationId.resourceId,
            Capitalization.Words,
            KeyboardType.Text
        )

    fun transform(initialValues: Map<IdentifierSpec, String?>) =
        simpleTextSpec.transform(initialValues)

    companion object {
        val DEFAULT_API_PATH = IdentifierSpec.Name
        val DEFAULT_LABEL_TRANSLATION_ID = TranslationId.AddressName
    }
}
