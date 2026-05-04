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
    EuMica("eu_mica");

    companion object {
        fun fromValue(value: String): RegulationType? {
            return entries.firstOrNull { it.value == value.lowercase() }
        }
    }
}

/**
 * An identifier requirement returned by the Crypto Onramp API.
 */
@ExperimentalCryptoOnramp
@Poko
class IdentifierRequirement internal constructor(
    val type: IdentifierType,
    val regulation: RegulationType
)

/**
 * A set of alternative identifiers that can satisfy a missing requirement.
 */
@ExperimentalCryptoOnramp
@Poko
class AlternativeGroup internal constructor(
    val originalMissingIdentifiers: List<IdentifierType>,
    val alternativeMissingIdentifiers: List<IdentifierType>
)

/**
 * Identifier requirements returned by the Crypto Onramp API.
 */
@ExperimentalCryptoOnramp
@Poko
class IdentifierRequirements internal constructor(
    val identifiers: List<IdentifierRequirement>,
    val alternatives: List<AlternativeGroup>
)

/**
 * Validation result returned after updating KYC info.
 */
@ExperimentalCryptoOnramp
@Poko
class UpdateKycInfoResult internal constructor(
    val valid: Boolean,
    val identifiers: List<IdentifierRequirement>,
    val alternatives: List<AlternativeGroup>,
    val invalidIdentifiers: List<IdentifierType>
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
    @SerialName("identifiers")
    val identifiers: List<IdentifierRequirementResponse> = emptyList(),
    @SerialName("alternatives")
    val alternatives: List<AlternativeGroupResponse> = emptyList(),
) {
    fun toIdentifierRequirements(): IdentifierRequirements {
        return IdentifierRequirements(
            identifiers = identifiers.map { it.toIdentifierRequirement() },
            alternatives = alternatives.map { it.toAlternativeGroup() }
        )
    }
}

@Serializable
internal data class UpdateKycInfoResponse(
    @SerialName("valid")
    val valid: Boolean = true,
    @SerialName("identifiers")
    val identifiers: List<IdentifierRequirementResponse> = emptyList(),
    @SerialName("alternatives")
    val alternatives: List<AlternativeGroupResponse> = emptyList(),
    @SerialName("invalid_identifiers")
    val invalidIdentifiers: List<String> = emptyList(),
) {
    fun toUpdateKycInfoResult(): UpdateKycInfoResult {
        return UpdateKycInfoResult(
            valid = valid,
            identifiers = identifiers.map { it.toIdentifierRequirement() },
            alternatives = alternatives.map { it.toAlternativeGroup() },
            invalidIdentifiers = invalidIdentifiers.map { identifierType ->
                requireNotNull(IdentifierType.fromValue(identifierType)) {
                    "Unrecognized identifier type: $identifierType"
                }
            }
        )
    }
}

@Serializable
internal data class IdentifierRequirementResponse(
    @SerialName("type")
    val type: String,
    @SerialName("regulation")
    val regulation: String
) {
    fun toIdentifierRequirement(): IdentifierRequirement {
        return IdentifierRequirement(
            type = requireNotNull(IdentifierType.fromValue(type)) {
                "Unrecognized identifier type: $type"
            },
            regulation = requireNotNull(RegulationType.fromValue(regulation)) {
                "Unrecognized regulation type: $regulation"
            }
        )
    }
}

@Serializable
internal data class AlternativeGroupResponse(
    @SerialName("original_missing_identifiers")
    val originalMissingIdentifiers: List<String>,
    @SerialName("alternative_missing_identifiers")
    val alternativeMissingIdentifiers: List<String>
) {
    fun toAlternativeGroup(): AlternativeGroup {
        return AlternativeGroup(
            originalMissingIdentifiers = originalMissingIdentifiers.map { identifierType ->
                requireNotNull(IdentifierType.fromValue(identifierType)) {
                    "Unrecognized identifier type: $identifierType"
                }
            },
            alternativeMissingIdentifiers = alternativeMissingIdentifiers.map { identifierType ->
                requireNotNull(IdentifierType.fromValue(identifierType)) {
                    "Unrecognized identifier type: $identifierType"
                }
            }
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
