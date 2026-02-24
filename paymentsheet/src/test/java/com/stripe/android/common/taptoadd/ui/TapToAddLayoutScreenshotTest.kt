package com.stripe.android.common.taptoadd.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.screenshottesting.PaparazziRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import kotlin.String
import kotlin.properties.Delegates

class TapToAddLayoutScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier,
        includeStripeTheme = false,
    )

    @Test
    fun sharedTransitionFromCardAddedToConfirmation() {
        paparazziRule.gif(
            end = 4500L
        ) {
            TapToAddTheme {
                val screen by remember {
                    var state by Delegates.notNull<MutableState<TapToAddNavigator.Screen>>()

                    state = mutableStateOf(
                        TapToAddNavigator.Screen.CardAdded(
                            interactor = FakeTapToAddCardAddedInteractor(
                                onShown = {
                                    state.value = TapToAddNavigator.Screen.Confirmation(
                                        interactor = FakeTapToAddConfirmationInteractor(),
                                    )
                                }
                            )
                        )
                    )

                    state
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
            TapToAddTheme {
                TapToAddLayout(
                    screen = TapToAddNavigator.Screen.Collecting(FakeTapToAddCollectingInteractor),
                ) {}
            }
        }
    }

    @Test
    fun error() {
        paparazziRule.gif(
            end = 4500L
        ) {
            TapToAddTheme {
                TapToAddLayout(
                    screen = TapToAddNavigator.Screen.Error(
                        message = "Something went wrong".resolvableString
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
    fun confirmationComplete() {
        confirmationScreenshotTest(
            primaryButtonState = TapToAddConfirmationInteractor.State.PrimaryButton.State.Complete,
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
        cardBrand: CardBrand = CardBrand.Visa,
        last4: String? = "4242",
        locked: Boolean = false,
        primaryButtonState: TapToAddConfirmationInteractor.State.PrimaryButton.State =
            TapToAddConfirmationInteractor.State.PrimaryButton.State.Idle,
        error: ResolvableString? = null,
    ) {
        paparazziRule.snapshot {
            TapToAddTheme {
                TapToAddLayout(
                    screen = TapToAddNavigator.Screen.Confirmation(
                        interactor = FakeTapToAddConfirmationInteractor(
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

    private object FakeTapToAddCollectingInteractor : TapToAddCollectingInteractor

    private class FakeTapToAddCardAddedInteractor(
        private val onShown: () -> Unit,
    ) : TapToAddCardAddedInteractor {
        override val cardBrand: CardBrand = CardBrand.Visa
        override val last4: String = "4242"

        override fun onShown() {
            onShown.invoke()
        }
    }

    private class FakeTapToAddConfirmationInteractor(
        cardBrand: CardBrand = CardBrand.Visa,
        last4: String? = "4242",
        locked: Boolean = true,
        primaryButtonState: TapToAddConfirmationInteractor.State.PrimaryButton.State =
            TapToAddConfirmationInteractor.State.PrimaryButton.State.Idle,
        error: ResolvableString? = null,
    ) : TapToAddConfirmationInteractor {
        override val state: StateFlow<TapToAddConfirmationInteractor.State> = MutableStateFlow(
            TapToAddConfirmationInteractor.State(
                cardBrand = cardBrand,
                last4 = last4,
                title = "Pay $50.99".resolvableString,
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
            )
        )

        override fun performAction(action: TapToAddConfirmationInteractor.Action) {
            // No-op
        }
    }
}
