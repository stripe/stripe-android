package com.stripe.android.paymentsheet.ui

import com.stripe.android.link.ui.LinkButtonState
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentelement.WalletButtonsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.ButtonThemes.LinkButtonTheme
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test

@OptIn(WalletButtonsPreview::class, AppearanceAPIAdditionsPreview::class)
class WalletButtonsScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule()

    @get:Rule
    val scopedThemePaparazziRule = PaparazziRule(
        SystemAppearance.entries,
        includeStripeTheme = false,
    )

    @Test
    fun oneButton() {
        val walletButtonsContent = createWalletButtonsContent(
            appearance = PaymentSheet.Appearance(),
            walletButtons = listOf(
                WalletButtonsInteractor.WalletButton.Link(
                    state = LinkButtonState.Email("email@email.com"),
                    linkBrand = LinkBrand.Link,
                )
            ),
            buttonsEnabled = true,
        )

        paparazziRule.snapshot {
            walletButtonsContent.Content { false }
        }
    }

    @Test
    fun oneButtonDisabled() {
        val walletButtonsContent = createWalletButtonsContent(
            appearance = PaymentSheet.Appearance(),
            walletButtons = listOf(
                WalletButtonsInteractor.WalletButton.Link(
                    state = LinkButtonState.Email("email@email.com"),
                    linkBrand = LinkBrand.Link,
                ),
            ),
            buttonsEnabled = false,
        )

        paparazziRule.snapshot {
            walletButtonsContent.Content { false }
        }
    }

    @Test
    fun automaticTheme() {
        snapshotWithAppearance(PaymentSheet.Appearance())
    }

    @Test
    fun alwaysLightTheme() {
        snapshotWithAppearance(
            PaymentSheet.Appearance(themeMode = PaymentSheet.ThemeMode.AlwaysLight),
        )
    }

    @Test
    fun alwaysDarkTheme() {
        snapshotWithAppearance(
            PaymentSheet.Appearance(themeMode = PaymentSheet.ThemeMode.AlwaysDark),
        )
    }

    @Test
    fun customAppearanceTheme() {
        snapshotWithAppearance(PaymentSheetAppearance.CrazyAppearance.appearance)
    }

    private fun snapshotWithAppearance(appearance: PaymentSheet.Appearance) {
        val walletButtonsContent = createWalletButtonsContent(
            appearance = appearance,
            walletButtons = listOf(
                WalletButtonsInteractor.WalletButton.Link(
                    state = LinkButtonState.Email("email@email.com"),
                    linkBrand = LinkBrand.Link,
                    theme = LinkButtonTheme.WHITE,
                )
            ),
            buttonsEnabled = true,
        )

        scopedThemePaparazziRule.snapshot {
            walletButtonsContent.Content { false }
        }
    }

    private fun createWalletButtonsContent(
        appearance: PaymentSheet.Appearance,
        walletButtons: List<WalletButtonsInteractor.WalletButton>,
        buttonsEnabled: Boolean,
    ) = WalletButtonsContent(
        interactor = FakeWalletButtonsInteractor(
            state = WalletButtonsInteractor.State(
                appearance = appearance,
                walletButtons = walletButtons,
                buttonsEnabled = buttonsEnabled,
                link2FAState = null
            )
        )
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
