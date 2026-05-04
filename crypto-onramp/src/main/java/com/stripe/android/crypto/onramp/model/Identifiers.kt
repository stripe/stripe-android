package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A compliance identifier collected for MiCA or CRS/CARF compliance.
 *
 * @property type The type of identifier provided.
 * @property value The identifier value.
 */
@ExperimentalCryptoOnramp
class ComplianceIdentifier {
    private var type: ComplianceIdentifierType? = null
    private var value: String? = null

    /**
     * Sets the identifier type.
     */
    fun type(type: ComplianceIdentifierType) = apply {
        this.type = type
    }

    /**
     * Sets the identifier value.
     */
    fun value(value: String) = apply {
        this.value = value
    }

    internal class State(
        val type: ComplianceIdentifierType,
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
 * The type of compliance identifier required or submitted for regulatory compliance.
 */
@ExperimentalCryptoOnramp
@Poko
class ComplianceIdentifierType(
    val value: String
) {
    init {
        require(value.isNotBlank()) {
            "value must not be blank"
        }
    }

    companion object {
        val AT_STN = ComplianceIdentifierType("at_stn")
        val BE_NRN = ComplianceIdentifierType("be_nrn")
        val BG_UCN = ComplianceIdentifierType("bg_ucn")
        val HR_OIB = ComplianceIdentifierType("hr_oib")
        val CY_TIC = ComplianceIdentifierType("cy_tic")
        val CZ_RC = ComplianceIdentifierType("cz_rc")
        val DK_CPR = ComplianceIdentifierType("dk_cpr")
        val EE_IK = ComplianceIdentifierType("ee_ik")
        val FI_HETU = ComplianceIdentifierType("fi_hetu")
        val FR_SPI = ComplianceIdentifierType("fr_spi")
        val DE_STN = ComplianceIdentifierType("de_stn")
        val GR_AFM = ComplianceIdentifierType("gr_afm")
        val HU_AD = ComplianceIdentifierType("hu_ad")
        val IS_KT = ComplianceIdentifierType("is_kt")
        val IE_PPSN = ComplianceIdentifierType("ie_ppsn")
        val IT_CF = ComplianceIdentifierType("it_cf")
        val LV_PK = ComplianceIdentifierType("lv_pk")
        val LT_AK = ComplianceIdentifierType("lt_ak")
        val LU_NIF = ComplianceIdentifierType("lu_nif")
        val MT_NIC = ComplianceIdentifierType("mt_nic")
        val MT_PP = ComplianceIdentifierType("mt_pp")
        val NL_BSN = ComplianceIdentifierType("nl_bsn")
        val PL_PESEL = ComplianceIdentifierType("pl_pesel")
        val PL_NIP = ComplianceIdentifierType("pl_nip")
        val PT_NIF = ComplianceIdentifierType("pt_nif")
        val RO_CNP = ComplianceIdentifierType("ro_cnp")
        val SK_RC = ComplianceIdentifierType("sk_rc")
        val SI_PIN = ComplianceIdentifierType("si_pin")
        val SE_PIN = ComplianceIdentifierType("se_pin")

        fun fromValue(value: String): ComplianceIdentifierType {
            return ComplianceIdentifierType(value)
        }
    }
}

/**
 * The regulation requiring a compliance identifier.
 */
@ExperimentalCryptoOnramp
enum class ComplianceRegulation(val value: String) {
    EuCarf("eu_carf"),
    EuMica("eu_mica");

    companion object {
        fun fromValue(value: String): ComplianceRegulation? {
            return entries.firstOrNull { it.value == value }
        }
    }
}

/**
 * A compliance identifier the customer still needs to provide.
 */
@ExperimentalCryptoOnramp
@Poko
class ComplianceIdentifierRequirement internal constructor(
    val type: ComplianceIdentifierType,
    val regulation: ComplianceRegulation
)

/**
 * A group describing alternative identifier types that may satisfy a requirement.
 */
@ExperimentalCryptoOnramp
@Poko
class ComplianceIdentifierAlternativeGroup internal constructor(
    val originalMissingIdentifiers: List<ComplianceIdentifierType>,
    val alternativeMissingIdentifiers: List<ComplianceIdentifierType>
)

/**
 * The compliance identifiers a customer still needs to provide.
 */
@ExperimentalCryptoOnramp
@Poko
class ComplianceIdentifierRequirements internal constructor(
    val identifiers: List<ComplianceIdentifierRequirement>,
    val alternatives: List<ComplianceIdentifierAlternativeGroup>
)

/**
 * The result of submitting compliance identifiers for MiCA and CRS/CARF compliance.
 */
@ExperimentalCryptoOnramp
@Poko
class SubmitIdentifiersResult internal constructor(
    val valid: Boolean,
    val identifiers: List<ComplianceIdentifierRequirement>,
    val alternatives: List<ComplianceIdentifierAlternativeGroup>,
    val invalidIdentifiers: List<ComplianceIdentifierType>
)

@Serializable
internal data class SubmitIdentifiersRequest(
    val credentials: CryptoCustomerRequestParams.Credentials,
    val identifiers: List<ComplianceIdentifierRequest>
)

@Serializable
internal data class ComplianceIdentifierRequest(
    val type: String,
    val value: String
)

@Serializable
internal data class ComplianceIdentifierRequirementsResponse(
    val identifiers: List<ComplianceIdentifierRequirementResponse> = emptyList(),
    val alternatives: List<ComplianceIdentifierAlternativeGroupResponse> = emptyList(),
) {
    fun toComplianceIdentifierRequirements(): ComplianceIdentifierRequirements {
        return ComplianceIdentifierRequirements(
            identifiers = identifiers.map { it.toComplianceIdentifierRequirement() },
            alternatives = alternatives.map { it.toComplianceIdentifierAlternativeGroup() }
        )
    }
}

@Serializable
internal data class SubmitIdentifiersResponse(
    val valid: Boolean = true,
    val identifiers: List<ComplianceIdentifierRequirementResponse> = emptyList(),
    val alternatives: List<ComplianceIdentifierAlternativeGroupResponse> = emptyList(),
    @SerialName("invalid_identifiers")
    val invalidIdentifiers: List<String> = emptyList(),
) {
    fun toSubmitIdentifiersResult(): SubmitIdentifiersResult {
        return SubmitIdentifiersResult(
            valid = valid,
            identifiers = identifiers.map { it.toComplianceIdentifierRequirement() },
            alternatives = alternatives.map { it.toComplianceIdentifierAlternativeGroup() },
            invalidIdentifiers = invalidIdentifiers.map(ComplianceIdentifierType::fromValue)
        )
    }
}

@Serializable
internal data class ComplianceIdentifierRequirementResponse(
    val type: String,
    val regulation: String
) {
    fun toComplianceIdentifierRequirement(): ComplianceIdentifierRequirement {
        return ComplianceIdentifierRequirement(
            type = ComplianceIdentifierType.fromValue(type),
            regulation = requireNotNull(ComplianceRegulation.fromValue(regulation)) {
                "Unrecognized compliance regulation: $regulation"
            }
        )
    }
}

@Serializable
internal data class ComplianceIdentifierAlternativeGroupResponse(
    @SerialName("original_missing_identifiers")
    val originalMissingIdentifiers: List<String>,
    @SerialName("alternative_missing_identifiers")
    val alternativeMissingIdentifiers: List<String>
) {
    fun toComplianceIdentifierAlternativeGroup(): ComplianceIdentifierAlternativeGroup {
        return ComplianceIdentifierAlternativeGroup(
            originalMissingIdentifiers = originalMissingIdentifiers.map(ComplianceIdentifierType::fromValue),
            alternativeMissingIdentifiers = alternativeMissingIdentifiers.map(ComplianceIdentifierType::fromValue)
        )
    }
}

internal fun List<ComplianceIdentifier>.toRequest(
    credentials: CryptoCustomerRequestParams.Credentials
): SubmitIdentifiersRequest {
    return SubmitIdentifiersRequest(
        credentials = credentials,
        identifiers = map { it.build().toRequest() }
    )
}

private fun ComplianceIdentifier.State.toRequest(): ComplianceIdentifierRequest {
    return ComplianceIdentifierRequest(
        type = type.value,
        value = value
    )
}
