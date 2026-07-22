package com.stripe.android.checkout.ece

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test

internal class ExpressCheckoutElementContentScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
    )

    @Test
    fun testWalletButtons() {
        paparazziRule.snapshot {
            ExpressCheckoutElementContent(
                interactor = FakeExpressCheckoutElementInteractor(
                    state = ExpressCheckoutElementInteractorStateFactory.create(),
                )
            )
        }
    }

    private class FakeExpressCheckoutElementInteractor(
        state: ExpressCheckoutElementInteractor.State,
    ) : ExpressCheckoutElementInteractor {
        override val state: StateFlow<ExpressCheckoutElementInteractor.State> = stateFlowOf(state)
    }
}
