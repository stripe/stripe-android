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

    private fun screenshotTest(
        primaryButtonState: TapToAddConfirmationInteractor.State.PrimaryButton.State,
        error: ResolvableString?,
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
                            cardBrand = CardBrand.Visa,
                            last4 = "4242",
                            title = "Pay $10.00".resolvableString,
                            primaryButton = TapToAddConfirmationInteractor.State.PrimaryButton(
                                label = "Pay".resolvableString,
                                locked = false,
                                state = primaryButtonState,
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
