package com.stripe.android.model.parsers

import com.stripe.android.model.ConsumerSession
import org.json.JSONObject

internal class ConsumerSessionJsonParser : ModelJsonParser<ConsumerSession> {
    override fun parse(json: JSONObject): ConsumerSession? {
        val consumerSessionJson = json.optJSONObject(FIELD_CONSUMER_SESSION) ?: return null

        val verificationSession =
            consumerSessionJson.optJSONArray(FIELD_CONSUMER_SESSION_VERIFICATION_SESSIONS)
                ?.let { verificationSessionsArray ->
                    (0 until verificationSessionsArray.length())
                        .map { index -> verificationSessionsArray.getJSONObject(index) }
                        .mapNotNull { parseVerificationSession(it) }
                } ?: emptyList()

        return ConsumerSession(
            consumerSessionJson.getString(FIELD_CONSUMER_SESSION_SECRET),
            consumerSessionJson.getString(FIELD_CONSUMER_SESSION_EMAIL),
            consumerSessionJson.getString(FIELD_CONSUMER_SESSION_PHONE),
            verificationSession
        )
    }

    private fun parseVerificationSession(json: JSONObject): ConsumerSession.VerificationSession =
        ConsumerSession.VerificationSession(
            ConsumerSession.VerificationSession.SessionType.fromValue(
                json.getString(FIELD_VERIFICATION_SESSION_TYPE).lowercase()
            ),
            ConsumerSession.VerificationSession.SessionState.fromValue(
                json.getString(FIELD_VERIFICATION_SESSION_STATE).lowercase()
            )
        )

    private companion object {
        private const val FIELD_CONSUMER_SESSION = "consumer_session"

        private const val FIELD_CONSUMER_SESSION_SECRET = "client_secret"
        private const val FIELD_CONSUMER_SESSION_EMAIL = "email_address"
        private const val FIELD_CONSUMER_SESSION_PHONE = "redacted_phone_number"
        private const val FIELD_CONSUMER_SESSION_VERIFICATION_SESSIONS = "verification_sessions"

        private const val FIELD_VERIFICATION_SESSION_TYPE = "type"
        private const val FIELD_VERIFICATION_SESSION_STATE = "state"
    }
}
