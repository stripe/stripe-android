package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
@Parcelize
data class NameSpec(
    @SerialName("api_path")
    override val apiPath: IdentifierSpec = IdentifierSpec.Name,

    @SerialName("translation_id")
    val labelTranslationId: TranslationId = TranslationId.AddressName
) : FormItemSpec() {
    @IgnoredOnParcel
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
}
