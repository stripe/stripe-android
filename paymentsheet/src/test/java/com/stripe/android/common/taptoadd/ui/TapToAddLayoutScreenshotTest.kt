package com.stripe.android.common.taptoadd.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.screenshottesting.PaparazziRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import java.lang.IllegalStateException
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
                                        interactor = FakeTapToAddConfirmationInteractor,
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

    private class FakeTapToAddCardAddedInteractor(
        private val onShown: () -> Unit,
    ) : TapToAddCardAddedInteractor {
        override val cardBrand: CardBrand = CardBrand.Visa
        override val last4: String = "4242"

        override fun onShown() {
            onShown.invoke()
        }
    }

    private object FakeTapToAddConfirmationInteractor : TapToAddConfirmationInteractor {
        override val state: StateFlow<TapToAddConfirmationInteractor.State> = MutableStateFlow(
            TapToAddConfirmationInteractor.State(
                cardBrand = CardBrand.Visa,
                last4 = "4242",
                title = "Pay $50.99".resolvableString,
                primaryButton = TapToAddConfirmationInteractor.State.PrimaryButton(
                    label = "Pay".resolvableString,
                    locked = true,
                    state = TapToAddConfirmationInteractor.State.PrimaryButton.State.Idle,
                ),
                error = null,
            )
        )

        override fun performAction(action: TapToAddConfirmationInteractor.Action) {
            throw IllegalStateException("Should not be called!")
        }
    }
}
