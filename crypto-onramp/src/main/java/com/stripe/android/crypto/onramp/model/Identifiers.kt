package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val value: String
) {
    AT_STN("at_stn"),
    BE_NRN("be_nrn"),
    BG_UCN("bg_ucn"),
    HR_OIB("hr_oib"),
    CY_TIC("cy_tic"),
    CZ_RC("cz_rc"),
    DK_CPR("dk_cpr"),
    EE_IK("ee_ik"),
    FI_HETU("fi_hetu"),
    FR_SPI("fr_spi"),
    DE_STN("de_stn"),
    GR_AFM("gr_afm"),
    HU_AD("hu_ad"),
    IS_KT("is_kt"),
    IE_PPSN("ie_ppsn"),
    IT_CF("it_cf"),
    LV_PK("lv_pk"),
    LT_AK("lt_ak"),
    LU_NIF("lu_nif"),
    MT_NIC("mt_nic"),
    MT_PP("mt_pp"),
    NL_BSN("nl_bsn"),
    PL_PESEL("pl_pesel"),
    PL_NIP("pl_nip"),
    PT_NIF("pt_nif"),
    RO_CNP("ro_cnp"),
    SK_RC("sk_rc"),
    SI_PIN("si_pin"),
    SE_PIN("se_pin");

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
internal data class UpdateKycInfoRequest(
    val credentials: CryptoCustomerRequestParams.Credentials,
    val identifiers: List<IdentifierRequest>
)

@Serializable
internal data class IdentifierRequest(
    val type: String,
    val value: String
)

@Serializable
internal data class IdentifierRequirementsResponse(
    val identifiers: List<IdentifierRequirementResponse> = emptyList(),
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
    val valid: Boolean = true,
    val identifiers: List<IdentifierRequirementResponse> = emptyList(),
    val alternatives: List<AlternativeGroupResponse> = emptyList(),
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
    val type: String,
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

internal fun List<Identifier>.toRequest(
    credentials: CryptoCustomerRequestParams.Credentials
): UpdateKycInfoRequest {
    return UpdateKycInfoRequest(
        credentials = credentials,
        identifiers = map { it.build().toRequest() }
    )
}

private fun Identifier.State.toRequest(): IdentifierRequest {
    return IdentifierRequest(
        type = type.value,
        value = value
    )
}
