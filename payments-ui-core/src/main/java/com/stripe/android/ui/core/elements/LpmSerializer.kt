package com.stripe.android.ui.core.elements

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.serializer


class LpmSerializer {
    private val format = Json {
        ignoreUnknownKeys = true
        serializersModule = module
    }

    fun serialize(data: SharedDataSpec) =
        format.encodeToJsonElement(serializer(), data)

    fun deserialize(str: String) = runCatching {
        format.decodeFromString<SharedDataSpec>(serializer(), str)
    }.onFailure { }

    fun deserializeList(str: String) =
        format.decodeFromString<List<SharedDataSpec>>(serializer(), str)

    companion object {
        val module = SerializersModule {
            polymorphic(FormItemSpec::class) {
                subclass(AddressSpec::class)
                subclass(AffirmTextSpec::class)
                subclass(AfterpayClearpayTextSpec::class)
                subclass(BsbSpec::class)
                subclass(AuBankAccountNumberSpec::class)
                subclass(AuBecsDebitMandateTextSpec::class)
                subclass(CountrySpec::class)
                subclass(DropdownSpec::class)
                subclass(EmailSpec::class)
                subclass(IbanSpec::class)
                subclass(KlarnaCountrySpec::class)
                subclass(StaticTextSpec::class)
                subclass(NameSpec::class)
                subclass(MandateTextSpec::class)
                subclass(SimpleTextSpec::class)
                subclass(CardDetailsSectionSpec::class)
                subclass(CardBillingSpec::class)
                //                    subclass(SofortAddressField::class)
            }
        }
    }
}
