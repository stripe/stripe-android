package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo
import com.stripe.android.elements.Address
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class KycInfo(
    val firstName: String,
    val lastName: String,
    val idNumber: String?,
    val idType: IdType?,

    @SerialName("date_of_birth")
    val dateOfBirth: DateOfBirth,

    @Serializable(with = PaymentSheetAddressSerializer::class)
    val address: Address,
    val nationalities: List<String>?,
    val birthCountry: String?,
    val birthCity: String?
)

@Serializable
internal data class DateOfBirth(
    val day: Int,
    val month: Int,
    val year: Int
)

@Suppress("UnusedPrivateProperty")
internal enum class IdType(private val value: String) {
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
