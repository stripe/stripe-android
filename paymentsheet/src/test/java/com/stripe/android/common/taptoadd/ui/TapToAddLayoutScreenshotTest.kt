package com.stripe.android.common.taptoadd.ui

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.stripe.android.common.taptoadd.TapToAddErrorMessage
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test
import kotlin.String

class TapToAddLayoutScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        boxModifier = Modifier,
        includeStripeTheme = false,
    )

    @Test
    fun sharedTransitionFromCardAddedToDelayToConfirmation() {
        paparazziRule.gif(
            end = 3500L
        ) {
            TapToAddTheme(imageRepository = null) {
                var screen by remember {
                    mutableStateOf<TapToAddNavigator.Screen>(
                        TapToAddNavigator.Screen.CardAdded(
                            interactor = FakeTapToAddCardAddedInteractor(),
                        )
                    )
                }

                LaunchedEffect(Unit) {
                    delay(1000L)
                    screen = TapToAddNavigator.Screen.Delay(
                        interactor = FakeTapToAddDelayInteractor(),
                    )
                    delay(1000L)
                    screen = TapToAddNavigator.Screen.Confirmation(
                        interactor = FakeTapToAddConfirmationInteractor(),
                    )
                }

                TapToAddLayout(
                    screen = screen,
                ) { }
            }
        }
    }

    @Test
    fun collecting() {
        paparazziRule.snapshot {
            TapToAddTheme(imageRepository = null) {
                TapToAddLayout(
                    screen = TapToAddNavigator.Screen.Collecting(FakeTapToAddCollectingInteractor()),
                ) {}
            }
        }
    }

    @Test
    fun cardAdded() {
        paparazziRule.snapshot {
            TapToAddTheme(imageRepository = null) {
                TapToAddLayout(
                    screen = TapToAddNavigator.Screen.CardAdded(FakeTapToAddCardAddedInteractor()),
                ) {}
            }
        }
    }

    @Test
    fun error() {
        paparazziRule.gif(
            end = 4500L
        ) {
            TapToAddTheme(imageRepository = null) {
                TapToAddLayout(
                    screen = TapToAddNavigator.Screen.Error(
                        message = TapToAddErrorMessage(
                            title = "Something wrong".resolvableString,
                            action = "Try again".resolvableString,
                        ),
                        dueToUnsupportedDevice = false,
                    ),
                ) {}
            }
        }
    }

    @Test
    fun confirmationIdle() {
        confirmationScreenshotTest(
            primaryButtonState = TapToAddConfirmationInteractor.State.PrimaryButton.State.Idle,
            error = null,
        )
    }

    @Test
    fun confirmationProcessing() {
        confirmationScreenshotTest(
            primaryButtonState = TapToAddConfirmationInteractor.State.PrimaryButton.State.Processing,
            error = null,
        )
    }

    @Test
    fun confirmationSuccess() {
        confirmationScreenshotTest(
            primaryButtonState = TapToAddConfirmationInteractor.State.PrimaryButton.State.Success,
            error = null,
        )
    }

    @Test
    fun confirmationWithCvc() {
        confirmationScreenshotTest(
            showCvcElement = true,
            error = null,
        )
    }

    @Test
    fun confirmationWithCvcFilledIn() {
        confirmationScreenshotTest(
            showCvcElement = true,
            cvcInitialValue = "223",
            error = null,
        )
    }

    @Test
    fun confirmationError() {
        confirmationScreenshotTest(
            primaryButtonState = TapToAddConfirmationInteractor.State.PrimaryButton.State.Idle,
            error = "Something went wrong".resolvableString,
        )
    }

    @Test
    fun confirmationLocked() {
        confirmationScreenshotTest(
            locked = true,
        )
    }

    @Test
    fun confirmationNoCardInfo() {
        confirmationScreenshotTest(
            last4 = null,
            locked = true,
            cardBrand = CardBrand.Unknown,
        )
    }

    private fun confirmationScreenshotTest(
        showCvcElement: Boolean = false,
        cvcInitialValue: String? = null,
        cardBrand: CardBrand = CardBrand.Visa,
        last4: String? = "4242",
        locked: Boolean = false,
        primaryButtonState: TapToAddConfirmationInteractor.State.PrimaryButton.State =
            TapToAddConfirmationInteractor.State.PrimaryButton.State.Idle,
        error: ResolvableString? = null,
    ) {
        paparazziRule.snapshot {
            TapToAddTheme(imageRepository = null) {
                TapToAddLayout(
                    screen = TapToAddNavigator.Screen.Confirmation(
                        interactor = FakeTapToAddConfirmationInteractor(
                            showCvcElement = showCvcElement,
                            cvcInitialValue = cvcInitialValue,
                            cardBrand = cardBrand,
                            last4 = last4,
                            locked = locked,
                            primaryButtonState = primaryButtonState,
                            error = error,
                        ),
                    ),
                ) {}
            }
        }
    }
}
