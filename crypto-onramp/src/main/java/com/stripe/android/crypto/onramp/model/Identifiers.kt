package com.stripe.android.crypto.onramp.model

import com.stripe.android.core.model.CountryCode
import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Identifier payload submitted to the Crypto Onramp API.
 *
 * The current backend path is still specialized, so callers continue to separate
 * MiCA and CARF submissions even though individual identifiers are modeled with
 * the reviewed typed enum surface.
 *
 * @property identifiersMica MiCA identifiers for the consumer.
 * @property identifiersCarf CARF identifiers for the consumer.
 */
@ExperimentalCryptoOnramp
class Identifiers {
    private var identifiersMica: List<Identifier>? = null
    private var identifiersCarf: List<Identifier>? = null

    /**
     * Sets MiCA identifiers for the consumer.
     */
    fun identifiersMica(identifiersMica: List<Identifier>?) = apply {
        this.identifiersMica = identifiersMica
    }

    /**
     * Sets CARF identifiers for the consumer.
     */
    fun identifiersCarf(identifiersCarf: List<Identifier>?) = apply {
        this.identifiersCarf = identifiersCarf
    }

    internal class State(
        val identifiersMica: List<Identifier.State>?,
        val identifiersCarf: List<Identifier.State>?
    )

    internal fun build(): State {
        return State(
            identifiersMica = identifiersMica?.map { it.build() },
            identifiersCarf = identifiersCarf?.map { it.build() }
        )
    }
}

/**
 * A typed identifier entry.
 *
 * @property type The reviewed identifier type.
 * @property value The identifier value.
 */
@ExperimentalCryptoOnramp
class Identifier {
    private var type: IdentifierType? = null
    private var value: String? = null

    /**
     * Sets the identifier type.
     */
    fun type(type: IdentifierType) = apply {
        this.type = type
    }

    /**
     * Sets the identifier value.
     */
    fun value(value: String) = apply {
        this.value = value
    }

    internal class State(
        val type: IdentifierType,
        val value: String
    )

    internal fun build(): State {
        return State(
            type = requireNotNull(type) {
                "type must not be null"
            },
            value = requireNotNull(value) {
                "value must not be null"
            }
        )
    }
}

/**
 * Supported identifier types for EU onramp compliance.
 */
@ExperimentalCryptoOnramp
enum class IdentifierType(
    val value: String,
    private val countryCodeValue: String
) {
    AT_STN("at_stn", "AT"),
    BE_NRN("be_nrn", "BE"),
    BG_UCN("bg_ucn", "BG"),
    HR_OIB("hr_oib", "HR"),
    CY_TIC("cy_tic", "CY"),
    CZ_RC("cz_rc", "CZ"),
    DK_CPR("dk_cpr", "DK"),
    EE_IK("ee_ik", "EE"),
    FI_HETU("fi_hetu", "FI"),
    FR_SPI("fr_spi", "FR"),
    DE_STN("de_stn", "DE"),
    GR_AFM("gr_afm", "GR"),
    HU_AD("hu_ad", "HU"),
    IS_KT("is_kt", "IS"),
    IE_PPSN("ie_ppsn", "IE"),
    IT_CF("it_cf", "IT"),
    LV_PK("lv_pk", "LV"),
    LT_AK("lt_ak", "LT"),
    LU_NIF("lu_nif", "LU"),
    MT_NIC("mt_nic", "MT"),
    MT_PP("mt_pp", "MT"),
    NL_BSN("nl_bsn", "NL"),
    PL_PESEL("pl_pesel", "PL"),
    PL_NIP("pl_nip", "PL"),
    PT_NIF("pt_nif", "PT"),
    RO_CNP("ro_cnp", "RO"),
    SK_RC("sk_rc", "SK"),
    SI_PIN("si_pin", "SI"),
    SE_PIN("se_pin", "SE");

    internal val countryCode: CountryCode
        get() = CountryCode.create(countryCodeValue)

    companion object {
        fun fromValue(value: String): IdentifierType? {
            return entries.firstOrNull { it.value == value.lowercase() }
        }
    }
}

/**
 * Regulations that can require identifier collection.
 */
@ExperimentalCryptoOnramp
enum class RegulationType(val value: String) {
    EuCarf("eu_carf"),
    EuMica("eu_mica")
}

/**
 * A required identifier along with the merchant-facing placeholder shown to the user.
 */
@ExperimentalCryptoOnramp
@Poko
class IdentifierHint internal constructor(
    val type: IdentifierType,
    val placeholder: String
)

/**
 * An identifier that is still required for a specific regulation.
 */
@ExperimentalCryptoOnramp
@Poko
class MissingIdentifier internal constructor(
    val type: IdentifierType,
    val placeholder: String,
    val alternateIdentifier: IdentifierHint?,
    val regulation: RegulationType
)

/**
 * Identifier requirements returned by the Crypto Onramp API.
 *
 * @property missingIdentifiers Missing MiCA and CARF identifiers still required.
 */
@ExperimentalCryptoOnramp
@Poko
class IdentifierRequirements internal constructor(
    val missingIdentifiers: List<MissingIdentifier>
)

/**
 * Validation result returned after updating KYC info.
 *
 * @property valid Whether the submitted identifiers were accepted.
 * @property missingIdentifiers Missing identifiers, if more are required.
 * @property invalidIdentifiers Submitted identifier types rejected by the API.
 */
@ExperimentalCryptoOnramp
@Poko
class UpdateKycInfoResult internal constructor(
    val valid: Boolean,
    val missingIdentifiers: List<MissingIdentifier>?,
    val invalidIdentifiers: List<IdentifierType>?
)

@Serializable
internal data class IdentifiersRequest(
    @SerialName("credentials")
    val credentials: CryptoCustomerRequestParams.Credentials,
    @SerialName("identifiers_mica")
    val identifiersMica: List<IdentifierRequest>? = null,
    @SerialName("identifiers_carf")
    val identifiersCarf: List<IdentifierRequest>? = null
)

@Serializable
internal data class IdentifierRequest(
    @SerialName("country")
    val country: String,
    @SerialName("identifier")
    val identifier: String,
    @SerialName("identifier_type")
    val identifierType: String
)

@Serializable
internal data class IdentifierRequirementsResponse(
    @SerialName("missing_identifiers_mica")
    val missingIdentifiersMica: List<MissingIdentifierResponse> = emptyList(),
    @SerialName("missing_identifiers_carf")
    val missingIdentifiersCarf: List<MissingIdentifierResponse> = emptyList(),
) {
    fun toIdentifierRequirements(): IdentifierRequirements {
        return IdentifierRequirements(
            missingIdentifiers = missingIdentifiersMica.map {
                it.toMissingIdentifier(RegulationType.EuMica)
            } + missingIdentifiersCarf.map {
                it.toMissingIdentifier(RegulationType.EuCarf)
            }
        )
    }
}

@Serializable
internal data class UpdateKycInfoResponse(
    @SerialName("valid")
    val valid: Boolean = true,
    @SerialName("missing_identifiers")
    val missingIdentifiers: MissingIdentifiersResponse? = null,
    @SerialName("errors")
    val errors: List<String>? = null,
) {
    fun toUpdateKycInfoResult(): UpdateKycInfoResult {
        return UpdateKycInfoResult(
            valid = valid,
            missingIdentifiers = missingIdentifiers?.toMissingIdentifiers(),
            invalidIdentifiers = errors?.map { identifierType ->
                requireNotNull(IdentifierType.fromValue(identifierType)) {
                    "Unrecognized identifier type: $identifierType"
                }
            }
        )
    }
}

@Serializable
internal data class MissingIdentifiersResponse(
    @SerialName("missing_identifiers_mica")
    val missingIdentifiersMica: List<MissingIdentifierResponse> = emptyList(),
    @SerialName("missing_identifiers_carf")
    val missingIdentifiersCarf: List<MissingIdentifierResponse> = emptyList(),
) {
    fun toMissingIdentifiers(): List<MissingIdentifier> {
        return missingIdentifiersMica.map {
            it.toMissingIdentifier(RegulationType.EuMica)
        } + missingIdentifiersCarf.map {
            it.toMissingIdentifier(RegulationType.EuCarf)
        }
    }
}

@Serializable
internal data class MissingIdentifierResponse(
    @SerialName("type")
    val type: String,
    @SerialName("placeholder")
    val placeholder: String,
    @SerialName("alternate_identifier")
    val alternateIdentifier: IdentifierHintResponse? = null
) {
    fun toMissingIdentifier(regulation: RegulationType): MissingIdentifier {
        val identifierType = requireNotNull(IdentifierType.fromValue(type)) {
            "Unrecognized identifier type: $type"
        }

        return MissingIdentifier(
            type = identifierType,
            placeholder = placeholder,
            alternateIdentifier = alternateIdentifier?.toIdentifierHint(),
            regulation = regulation
        )
    }
}

@Serializable
internal data class IdentifierHintResponse(
    @SerialName("type")
    val type: String,
    @SerialName("placeholder")
    val placeholder: String
) {
    fun toIdentifierHint(): IdentifierHint {
        return IdentifierHint(
            type = requireNotNull(IdentifierType.fromValue(type)) {
                "Unrecognized identifier type: $type"
            },
            placeholder = placeholder
        )
    }
}

internal fun Identifiers.toRequest(
    credentials: CryptoCustomerRequestParams.Credentials
): IdentifiersRequest {
    val state = build()

    return IdentifiersRequest(
        credentials = credentials,
        identifiersMica = state.identifiersMica?.map { it.toRequest() },
        identifiersCarf = state.identifiersCarf?.map { it.toRequest() }
    )
}

private fun Identifier.State.toRequest(): IdentifierRequest {
    return IdentifierRequest(
        country = type.countryCode.value,
        identifier = value,
        identifierType = type.value
    )
}
