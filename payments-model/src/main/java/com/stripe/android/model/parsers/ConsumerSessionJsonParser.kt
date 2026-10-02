package com.stripe.android.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.MobileFallbackWebviewParams
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ConsumerSessionJsonParser : ModelJsonParser<ConsumerSession> {
    override fun parse(json: JSONObject): ConsumerSession? {
        val consumerSessionJson = json.optJSONObject(FIELD_CONSUMER_SESSION) ?: return null

        val verificationSession =
            consumerSessionJson.optJSONArray(FIELD_CONSUMER_SESSION_VERIFICATION_SESSIONS)
                ?.let { verificationSessionsArray ->
                    (0 until verificationSessionsArray.length())
                        .map { index -> verificationSessionsArray.getJSONObject(index) }
                        .mapNotNull { parseVerificationSession(it) }
                } ?: emptyList()

        val mobileFallbackWebviewParams =
            consumerSessionJson.optJSONObject(FIELD_MOBILE_FALLBACK_WEBVIEW_PARAMS)
                ?.let { parseMobileFallbackWebviewParams(it) }

        val currentAuthenticationLevel =
            optString(consumerSessionJson, FIELD_CURRENT_AUTHENTICATION_LEVEL)
                ?.let { ConsumerSession.AuthenticationLevel.fromValue(it) }

        val minimumAuthenticationLevel =
            optString(consumerSessionJson, FIELD_MINIMUM_AUTHENTICATION_LEVEL)
                ?.let { ConsumerSession.AuthenticationLevel.fromValue(it) }

        val linkBrand = optString(json, FIELD_LINK_BRAND)?.let { value ->
            LinkBrand.entries.firstOrNull { it.value == value }
        }

        val supportedPaymentDetailsTypes =
            consumerSessionJson.optJSONArray(FIELD_SUPPORT_PAYMENT_DETAILS_TYPES)
                ?.let { array -> (0 until array.length()).map { array.getString(it) } }
                ?: emptyList()

        val availableVerificationFactors =
            consumerSessionJson.optJSONArray(FIELD_AVAILABLE_VERIFICATION_FACTORS)
                ?.let { array ->
                    (0 until array.length())
                        .mapNotNull { index -> array.optJSONObject(index) }
                        .map { parseVerificationFactor(it) }
                }

        val emailOtpRequiresAdditionalInfo = json.optJSONObject(FIELD_SETTINGS)
            ?.takeIf { it.has(FIELD_EMAIL_OTP_REQUIRES_ADDITIONAL_INFO) }
            ?.optBoolean(FIELD_EMAIL_OTP_REQUIRES_ADDITIONAL_INFO)

        return ConsumerSession(
            clientSecret = consumerSessionJson.getString(FIELD_CONSUMER_SESSION_SECRET),
            linkSessionKey = optString(consumerSessionJson, FIELD_LINK_SESSION_KEY),
            emailAddress = consumerSessionJson.getString(FIELD_CONSUMER_SESSION_EMAIL),
            redactedFormattedPhoneNumber = consumerSessionJson.getString(FIELD_CONSUMER_SESSION_FORMATTED_PHONE),
            redactedPhoneNumber = consumerSessionJson.getString(FIELD_CONSUMER_SESSION_PHONE),
            unredactedPhoneNumber = optString(consumerSessionJson, FIELD_CONSUMER_SESSION_UNREDACTED_PHONE),
            phoneNumberCountry = optString(consumerSessionJson, FIELD_CONSUMER_SESSION_PHONE_COUNTRY),
            verificationSessions = verificationSession,
            mobileFallbackWebviewParams = mobileFallbackWebviewParams,
            currentAuthenticationLevel = currentAuthenticationLevel,
            minimumAuthenticationLevel = minimumAuthenticationLevel,
            linkBrand = linkBrand,
            supportedPaymentDetailsTypes = supportedPaymentDetailsTypes,
            availableVerificationFactors = availableVerificationFactors,
            emailOtpRequiresAdditionalInfo = emailOtpRequiresAdditionalInfo,
        )
    }

    private fun parseVerificationFactor(json: JSONObject): ConsumerSession.VerificationFactor =
        ConsumerSession.VerificationFactor(
            type = ConsumerSession.VerificationFactor.FactorType.fromValue(
                optString(json, FIELD_VERIFICATION_FACTOR_TYPE).orEmpty()
            ),
            id = optString(json, FIELD_VERIFICATION_FACTOR_ID),
            providesFurtherVerification = json.optBoolean(FIELD_PROVIDES_FURTHER_VERIFICATION, false),
            temporarilyDisabled = json.optBoolean(FIELD_TEMPORARILY_DISABLED, false),
        )

    private fun parseVerificationSession(json: JSONObject): ConsumerSession.VerificationSession =
        ConsumerSession.VerificationSession(
            ConsumerSession.VerificationSession.SessionType.fromValue(
                json.getString(FIELD_VERIFICATION_SESSION_TYPE).lowercase()
            ),
            ConsumerSession.VerificationSession.SessionState.fromValue(
                json.getString(FIELD_VERIFICATION_SESSION_STATE).lowercase()
            )
        )

    private fun parseMobileFallbackWebviewParams(json: JSONObject): MobileFallbackWebviewParams =
        MobileFallbackWebviewParams(
            webViewRequirementType = MobileFallbackWebviewParams.WebviewRequirementType.fromValue(
                json.getString(FIELD_WEBVIEW_REQUIREMENT_TYPE)
            ),
            webviewOpenUrl = optString(json, FIELD_WEBVIEW_OPEN_URL)
        )

    private companion object {
        private const val FIELD_CONSUMER_SESSION = "consumer_session"

        private const val FIELD_CONSUMER_SESSION_SECRET = "client_secret"
        private const val FIELD_LINK_SESSION_KEY = "link_session_key"
        private const val FIELD_CONSUMER_SESSION_EMAIL = "email_address"
        private const val FIELD_CONSUMER_SESSION_PHONE = "redacted_phone_number"
        private const val FIELD_CONSUMER_SESSION_FORMATTED_PHONE = "redacted_formatted_phone_number"
        private const val FIELD_CONSUMER_SESSION_VERIFICATION_SESSIONS = "verification_sessions"
        private const val FIELD_CONSUMER_SESSION_UNREDACTED_PHONE = "unredacted_phone_number"
        private const val FIELD_CONSUMER_SESSION_PHONE_COUNTRY = "phone_number_country"
        private const val FIELD_MOBILE_FALLBACK_WEBVIEW_PARAMS = "mobile_fallback_webview_params"
        private const val FIELD_CURRENT_AUTHENTICATION_LEVEL = "current_authentication_level"
        private const val FIELD_MINIMUM_AUTHENTICATION_LEVEL = "minimum_authentication_level"

        private const val FIELD_AVAILABLE_VERIFICATION_FACTORS = "available_verification_factors"
        private const val FIELD_VERIFICATION_FACTOR_TYPE = "type"
        private const val FIELD_VERIFICATION_FACTOR_ID = "id"
        private const val FIELD_PROVIDES_FURTHER_VERIFICATION = "provides_further_verification"
        private const val FIELD_TEMPORARILY_DISABLED = "temporarily_disabled"

        private const val FIELD_SETTINGS = "settings"
        private const val FIELD_EMAIL_OTP_REQUIRES_ADDITIONAL_INFO = "email_otp_requires_additional_info"

        private const val FIELD_VERIFICATION_SESSION_TYPE = "type"
        private const val FIELD_VERIFICATION_SESSION_STATE = "state"

        private const val FIELD_LINK_BRAND = "link_brand"
        private const val FIELD_SUPPORT_PAYMENT_DETAILS_TYPES = "support_payment_details_types"
        private const val FIELD_WEBVIEW_REQUIREMENT_TYPE = "webview_requirement_type"
        private const val FIELD_WEBVIEW_OPEN_URL = "webview_open_url"
    }
}
