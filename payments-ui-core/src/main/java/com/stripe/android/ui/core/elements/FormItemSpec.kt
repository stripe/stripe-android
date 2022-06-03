package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * This is used to define each section in the visual form layout specification
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable(with = FormItemSpecSerializer::class)
sealed class FormItemSpec {
    @SerialName("api_path")
    abstract val apiPath: IdentifierSpec

    internal fun createSectionElement(
        sectionFieldElement: SectionFieldElement,
        label: Int? = null
    ) =
        SectionElement(
            IdentifierSpec.Generic("${sectionFieldElement.identifier.v1}_section"),
            sectionFieldElement,
            SectionController(
                label,
                listOf(sectionFieldElement.sectionFieldErrorController())
            )
        )
}

object FormItemSpecSerializer :
    JsonContentPolymorphicSerializer<FormItemSpec>(FormItemSpec::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out FormItemSpec> {
        return when (element.jsonObject["type"]?.jsonPrimitive?.content) {
            "billing_address" -> AddressSpec.serializer()
            "affirm_header" -> AffirmTextSpec.serializer()
            "afterpay_header" -> AfterpayClearpayTextSpec.serializer()
            "au_becs_bsb_number" -> BsbSpec.serializer()
            "au_becs_account_number" -> AuBankAccountNumberSpec.serializer()
            "au_becs_mandate" -> AuBecsDebitMandateTextSpec.serializer()
            "country" -> CountrySpec.serializer()
            "selector" -> DropdownSpec.serializer()
            "email" -> EmailSpec.serializer()
            "iban" -> IbanSpec.serializer()
            "klarna_country" -> KlarnaCountrySpec.serializer()
            "klarna_header" -> KlarnaHeaderStaticTextSpec.serializer()
            "static_text" -> StaticTextSpec.serializer()
            "name" -> NameSpec.serializer()
            "mandate" -> MandateTextSpec.serializer()
            "sepa_mandate" -> SepaMandateTextSpec.serializer()
            "text" -> SimpleTextSpec.serializer()
            "card_details" -> CardDetailsSectionSpec.serializer()
            "card_billing" -> CardBillingSpec.serializer()
            else -> EmptyFormSpec.serializer()
        }
    }
}
