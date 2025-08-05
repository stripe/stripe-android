package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class KycInfo(
    @SerialName("first_name")
    val firstName: String,

    @SerialName("last_name")
    val lastName: String,

    @SerialName("id_number")
    val idNumber: String?,

    @SerialName("id_type")
    @Serializable(with = IdTypeSerializer::class)
    val idType: IdType?,

    @SerialName("dob")
    val dateOfBirth: DateOfBirth,

    @Serializable(with = PaymentSheetAddressSerializer::class)
    val address: PaymentSheet.Address,
    val nationalities: List<String>?,

    @SerialName("birth_country")
    val birthCountry: String?,

    @SerialName("birth_city")
    val birthCity: String?
)

@Serializable
data class DateOfBirth(
    val day: Int,
    val month: Int,
    val year: Int
)

@Suppress("UnusedPrivateProperty")
enum class IdType(internal val value: String) {
    AADHAAR("aadhaar"),
    ABN("abn"),
    BUSINESS_TAX_DEDUCTION_ACCOUNT_NUMBER("business_tax_deduction_account_number"),
    COMPANY_REGISTRATION_NUMBER("company_registration_number"),
    CORPORATE_IDENTITY_NUMBER("corporate_identity_number"),
    GOODS_AND_SERVICES_TAX_ID_NUMBER("goods_and_services_tax_id_number"),
    INDIA_IMPORTER_EXPORTER_CODE("india_importer_exporter_code"),
    EXPORT_LICENSE_ID("export_license_id"),
    LEGACY_ID_NUMBER("id_number"),
    LIMITED_LIABILITY_PARTNERSHIP_ID("limited_liability_partnership_id"),
    PAN("pan"),
    UDYAM_NUMBER("udyam_number"),
    TAX_ID("tax_id"),
    VAT_ID("vat_id"),
    VOTER_ID("voter_id"),
    BRAZIL_CPF("brazil_cpf"),
    BRAZIL_REGISTRO_GERAL("brazil_registro_geral"),
    SPANISH_PERSON_NUMBER("spanish_person_number"),
    TH_LASER_CODE("th_laser_code"),
    FISCAL_CODE("fiscal_code"),
    SOCIAL_SECURITY_NUMBER("social_security_number"),
    REGON_NUMBER("regon_number"),
    PASSPORT_NUMBER("passport_number"),
    DRIVING_LICENSE_NUMBER("driving_license_number"),
    PHOTO_ID_NUMBER("photo_id_number")
}

private object IdTypeSerializer : KSerializer<IdType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("IdType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: IdType) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): IdType {
        val raw = decoder.decodeString()
        return IdType.entries.firstOrNull { it.value == raw }
            ?: throw SerializationException("Unknown IdType: $raw")
    }
}
