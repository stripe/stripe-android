package com.stripe.android.crypto.onramp.model

import com.stripe.android.core.model.CountryCode
import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/**
 * EU identifier payload submitted to the Crypto Onramp API.
 *
 * @property identifiersMica MiCA identifiers for the consumer.
 * @property identifiersCarf CARF identifiers for the consumer.
 */
@ExperimentalCryptoOnramp
class EuIdentifiers {
    private var identifiersMica: List<EuIdentifier>? = null
    private var identifiersCarf: List<EuIdentifier>? = null

    /**
     * Sets MiCA identifiers for the consumer.
     */
    fun identifiersMica(identifiersMica: List<EuIdentifier>?) = apply {
        this.identifiersMica = identifiersMica
    }

    /**
     * Sets CARF identifiers for the consumer.
     */
    fun identifiersCarf(identifiersCarf: List<EuIdentifier>?) = apply {
        this.identifiersCarf = identifiersCarf
    }

    internal class State(
        val identifiersMica: List<EuIdentifier.State>?,
        val identifiersCarf: List<EuIdentifier.State>?
    )

    internal fun build(): State {
        return State(
            identifiersMica = identifiersMica?.map { it.build() },
            identifiersCarf = identifiersCarf?.map { it.build() }
        )
    }
}

/**
 * A country-scoped EU identifier.
 *
 * @property country ISO 3166-1 alpha-2 country code associated with the identifier.
 * @property identifier The identifier value.
 */
@ExperimentalCryptoOnramp
class EuIdentifier {
    private var country: CountryCode? = null
    private var identifier: String? = null

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

    internal class State(
        val country: CountryCode,
        val identifier: String
    )

    internal fun build(): State {
        return State(
            country = requireNotNull(country) {
                "country must not be null"
            },
            identifier = requireNotNull(identifier) {
                "identifier must not be null"
            }
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
 * Missing EU identifiers returned by the submit EU identifiers API.
 *
 * @property missingIdentifiersMica Missing MiCA identifier field names.
 * @property missingIdentifiersCarf Missing CARF identifier field names.
 */
@ExperimentalCryptoOnramp
@Poko
class MissingEuIdentifiers internal constructor(
    val missingIdentifiersMica: List<String>,
    val missingIdentifiersCarf: List<String>
)

/**
 * Validation result returned after submitting EU identifiers.
 *
 * @property valid Whether the submitted identifiers were accepted.
 * @property missingIdentifiers Missing identifiers, if more are required.
 * @property errors Invalid country codes for identifiers that were rejected.
 */
@ExperimentalCryptoOnramp
@Poko
class SubmitEuIdentifiersResult internal constructor(
    val valid: Boolean,
    val missingIdentifiers: MissingEuIdentifiers?,
    val errors: List<String>?
)

@Serializable
internal data class EuIdentifiersRequest(
    @SerialName("credentials")
    val credentials: CryptoCustomerRequestParams.Credentials,
    @SerialName("identifiers_mica")
    val identifiersMica: List<EuIdentifierRequest>? = null,
    @SerialName("identifiers_carf")
    val identifiersCarf: List<EuIdentifierRequest>? = null
)

@Serializable
internal data class EuIdentifierRequest(
    @SerialName("country")
    val country: String,
    @SerialName("identifier")
    val identifier: String
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
internal data class SubmitEuIdentifiersResponse(
    @SerialName("valid")
    val valid: Boolean = false,
    @SerialName("missing_identifiers")
    val missingIdentifiers: JsonObject? = null,
    @SerialName("errors")
    val errors: List<String>? = null,
) {
    fun toSubmitEuIdentifiersResult(): SubmitEuIdentifiersResult {
        return SubmitEuIdentifiersResult(
            valid = valid,
            missingIdentifiers = missingIdentifiers?.toMissingEuIdentifiers(),
            errors = errors
        )
    }
}

internal fun EuIdentifiers.toRequest(
    credentials: CryptoCustomerRequestParams.Credentials
): EuIdentifiersRequest {
    val state = build()

    return EuIdentifiersRequest(
        credentials = credentials,
        identifiersMica = state.identifiersMica?.map { it.toRequest() },
        identifiersCarf = state.identifiersCarf?.map { it.toRequest() }
    )
}

private fun EuIdentifier.State.toRequest(): EuIdentifierRequest {
    return EuIdentifierRequest(
        country = country.value,
        identifier = identifier
    )
}

private fun JsonObject.toMissingEuIdentifiers(): MissingEuIdentifiers {
    val missingIdentifiersMica = readStringList(
        "missing_identifiers_mica",
        "identifiers_mica",
        "mica",
    )
    val missingIdentifiersCarf = readStringList(
        "missing_identifiers_carf",
        "identifiers_carf",
        "carf",
    )
    val fallbackIdentifiers = if (missingIdentifiersMica == null && missingIdentifiersCarf == null) {
        entries.mapNotNull { (key, value) ->
            ((value as? JsonPrimitive)?.booleanOrNull == true).takeIf { it }?.let { key }
        }
    } else {
        emptyList()
    }

    return MissingEuIdentifiers(
        missingIdentifiersMica = missingIdentifiersMica ?: emptyList(),
        missingIdentifiersCarf = missingIdentifiersCarf ?: fallbackIdentifiers
    )
}

private fun JsonObject.readStringList(vararg keys: String): List<String>? {
    return keys.firstNotNullOfOrNull { key ->
        (this[key] as? JsonArray)?.mapNotNull { element ->
            (element as? JsonPrimitive)?.contentOrNull
        }
    }
}
