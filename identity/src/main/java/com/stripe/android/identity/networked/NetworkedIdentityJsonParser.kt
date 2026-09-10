package com.stripe.android.identity.networked

import com.stripe.android.core.model.StripeJsonUtils.optString
import org.json.JSONArray
import org.json.JSONObject

/** NI needs verification IDs and auth-session secrets, which the shared Link parser does not retain. */
internal object NetworkedIdentityJsonParser {
    fun lookup(json: JSONObject): NetworkedIdentityLookup = if (json.getBoolean("exists")) {
        NetworkedIdentityLookup.Found(
            session = consumerSession(json.getJSONObject("consumer_session")),
            publishableKey = json.publishableKey(),
            accountId = optString(json, "account_id"),
            authSessionClientSecret = optString(json, "auth_session_client_secret"),
            emailOtpRequiresAdditionalInfo = json.optionalBoolean("email_otp_requires_additional_info"),
            emailOtpVerifyPhoneDespiteSmsOtp = json.optionalBoolean("email_otp_verify_phone_despite_sms_otp"),
            experiments = json.optJSONArray("experiments")?.mapObjects { experiment ->
                NetworkedIdentityExperiment(
                    experimentName = experiment.getString("experiment_name"),
                    variant = experiment.getString("variant"),
                    responseId = optString(experiment, "response_id"),
                )
            }.orEmpty(),
        )
    } else {
        NetworkedIdentityLookup.NotFound(errorMessage = optString(json, "error_message"))
    }

    fun signUp(json: JSONObject): NetworkedIdentitySignUpResponse = NetworkedIdentitySignUpResponse(
        session = consumerSession(json.getJSONObject("consumer_session")),
        publishableKey = json.publishableKey(),
        accountId = json.requiredString("account_id"),
        authSessionClientSecret = optString(json, "auth_session_client_secret"),
    )

    fun sessionResponse(json: JSONObject): NetworkedIdentitySessionResponse = NetworkedIdentitySessionResponse(
        session = consumerSession(json.getJSONObject("consumer_session")),
        authSessionClientSecret = optString(json, "auth_session_client_secret"),
    )

    fun documents(json: JSONObject): List<NetworkedIdentityDocument> =
        json.getJSONArray("data").mapObjects { document ->
            NetworkedIdentityDocument(
                id = document.requiredString("id"),
                documentType = NetworkedIdentityDocumentType.entries.firstOrNull {
                    it.value == document.getString("document_type")
                } ?: NetworkedIdentityDocumentType.UNKNOWN,
                created = document.getLong("created"),
                country = optString(document, "country"),
                region = optString(document, "region"),
                redactedDocumentNumber = optString(document, "redacted_document_number"),
                expirationDate = document.optionalLong("expiration_date"),
                liveCaptured = document.optionalBoolean("live_captured"),
            )
        }

    fun associationToken(json: JSONObject): NetworkedIdentityAssociationToken = NetworkedIdentityAssociationToken(
        associationToken = json.requiredString("association_token"),
    )

    fun extendSession(json: JSONObject): NetworkedIdentityExtendSessionResponse =
        NetworkedIdentityExtendSessionResponse(
            consumerSessionClientSecret = optString(json, "consumer_session_client_secret"),
        )

    private fun consumerSession(json: JSONObject): NetworkedIdentityConsumerSession = NetworkedIdentityConsumerSession(
        clientSecret = json.requiredString("client_secret"),
        emailAddress = json.requiredString("email_address"),
        redactedPhoneNumber = json.requiredString("redacted_phone_number"),
        redactedFormattedPhoneNumber = json.requiredString("redacted_formatted_phone_number"),
        unredactedPhoneNumber = optString(json, "unredacted_phone_number"),
        phoneNumberCountry = optString(json, "phone_number_country"),
        verificationSessions = json.optJSONArray("verification_sessions")?.mapObjects { verification ->
            NetworkedIdentityVerificationSession(
                id = optString(verification, "id"),
                type = NetworkedIdentityVerificationType.entries.firstOrNull {
                    it.name.equals(verification.getString("type"), ignoreCase = true)
                } ?: NetworkedIdentityVerificationType.UNKNOWN,
                state = NetworkedIdentityVerificationState.entries.firstOrNull {
                    it.name.equals(verification.getString("state"), ignoreCase = true)
                } ?: NetworkedIdentityVerificationState.UNKNOWN,
                verificationToken = optString(verification, "verification_token"),
            )
        }.orEmpty(),
    )

    private fun JSONObject.optionalBoolean(key: String): Boolean? = if (isNull(key)) null else getBoolean(key)

    private fun JSONObject.optionalLong(key: String): Long? = if (isNull(key)) null else getLong(key)

    private fun JSONObject.requiredString(key: String): String = requireNotNull(get(key) as? String).also {
        require(it.isNotBlank())
    }

    private fun JSONObject.publishableKey(): String = requiredString("publishable_key").also {
        require(it.startsWith("pk_"))
    }

    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        (0 until length()).map { transform(getJSONObject(it)) }
}
