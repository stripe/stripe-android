package com.stripe.android.paymentelement.embedded.linkinline

import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineVerificationEligibility.IneligibleReason

/**
 * Analytics for a returning Link customer's inline verification in the Embedded Payment Element.
 */
internal sealed class EmbeddedLinkInlineAuthEvent(
    name: String,
    val reason: String? = null,
) {
    val eventName: String = "link.embedded_inline_auth.$name"

    /**
     * The card is first visible and its code is about to be sent.
     */
    data object Shown : EmbeddedLinkInlineAuthEvent("shown")

    /**
     * A tap started typing the code in the card.
     */
    data object Focused : EmbeddedLinkInlineAuthEvent("focused")

    data class DialogShown(
        val dialogReason: DialogReason,
    ) : EmbeddedLinkInlineAuthEvent("dialog_shown", dialogReason.analyticsValue)

    /**
     * The customer closed the card.
     */
    data object Dismissed : EmbeddedLinkInlineAuthEvent("dismissed")

    /**
     * The card collapsed without the customer closing it.
     */
    data class FellBack(
        val fallbackReason: FallbackReason,
    ) : EmbeddedLinkInlineAuthEvent("fell_back", fallbackReason.analyticsValue)

    data object WalletSelected : EmbeddedLinkInlineAuthEvent("wallet_selected")

    data object WalletClosed : EmbeddedLinkInlineAuthEvent("wallet_closed")

    data class Ineligible(
        val ineligibleReason: IneligibleReason,
    ) : EmbeddedLinkInlineAuthEvent("ineligible", ineligibleReason.analyticsValue)

    /**
     * Why the code is entered in the Link dialog instead of the card.
     */
    enum class DialogReason(val analyticsValue: String) {
        ForcedByServer("forced_by_server"),

        /**
         * A screen reader is running. The value matches iOS, where the screen reader is VoiceOver.
         */
        ScreenReader("voice_over"),
        UnsupportedStep("unsupported_step"),
        NotInWindow("not_in_window"),
        NoScrollView("no_scroll_view"),
        DoesNotFit("does_not_fit"),
        Obscured("obscured"),
        Email("email"),
        PhoneMatch("phone_match"),
        Confirm("confirm"),
    }

    enum class FallbackReason(val analyticsValue: String) {
        AttestationFailed("attestation_failed"),
        SendFailed("send_failed"),
        Blocked("blocked"),
        WebHandoff("web_handoff"),
    }
}
