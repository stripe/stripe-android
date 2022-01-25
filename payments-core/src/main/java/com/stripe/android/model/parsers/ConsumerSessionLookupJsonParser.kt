package com.stripe.android.model.parsers

import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionLookup.VerificationSession
import org.json.JSONObject

internal class ConsumerSessionLookupJsonParser : ModelJsonParser<ConsumerSessionLookup> {
    override fun parse(json: JSONObject): ConsumerSessionLookup? {
        val exists = json.getBoolean(FIELD_EXISTS)
        val consumerSession = parseConsumerSession(json.optJSONObject(FIELD_CONSUMER_SESSION))
        val errorMessage = json.optString(FIELD_ERROR_MESSAGE).takeIf {
            it.isNotBlank() && !it.equals("null")
        }

        return ConsumerSessionLookup(exists, consumerSession, errorMessage)
    }

    private fun parseConsumerSession(json: JSONObject?): ConsumerSessionLookup.ConsumerSession? =
        json?.let {
            val verificationSession =
                json.optJSONArray(FIELD_CONSUMER_SESSION_VERIFICATION_SESSIONS)
                    .let { verificationSessionsArray ->
                        (0 until verificationSessionsArray.length())
                            .map { index -> verificationSessionsArray.getJSONObject(index) }
                            .mapNotNull { parseVerificationSession(it) }
                    } ?: emptyList()

            ConsumerSessionLookup.ConsumerSession(
                json.getString(FIELD_CONSUMER_SESSION_SECRET),
                json.getString(FIELD_CONSUMER_SESSION_EMAIL),
                json.getString(FIELD_CONSUMER_SESSION_PHONE),
                verificationSession
            )
        }

    private fun parseVerificationSession(json: JSONObject): VerificationSession =
        VerificationSession(
            VerificationSession.SessionType.valueOf(
                json.getString(FIELD_VERIFICATION_SESSION_TYPE)
            ),
            VerificationSession.SessionState.valueOf(
                json.getString(FIELD_VERIFICATION_SESSION_STATE)
            )
        )

    private companion object {
        private const val FIELD_EXISTS = "exists"
        private const val FIELD_ERROR_MESSAGE = "error_message"
        private const val FIELD_CONSUMER_SESSION = "consumer_session"

        private const val FIELD_CONSUMER_SESSION_SECRET = "client_secret"
        private const val FIELD_CONSUMER_SESSION_EMAIL = "email_address"
        private const val FIELD_CONSUMER_SESSION_PHONE = "redacted_phone_number"
        private const val FIELD_CONSUMER_SESSION_VERIFICATION_SESSIONS = "verification_sessions"

        private const val FIELD_VERIFICATION_SESSION_TYPE = "type"
        private const val FIELD_VERIFICATION_SESSION_STATE = "state"
    }
}
