@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.elements.ece.ExpressButton
import com.stripe.android.elements.ece.ExpressCheckoutElementContent
import com.stripe.android.elements.ece.ExpressCheckoutElementInteractor
import com.stripe.android.elements.ece.ExpressCheckoutElementInteractorStateFactory
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test

internal class ExpressCheckoutElementScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        boxModifier = Modifier.fillMaxWidth(),
    )

    @Test
    fun rendersGooglePayAndLinkButtons() {
        paparazziRule.snapshot {
            ExpressCheckoutElementContent(
                interactor = FakeExpressCheckoutElementInteractor(),
                googlePayButton = { _, _ -> FakeGooglePayButton() },
            )
        }
    }

    @Test
    fun rendersGooglePayAndLinkButtonsInOneRow() {
        paparazziRule.snapshot {
            ExpressCheckoutElementContent(
                interactor = FakeExpressCheckoutElementInteractor(
                    state = stateFlowOf(
                        ExpressCheckoutElementInteractorStateFactory.create(
                            buttonLayout = ExpressCheckoutElement.Configuration.Appearance.ButtonLayout()
                                .maxRows(1)
                                .build(),
                        )
                    ),
                ),
                googlePayButton = { _, _ -> FakeGooglePayButton() },
            )
        }
    }

    @Test
    fun rendersThreeButtonsInTwoByTwoGrid() {
        val defaultState = ExpressCheckoutElementInteractorStateFactory.create()
        val thirdButton = defaultState.expressButtons
            .filterIsInstance<ExpressButton.GooglePay>()
            .single()
            .copy(shippingAddressRequired = true)

        paparazziRule.snapshot {
            ExpressCheckoutElementContent(
                interactor = FakeExpressCheckoutElementInteractor(
                    state = stateFlowOf(
                        defaultState.copy(
                            expressButtons = defaultState.expressButtons + thirdButton,
                            buttonLayout = ExpressCheckoutElement.Configuration.Appearance.ButtonLayout()
                                .maxColumns(2)
                                .maxRows(2)
                                .build(),
                        )
                    ),
                ),
                googlePayButton = { _, _ -> FakeGooglePayButton() },
            )
        }
    }

    @Composable
    private fun FakeGooglePayButton() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    color = Color.Black,
                    shape = RoundedCornerShape(6.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Google Pay",
                color = Color.White,
                fontWeight = FontWeight.Medium,
            )
        }
    }

    private class FakeExpressCheckoutElementInteractor(
        override val state: StateFlow<ExpressCheckoutElementInteractor.State> = stateFlowOf(
            ExpressCheckoutElementInteractorStateFactory.create(),
        ),
    ) : ExpressCheckoutElementInteractor {
        override fun handleViewAction(viewAction: ExpressCheckoutElementInteractor.ViewAction) = Unit
    }
}
