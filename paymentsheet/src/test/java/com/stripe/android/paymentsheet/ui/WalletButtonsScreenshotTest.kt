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
                LinkWalletButton(
                    email = "email@email.com",
                    onPressed = {},
                )
            ),
            buttonsEnabled = true,
        )

        paparazziRule.snapshot {
            walletButtonsContent.Content()
        }
    }

    @Test
    fun twoButtons() {
        val walletButtonsContent = createWalletButtonsContent(
            walletButtons = listOf(
                LinkWalletButton(
                    email = "email@email.com",
                    onPressed = {},
                ),
                LinkWalletButton(
                    email = null,
                    onPressed = {},
                )
            ),
            buttonsEnabled = true,
        )

        paparazziRule.snapshot {
            walletButtonsContent.Content()
        }
    }

    @Test
    fun twoButtonsDisabled() {
        val walletButtonsContent = createWalletButtonsContent(
            walletButtons = listOf(
                LinkWalletButton(
                    email = "email@email.com",
                    onPressed = {},
                ),
                LinkWalletButton(
                    email = null,
                    onPressed = {},
                )
            ),
            buttonsEnabled = false,
        )

        paparazziRule.snapshot {
            walletButtonsContent.Content()
        }
    }

    @Test
    fun threeButtons() {
        val walletButtonsContent = createWalletButtonsContent(
            walletButtons = listOf(
                LinkWalletButton(
                    email = "email@email.com",
                    onPressed = {},
                ),
                LinkWalletButton(
                    email = null,
                    onPressed = {},
                ),
                LinkWalletButton(
                    email = "link@link.com",
                    onPressed = {},
                )
            ),
            buttonsEnabled = true,
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
    }
}
