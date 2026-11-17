package com.stripe.android.paymentelement.embedded.linkinline

import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.verification.LinkAuthFlow
import com.stripe.android.link.ui.verification.LinkAuthFlowState
import com.stripe.android.model.VerificationType

/**
 * Decides whether a returning Link customer verifies in a card shown inline in the Embedded Payment Element.
 */
internal object EmbeddedLinkInlineVerificationEligibility {
    /**
     * Why the card isn't shown, in the order the conditions are checked.
     */
    enum class IneligibleReason(val analyticsValue: String) {
        DisabledByServer("disabled_by_server"),
        DisabledByMerchant("disabled_by_merchant"),
        LinkNotDisplayed("link_not_displayed"),
        NotReturningLinkUser("not_returning_link_user"),
        AlreadyVerified("already_verified"),
        NativeLinkUnavailable("native_link_unavailable"),
        WebViewRequired("web_view_required"),
        VerificationAlreadyStarted("verification_already_started"),
        HasSavedPaymentMethods("has_saved_payment_methods"),
        DismissedThisSession("dismissed_this_session"),
        ShownElsewhere("shown_elsewhere"),
        CodeRecentlySent("code_recently_sent"),
        UnsupportedVerification("unsupported_verification");

        /**
         * Whether the reason is logged. Only returning customers who need to verify while the feature is on say
         * anything about the card.
         */
        val isReported: Boolean
            get() = when (this) {
                DisabledByServer,
                LinkNotDisplayed,
                NotReturningLinkUser,
                AlreadyVerified -> false
                DisabledByMerchant,
                NativeLinkUnavailable,
                WebViewRequired,
                VerificationAlreadyStarted,
                HasSavedPaymentMethods,
                DismissedThisSession,
                ShownElsewhere,
                CodeRecentlySent,
                UnsupportedVerification -> true
            }
    }

    data class Inputs(
        val isEnabledByServer: Boolean,
        val isAllowedByMerchant: Boolean,
        val isLinkDisplayed: Boolean,
        val linkAccount: LinkAccount?,
        val canUseNativeLink: Boolean,
        val hasSavedPaymentMethods: Boolean,
    )

    sealed interface Result {
        /**
         * @param authFlow An attempt prepared for [linkAccount] that hasn't sent anything yet.
         */
        data class Eligible(
            val linkAccount: LinkAccount,
            val authFlow: LinkAuthFlow,
        ) : Result

        data class Ineligible(val reason: IneligibleReason) : Result
    }

    /**
     * @param registry The process-wide memory of cards shown, dismissed and codes sent.
     * @param owner The Embedded Payment Element asking, which may already hold the card.
     * @param prepareAuthFlow Creates an attempt for the current account without making any request.
     */
    fun evaluate(
        inputs: Inputs,
        registry: EmbeddedLinkInlineAuthRegistry,
        owner: Any,
        prepareAuthFlow: () -> LinkAuthFlow,
    ): Result {
        val reason = ineligibleReason(inputs, registry, owner)
        if (reason != null) {
            return Result.Ineligible(reason)
        }
        val linkAccount = requireNotNull(inputs.linkAccount)
        val authFlow = prepareAuthFlow()
        if (!authFlow.state.value.isFirstSmsCode()) {
            authFlow.invalidate()
            return Result.Ineligible(IneligibleReason.UnsupportedVerification)
        }
        return Result.Eligible(linkAccount = linkAccount, authFlow = authFlow)
    }

    private fun ineligibleReason(
        inputs: Inputs,
        registry: EmbeddedLinkInlineAuthRegistry,
        owner: Any,
    ): IneligibleReason? {
        val linkAccount = inputs.linkAccount
        return when {
            !inputs.isEnabledByServer -> IneligibleReason.DisabledByServer
            !inputs.isAllowedByMerchant -> IneligibleReason.DisabledByMerchant
            !inputs.isLinkDisplayed -> IneligibleReason.LinkNotDisplayed
            linkAccount == null -> IneligibleReason.NotReturningLinkUser
            linkAccount.accountStatus is AccountStatus.Verified -> IneligibleReason.AlreadyVerified
            !inputs.canUseNativeLink -> IneligibleReason.NativeLinkUnavailable
            linkAccount.webviewRequired -> IneligibleReason.WebViewRequired
            linkAccount.accountStatus is AccountStatus.VerificationStarted ->
                IneligibleReason.VerificationAlreadyStarted
            inputs.hasSavedPaymentMethods -> IneligibleReason.HasSavedPaymentMethods
            else -> registry.ineligibleReason(email = linkAccount.email, owner = owner)
        }
    }

    private fun LinkAuthFlowState.isFirstSmsCode(): Boolean {
        return screen == LinkAuthFlowState.Screen.Otp && verificationType == VerificationType.SMS && step == 1
    }
}
