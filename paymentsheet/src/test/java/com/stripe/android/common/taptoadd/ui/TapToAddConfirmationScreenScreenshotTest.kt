package com.stripe.android.common.taptoadd.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.screenshottesting.PaparazziRule
import org.junit.Rule
import org.junit.Test

internal class TapToAddConfirmationScreenScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule()

    @Test
    fun idle() {
        screenshotTest(
            primaryButtonState = TapToAddConfirmationInteractor.State.PrimaryButton.State.Idle,
            error = null,
        )
    }

    @Test
    fun processing() {
        screenshotTest(
            primaryButtonState = TapToAddConfirmationInteractor.State.PrimaryButton.State.Processing,
            error = null,
        )
    }

    @Test
    fun complete() {
        screenshotTest(
            primaryButtonState = TapToAddConfirmationInteractor.State.PrimaryButton.State.Complete,
            error = null,
        )
    }

    @Test
    fun error() {
        screenshotTest(
            primaryButtonState = TapToAddConfirmationInteractor.State.PrimaryButton.State.Idle,
            error = "Something went wrong".resolvableString,
        )
    }

    @Test
    fun locked() {
        screenshotTest(
            locked = true,
        )
    }

    @Test
    fun noCardInfo() {
        screenshotTest(
            last4 = null,
            locked = true,
            cardBrand = CardBrand.Unknown,
        )
    }

    private fun screenshotTest(
        cardBrand: CardBrand = CardBrand.Visa,
        last4: String? = "4242",
        locked: Boolean = false,
        primaryButtonState: TapToAddConfirmationInteractor.State.PrimaryButton.State =
            TapToAddConfirmationInteractor.State.PrimaryButton.State.Idle,
        error: ResolvableString? = null,
    ) {
        paparazziRule.snapshot {
            TapToAddTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.background)
                        .padding(10.dp)
                ) {
                    TapToAddConfirmationScreen(
                        state = TapToAddConfirmationInteractor.State(
                            cardBrand = cardBrand,
                            last4 = last4,
                            title = "Pay $10.00".resolvableString,
                            primaryButton = TapToAddConfirmationInteractor.State.PrimaryButton(
                                label = "Pay".resolvableString,
                                locked = locked,
                                state = primaryButtonState,
                                enabled = true,
                            ),
                            form = TapToAddConfirmationInteractor.State.Form(
                                elements = emptyList(),
                                enabled = true,
                            ),
                            error = error,
                        ),
                        onComplete = {},
                        onPrimaryButtonPress = {},
                    )
                }
            }
        }
    }
}
