package com.stripe.android.paymentelement.embedded.linkinline

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.verification.LinkAuthFlowState
import com.stripe.android.model.VerificationType
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineCardState.Action
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineCardState.Actions
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineCardState.MenuItem
import com.stripe.android.paymentsheet.R
import org.junit.Test

internal class EmbeddedLinkInlineCardStateTest {

    @Test
    fun `card shows the email, recipient and code state`() {
        val state = EmbeddedLinkInlineCardState.create(
            state = authFlowState(
                errorMessage = "Wrong code".resolvableString,
                inputRevision = 3,
                invalidCodeRevision = 2,
            ),
            email = EMAIL,
        )

        assertThat(state.email).isEqualTo(EMAIL)
        assertThat(state.recipient).isEqualTo(RECIPIENT)
        assertThat(state.isCodeEntryEnabled).isTrue()
        assertThat(state.errorMessage).isEqualTo("Wrong code".resolvableString)
        assertThat(state.inputRevision).isEqualTo(3)
        assertThat(state.invalidCodeRevision).isEqualTo(2)
    }

    @Test
    fun `resend is the only action without email verification`() {
        val state = EmbeddedLinkInlineCardState.create(state = authFlowState(), email = EMAIL)

        assertThat(state.actions).isEqualTo(
            Actions.Resend(label = resolvableString(R.string.stripe_verification_resend), isEnabled = true)
        )
        assertThat(state.isActionLoading).isFalse()
    }

    @Test
    fun `resend counts down during its cooldown`() {
        val state = EmbeddedLinkInlineCardState.create(
            state = authFlowState(canResend = false, resendSecondsRemaining = 7),
            email = EMAIL,
        )

        assertThat(state.actions).isEqualTo(
            Actions.Resend(
                label = resolvableString(R.string.stripe_link_auth_resend_countdown, 7),
                isEnabled = false,
            )
        )
    }

    @Test
    fun `resend and email code are behind more options with email verification`() {
        val state = EmbeddedLinkInlineCardState.create(
            state = authFlowState(canSendToEmail = true),
            email = EMAIL,
        )

        assertThat(state.actions).isEqualTo(
            Actions.Menu(
                items = listOf(
                    MenuItem(
                        action = Action.Resend,
                        label = resolvableString(R.string.stripe_verification_resend),
                        isEnabled = true,
                    ),
                    MenuItem(
                        action = Action.Email,
                        label = resolvableString(R.string.stripe_link_auth_email_code),
                        isEnabled = true,
                    ),
                ),
                isEnabled = true,
            )
        )
    }

    @Test
    fun `menu shows the resend countdown as a disabled item`() {
        val state = EmbeddedLinkInlineCardState.create(
            state = authFlowState(canSendToEmail = true, canResend = false, resendSecondsRemaining = 4),
            email = EMAIL,
        )

        val menu = state.actions as Actions.Menu
        assertThat(menu.items.first()).isEqualTo(
            MenuItem(
                action = Action.Resend,
                label = resolvableString(R.string.stripe_link_auth_resend_countdown, 4),
                isEnabled = false,
            )
        )
    }

    @Test
    fun `first send shows the action as loading and disables code entry`() {
        val state = EmbeddedLinkInlineCardState.create(
            state = authFlowState(
                isLoading = true,
                isCodeSent = false,
                canSubmitCode = false,
                canResend = false,
                canSendToEmail = true,
            ),
            email = EMAIL,
        )

        assertThat(state.isActionLoading).isTrue()
        assertThat(state.isCodeEntryEnabled).isFalse()
        assertThat((state.actions as Actions.Menu).isEnabled).isFalse()
    }

    @Test
    fun `resend shows the action as loading`() {
        val state = EmbeddedLinkInlineCardState.create(
            state = authFlowState(isLoading = true, isResending = true, canResend = false),
            email = EMAIL,
        )

        assertThat(state.isActionLoading).isTrue()
    }

    @Test
    fun `checking a code doesn't show the action as loading`() {
        val state = EmbeddedLinkInlineCardState.create(
            state = authFlowState(isLoading = true, canSubmitCode = false, canResend = false),
            email = EMAIL,
        )

        assertThat(state.isActionLoading).isFalse()
        assertThat(state.actions).isEqualTo(
            Actions.Resend(label = resolvableString(R.string.stripe_verification_resend), isEnabled = false)
        )
    }

    @Test
    fun `no actions outside the code screen`() {
        val state = EmbeddedLinkInlineCardState.create(
            state = authFlowState(screen = LinkAuthFlowState.Screen.Blocked),
            email = EMAIL,
        )

        assertThat(state.actions).isEqualTo(Actions.None)
    }

    @Suppress("LongParameterList")
    private fun authFlowState(
        screen: LinkAuthFlowState.Screen = LinkAuthFlowState.Screen.Otp,
        isLoading: Boolean = false,
        isResending: Boolean = false,
        isCodeSent: Boolean = true,
        errorMessage: com.stripe.android.core.strings.ResolvableString? = null,
        inputRevision: Int = 1,
        invalidCodeRevision: Int = 0,
        canSubmitCode: Boolean = true,
        canResend: Boolean = true,
        resendSecondsRemaining: Int = 0,
        canSendToEmail: Boolean = false,
    ) = LinkAuthFlowState(
        screen = screen,
        verificationType = VerificationType.SMS,
        step = 1,
        isLoading = isLoading,
        isResending = isResending,
        errorMessage = errorMessage,
        inputRevision = inputRevision,
        invalidCodeRevision = invalidCodeRevision,
        isCodeSent = isCodeSent,
        canGoBack = false,
        canSubmitCode = canSubmitCode,
        canResend = canResend,
        resendSecondsRemaining = resendSecondsRemaining,
        canSendToEmail = canSendToEmail,
        recipient = RECIPIENT,
        phoneNumberLastTwoDigits = "55",
    )

    private companion object {
        const val EMAIL = "jenny.rosen@example.com"
        const val RECIPIENT = "(•••) ••• ••55"
    }
}
