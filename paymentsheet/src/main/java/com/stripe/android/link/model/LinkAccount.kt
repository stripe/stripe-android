package com.stripe.android.link.model

import android.os.Parcelable
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.DisplayablePaymentDetails
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.MobileFallbackWebviewParams
import com.stripe.android.uicore.elements.convertPhoneNumberToE164
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * Immutable object representing a Link account.
 */
@Parcelize
internal data class LinkAccount(
    private val consumerSession: ConsumerSession,
    val consumerPublishableKey: String? = null,
    val displayablePaymentDetails: DisplayablePaymentDetails? = null,
    val linkAuthIntentInfo: LinkAuthIntentInfo? = null,
    val viewedWebviewOpenUrl: Boolean = false,
) : Parcelable {

    val supportedPaymentDetailsTypes: List<String>
        get() = consumerSession.supportedPaymentDetailsTypes

    val linkBrand: LinkBrand?
        get() = consumerSession.linkBrand?.let { consumerLinkBrand ->
            if (FeatureFlags.forceOnelinkConsumer.isEnabled) LinkBrand.Onelink else consumerLinkBrand
        }

    // Raw value from the backend, used to carry forward across session updates.
    internal val consumerLinkBrand: LinkBrand?
        get() = consumerSession.linkBrand

    @IgnoredOnParcel
    val redactedPhoneNumber = consumerSession.redactedFormattedPhoneNumber.replace("*", "•")

    val unredactedPhoneNumber: String?
        get() {
            val nationalPhoneNumber = consumerSession.unredactedPhoneNumber
            val countryCode = consumerSession.phoneNumberCountry

            return if (nationalPhoneNumber != null && countryCode != null) {
                convertPhoneNumberToE164(nationalPhoneNumber, countryCode)
            } else {
                null
            }
        }

    /**
     * The last two digits of the account's phone number, used as a hint when confirming it.
     */
    val phoneNumberLastTwoDigits: String?
        get() = consumerSession.redactedPhoneNumber.takeLast(2).takeIf { digits ->
            digits.length == 2 && digits.all { it.isDigit() }
        }

    val phoneNumberCountry: String?
        get() = consumerSession.phoneNumberCountry

    val availableVerificationFactors: List<ConsumerSession.VerificationFactor>?
        get() = consumerSession.availableVerificationFactors

    val emailOtpRequiresAdditionalInfo: Boolean?
        get() = consumerSession.emailOtpRequiresAdditionalInfo

    val verificationSessions: List<ConsumerSession.VerificationSession>
        get() = consumerSession.verificationSessions

    val meetsMinimumAuthenticationLevel: Boolean
        get() = consumerSession.meetsMinimumAuthenticationLevel

    val webviewRequired: Boolean
        get() = consumerSession.mobileFallbackWebviewParams?.webViewRequirementType ==
            MobileFallbackWebviewParams.WebviewRequirementType.Required

    @IgnoredOnParcel
    val clientSecret = consumerSession.clientSecret

    @IgnoredOnParcel
    val linkSessionKey = consumerSession.linkSessionKey

    @IgnoredOnParcel
    val email = consumerSession.emailAddress

    @IgnoredOnParcel
    val isVerified: Boolean = consumerSession.meetsMinimumAuthenticationLevel ||
        consumerSession.isVerifiedForSignup() ||
        consumerSession.isVerifiedWithLinkAuthToken()

    @IgnoredOnParcel
    val completedSignup: Boolean = consumerSession.isVerifiedForSignup()

    val consentPresentation: ConsentPresentation?
        get() = linkAuthIntentInfo?.consentPresentation

    @IgnoredOnParcel
    val accountStatus = when {
        isVerified -> {
            AccountStatus.Verified(
                consentPresentation = consentPresentation,
                meetsMinimumAuthenticationLevel = consumerSession.meetsMinimumAuthenticationLevel,
            )
        }
        consumerSession.containsOtpSessionStarted() -> {
            AccountStatus.VerificationStarted
        }
        else -> {
            val params = consumerSession.mobileFallbackWebviewParams
            AccountStatus.NeedsVerification(
                webviewOpenUrl = params
                    ?.webviewOpenUrl
                    ?.takeIf {
                        params.webViewRequirementType == MobileFallbackWebviewParams.WebviewRequirementType.Required
                    }
            )
        }
    }

    @IgnoredOnParcel
    val webviewOpenUrl: String? = consumerSession.mobileFallbackWebviewParams?.webviewOpenUrl

    private fun ConsumerSession.containsOtpSessionStarted() = verificationSessions.find {
        val isOtp = it.type == ConsumerSession.VerificationSession.SessionType.Sms ||
            it.type == ConsumerSession.VerificationSession.SessionType.Email
        isOtp && it.state == ConsumerSession.VerificationSession.SessionState.Started
    } != null

    private fun ConsumerSession.isVerifiedForSignup() = verificationSessions.find {
        it.type == ConsumerSession.VerificationSession.SessionType.SignUp &&
            it.state == ConsumerSession.VerificationSession.SessionState.Started
    } != null

    private fun ConsumerSession.isVerifiedWithLinkAuthToken() = verificationSessions.find {
        it.type == ConsumerSession.VerificationSession.SessionType.LinkAuthToken &&
            it.state == ConsumerSession.VerificationSession.SessionState.Verified
    } != null
}
