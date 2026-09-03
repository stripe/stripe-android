package com.stripe.android.ui.core.elements

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SectionFieldElement
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement

/**
 * This is used to define each section in the visual form layout specification
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable(with = FormItemSpecSerializer::class)
sealed class FormItemSpec : Parcelable {
    @SerialName("api_path")
    abstract val apiPath: IdentifierSpec

    internal fun createSectionElement(
        sectionFieldElement: SectionFieldElement,
        label: ResolvableString? = null
    ): SectionElement = SectionElement.wrap(sectionFieldElement, label)

    internal fun createSectionElement(
        sectionFieldElements: List<SectionFieldElement>,
        label: ResolvableString? = null
    ): SectionElement = SectionElement.wrap(sectionFieldElements, label)
}

object FormItemSpecSerializer :
    JsonContentPolymorphicSerializer<FormItemSpec>(FormItemSpec::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<FormItemSpec> {
        throw SerializationException("Unsupported FormItemSpec: $element")
    }
}
