package com.stripe.android.paymentsheet.ui

import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test

class WalletButtonsScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule()

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

    private fun createWalletButtonsContent(
        walletButtons: List<WalletButtonsInteractor.WalletButton>,
        buttonsEnabled: Boolean,
    ) = WalletButtonsContent(
        interactor = FakeWalletButtonsInteractor(
            state = WalletButtonsInteractor.State(
                walletButtons = walletButtons,
                buttonsEnabled = buttonsEnabled,
            )
        )
    )

    private class FakeWalletButtonsInteractor(
        state: WalletButtonsInteractor.State,
    ) : WalletButtonsInteractor {
        override val state: StateFlow<WalletButtonsInteractor.State> = stateFlowOf(state)

        override fun handleViewAction(action: WalletButtonsInteractor.ViewAction) {
            when (action) {
                is WalletButtonsInteractor.ViewAction.OnRendered,
                is WalletButtonsInteractor.ViewAction.OnUnRendered -> Unit
                is WalletButtonsInteractor.ViewAction.OnButtonPressed -> error("Should not be called!")
            }
        }
    }
}
