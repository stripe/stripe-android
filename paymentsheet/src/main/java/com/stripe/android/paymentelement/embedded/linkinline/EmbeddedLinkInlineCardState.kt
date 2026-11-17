package com.stripe.android.paymentelement.embedded.linkinline

import androidx.compose.runtime.Immutable
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.verification.LinkAuthFlowState
import com.stripe.android.paymentsheet.R

/**
 * What the inline Link verification card shows for an attempt.
 */
@Immutable
internal data class EmbeddedLinkInlineCardState(
    val email: String,
    val recipient: String,
    val isCodeEntryEnabled: Boolean,
    val errorMessage: ResolvableString?,
    val actions: Actions,
    /**
     * Whether a code is being sent, which shows the action button as loading.
     */
    val isActionLoading: Boolean,
    /**
     * Changes whenever the code field should be cleared.
     */
    val inputRevision: Int,
    /**
     * Changes whenever a submitted code was rejected, which plays the invalid-code animation.
     */
    val invalidCodeRevision: Int,
) {
    sealed interface Actions {
        data object None : Actions

        /**
         * The only action, resending the code, shown inline.
         */
        data class Resend(
            val label: ResolvableString,
            val isEnabled: Boolean,
        ) : Actions

        /**
         * A "More options" button opening a menu.
         */
        data class Menu(
            val items: List<MenuItem>,
            val isEnabled: Boolean,
        ) : Actions
    }

    data class MenuItem(
        val action: Action,
        val label: ResolvableString,
        val isEnabled: Boolean,
    )

    enum class Action {
        Resend,
        Email,
    }

    companion object {
        fun create(state: LinkAuthFlowState, email: String): EmbeddedLinkInlineCardState {
            val isSendingCode = state.isResending ||
                (state.isLoading && !state.isCodeSent && state.screen == LinkAuthFlowState.Screen.Otp)
            return EmbeddedLinkInlineCardState(
                email = email,
                recipient = state.recipient,
                isCodeEntryEnabled = state.canSubmitCode,
                errorMessage = state.errorMessage,
                actions = actions(state, isSendingCode),
                isActionLoading = isSendingCode,
                inputRevision = state.inputRevision,
                invalidCodeRevision = state.invalidCodeRevision,
            )
        }

        private fun actions(state: LinkAuthFlowState, isSendingCode: Boolean): Actions {
            if (state.screen != LinkAuthFlowState.Screen.Otp) {
                return Actions.None
            }
            val resendLabel = if (state.resendSecondsRemaining > 0) {
                resolvableString(R.string.stripe_link_auth_resend_countdown, state.resendSecondsRemaining)
            } else {
                resolvableString(R.string.stripe_verification_resend)
            }
            if (!state.canSendToEmail) {
                return Actions.Resend(label = resendLabel, isEnabled = state.canResend)
            }
            return Actions.Menu(
                items = listOf(
                    MenuItem(action = Action.Resend, label = resendLabel, isEnabled = state.canResend),
                    MenuItem(
                        action = Action.Email,
                        label = resolvableString(R.string.stripe_link_auth_email_code),
                        isEnabled = true,
                    ),
                ),
                isEnabled = !isSendingCode,
            )
        }
    }
}
