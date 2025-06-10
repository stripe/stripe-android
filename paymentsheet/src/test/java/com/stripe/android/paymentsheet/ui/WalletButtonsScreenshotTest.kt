package com.stripe.android.paymentsheet.ui

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.verification.VerificationViewState
import com.stripe.android.paymentsheet.ui.WalletButtonsInteractor.State.LinkOtpState
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.ui.core.elements.OTPSpec
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test

class WalletButtonsScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        FontSize.entries,
        PaymentSheetAppearance.entries
    )

    @Test
    fun oneButton() {
        val walletButtonsContent = createWalletButtonsContent(
            walletButtons = listOf(
                WalletButtonsInteractor.WalletButton.Link(
                    email = "email@email.com",
                )
            ),
            buttonsEnabled = true,
        )

        paparazziRule.snapshot {
            walletButtonsContent.Content()
        }
    }

    @Test
    fun oneButtonDisabled() {
        val walletButtonsContent = createWalletButtonsContent(
            walletButtons = listOf(
                WalletButtonsInteractor.WalletButton.Link(
                    email = "email@email.com",
                ),
            ),
            buttonsEnabled = false,
        )

        paparazziRule.snapshot {
            walletButtonsContent.Content()
        }
    }

    @Test
    fun oneButtonAnd2FAIdle() {
        val walletButtonsContent = createWalletButtonsContent(
            walletButtons = listOf(
                WalletButtonsInteractor.WalletButton.Link(
                    email = "email@email.com",
                )
            ),
            linkOtpState = createLinkOTPState(
                isProcessing = false,
                errorMessage = null
            ),
            buttonsEnabled = true
        )
        paparazziRule.snapshot {
            walletButtonsContent.Content()
        }
    }

    @Test
    fun oneButtonAnd2FAProcessing() {
        val walletButtonsContent = createWalletButtonsContent(
            walletButtons = listOf(
                WalletButtonsInteractor.WalletButton.Link(
                    email = "email@email.com",
                )
            ),
            linkOtpState = createLinkOTPState(
                isProcessing = true,
                errorMessage = null
            ),
            buttonsEnabled = true
        )
        paparazziRule.snapshot {
            walletButtonsContent.Content()
        }
    }

    @Test
    fun oneButtonAnd2FAError() {
        val walletButtonsContent = createWalletButtonsContent(
            walletButtons = listOf(
                WalletButtonsInteractor.WalletButton.Link(
                    email = "email@email.com",
                )
            ),
            linkOtpState = createLinkOTPState(
                isProcessing = false,
                errorMessage = "Something went wrong, try again later"
            ),
            buttonsEnabled = true
        )
        paparazziRule.snapshot {
            walletButtonsContent.Content()
        }
    }


    private fun createWalletButtonsContent(
        walletButtons: List<WalletButtonsInteractor.WalletButton>,
        buttonsEnabled: Boolean,
        linkOtpState: LinkOtpState? = null,
    ) = WalletButtonsContent(
        interactor = FakeWalletButtonsInteractor(
            state = WalletButtonsInteractor.State(
                walletButtons = walletButtons,
                buttonsEnabled = buttonsEnabled,
                link2FAState = linkOtpState
            )
        )
    )

    private fun createLinkOTPState(
        isProcessing: Boolean,
        errorMessage: String?,
    ): LinkOtpState = LinkOtpState(
        viewState = VerificationViewState(
            isProcessing = isProcessing,
            requestFocus = true,
            errorMessage = errorMessage?.resolvableString,
            isSendingNewCode = false,
            didSendNewCode = false,
            redactedPhoneNumber = "123-456-7890",
            email = "test@test.com",
            isDialog = false
        ),
        otpElement = OTPSpec.transform()
    )

    private class FakeWalletButtonsInteractor(
        state: WalletButtonsInteractor.State,
    ) : WalletButtonsInteractor {
        override val state: StateFlow<WalletButtonsInteractor.State> = stateFlowOf(state)

        override fun handleViewAction(action: WalletButtonsInteractor.ViewAction) {
            when (action) {
                is WalletButtonsInteractor.ViewAction.OnShown,
                is WalletButtonsInteractor.ViewAction.OnHidden,
                WalletButtonsInteractor.ViewAction.OnResendCode,
                WalletButtonsInteractor.ViewAction.OnResendCodeNotificationSent -> Unit
                is WalletButtonsInteractor.ViewAction.OnButtonPressed -> error("Should not be called!")
            }
        }
    }
}
