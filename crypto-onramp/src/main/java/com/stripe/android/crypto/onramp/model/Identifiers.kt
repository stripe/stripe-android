package com.stripe.android.crypto.onramp.model

import com.stripe.android.core.model.CountryCode
import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Identifier payload submitted to the Crypto Onramp API.
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
 * A country-scoped identifier entry.
 *
 * @property country ISO 3166-1 alpha-2 country code associated with the identifier.
 * @property identifier The identifier value.
 * @property identifierType Optional subtype for classifying the identifier.
 */
@ExperimentalCryptoOnramp
class Identifier {
    private var country: CountryCode? = null
    private var identifier: String? = null
    private var identifierType: String? = null

    /**
     * Sets the ISO 3166-1 alpha-2 country code associated with the identifier.
     */
    fun country(country: CountryCode) = apply {
        this.country = country
    }

    /**
     * Sets the identifier value.
     */
    fun identifier(identifier: String) = apply {
        this.identifier = identifier
    }

    /**
     * Sets an optional subtype for the identifier.
     */
    fun identifierType(identifierType: String) = apply {
        this.identifierType = identifierType
    }

    internal class State(
        val country: CountryCode,
        val identifier: String,
        val identifierType: String?
    )

    internal fun build(): State {
        return State(
            country = requireNotNull(country) {
                "country must not be null"
            },
            identifier = requireNotNull(identifier) {
                "identifier must not be null"
            },
            identifierType = identifierType
        )
    }
}

/**
 * Identifier requirements returned by the Crypto Onramp API.
 *
 * @property missingIdentifiersMica Missing MiCA identifier field names.
 * @property missingIdentifiersCarf Missing CARF identifier field names.
 */
@ExperimentalCryptoOnramp
@Poko
class IdentifierRequirements internal constructor(
    val missingIdentifiersMica: List<String>,
    val missingIdentifiersCarf: List<String>
)

/**
 * Missing identifiers returned by the KYC info update API.
 *
 * @property missingIdentifiersMica Missing MiCA identifier field names.
 * @property missingIdentifiersCarf Missing CARF identifier field names.
 */
@ExperimentalCryptoOnramp
@Poko
class MissingIdentifiers internal constructor(
    val missingIdentifiersMica: List<String>,
    val missingIdentifiersCarf: List<String>
)

/**
 * Validation result returned after updating KYC info.
 *
 * @property valid Whether the submitted identifiers were accepted.
 * @property missingIdentifiers Missing identifiers, if more are required.
 * @property errors Validation errors returned by the API.
 */
@ExperimentalCryptoOnramp
@Poko
class UpdateKycInfoResult internal constructor(
    val valid: Boolean,
    val missingIdentifiers: MissingIdentifiers?,
    val errors: List<String>?
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
    val identifierType: String? = null
)

@Serializable
internal data class IdentifierRequirementsResponse(
    @SerialName("missing_identifiers_mica")
    val missingIdentifiersMica: List<String> = emptyList(),
    @SerialName("missing_identifiers_carf")
    val missingIdentifiersCarf: List<String> = emptyList(),
) {
    fun toIdentifierRequirements(): IdentifierRequirements {
        return IdentifierRequirements(
            missingIdentifiersMica = missingIdentifiersMica,
            missingIdentifiersCarf = missingIdentifiersCarf
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
            errors = errors
        )
    }
}

@Serializable
internal data class MissingIdentifiersResponse(
    @SerialName("missing_identifiers_mica")
    val missingIdentifiersMica: List<String> = emptyList(),
    @SerialName("missing_identifiers_carf")
    val missingIdentifiersCarf: List<String> = emptyList(),
) {
    fun toMissingIdentifiers(): MissingIdentifiers {
        return MissingIdentifiers(
            missingIdentifiersMica = missingIdentifiersMica,
            missingIdentifiersCarf = missingIdentifiersCarf
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
        country = country.value,
        identifier = identifier,
        identifierType = identifierType
    )
}
