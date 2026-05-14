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

    val linkBrand: LinkBrand?
        get() = consumerSession.linkBrand?.let { consumerLinkBrand ->
            if (FeatureFlags.forceNotlinkConsumer.isEnabled) LinkBrand.Notlink else consumerLinkBrand
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

    @IgnoredOnParcel
    val clientSecret = consumerSession.clientSecret

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
        consumerSession.containsSMSSessionStarted() -> {
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

    private fun ConsumerSession.containsSMSSessionStarted() = verificationSessions.find {
        it.type == ConsumerSession.VerificationSession.SessionType.Sms &&
            it.state == ConsumerSession.VerificationSession.SessionState.Started
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
