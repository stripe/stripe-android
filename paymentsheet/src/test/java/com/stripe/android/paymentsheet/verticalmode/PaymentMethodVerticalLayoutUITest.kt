package com.stripe.android.paymentsheet.verticalmode

import android.os.Build
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.definitions.CardDefinition
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.ViewActionRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class PaymentMethodVerticalLayoutUITest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun clickingOnManageScreen_transitionsToManageScreen() = runScenario(
        PaymentMethodVerticalLayoutInteractor.State(
            supportedPaymentMethods = emptyList(),
            isProcessing = false,
            selectedPaymentMethodIndex = -1,
        )
    ) {
        assertThat(viewActionRecorder.viewActions).isEmpty()
        composeRule.onNodeWithTag(TEST_TAG_MANAGE_SCREEN).performClick()
        viewActionRecorder.consume(
            PaymentMethodVerticalLayoutInteractor.ViewAction.TransitionToManageSavedPaymentMethods
        )
        assertThat(viewActionRecorder.viewActions).isEmpty()
    }

    @Test
    fun clickingOnNewPaymentMethod_transitionsToForm() = runScenario(
        PaymentMethodVerticalLayoutInteractor.State(
            supportedPaymentMethods = listOf(
                CardDefinition.uiDefinitionFactory().supportedPaymentMethod(CardDefinition, emptyList())!!,
            ),
            isProcessing = false,
            selectedPaymentMethodIndex = -1,
        )
    ) {
        assertThat(viewActionRecorder.viewActions).isEmpty()
        composeRule.onNodeWithTag(TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON + "_card").performClick()
        viewActionRecorder.consume(PaymentMethodVerticalLayoutInteractor.ViewAction.PaymentMethodSelected("card"))
        assertThat(viewActionRecorder.viewActions).isEmpty()
    }

    @Test
    fun allSupportedPaymentMethodsAreShown() = runScenario(
        PaymentMethodVerticalLayoutInteractor.State(
            supportedPaymentMethods = PaymentMethodMetadataFactory.create(
                PaymentIntentFixtures.PI_WITH_PAYMENT_METHOD!!.copy(
                    paymentMethodTypes = listOf("card", "cashapp", "klarna")
                )
            ).sortedSupportedPaymentMethods(),
            isProcessing = false,
            selectedPaymentMethodIndex = -1,
        )
    ) {
        assertThat(
            composeRule.onNodeWithTag(TEST_TAG_NEW_PAYMENT_METHOD_VERTICAL_LAYOUT_UI)
                .onChildren().fetchSemanticsNodes().size
        ).isEqualTo(3)

        composeRule.onNodeWithTag(TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON + "_card").assertExists()
        composeRule.onNodeWithTag(TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON + "_cashapp").assertExists()
        composeRule.onNodeWithTag(TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON + "_klarna").assertExists()
    }

    @Test
    fun correctIndexIsSelected() = runScenario(
        PaymentMethodVerticalLayoutInteractor.State(
            supportedPaymentMethods = PaymentMethodMetadataFactory.create(
                PaymentIntentFixtures.PI_WITH_PAYMENT_METHOD!!.copy(
                    paymentMethodTypes = listOf("card", "cashapp", "klarna")
                )
            ).sortedSupportedPaymentMethods(),
            isProcessing = false,
            selectedPaymentMethodIndex = 1,
        )
    ) {
        assertThat(
            composeRule.onNodeWithTag(TEST_TAG_NEW_PAYMENT_METHOD_VERTICAL_LAYOUT_UI)
                .onChildren().fetchSemanticsNodes().size
        ).isEqualTo(3)

        composeRule.onNodeWithTag(TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON + "_card")
            .assertExists()
            .onChildren().assertAll(isSelected().not())
        composeRule.onNodeWithTag(TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON + "_cashapp")
            .assertExists()
            .onChildren().assertAny(isSelected())
        composeRule.onNodeWithTag(TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON + "_klarna")
            .assertExists()
            .onChildren().assertAll(isSelected().not())
    }

    private fun runScenario(
        initialState: PaymentMethodVerticalLayoutInteractor.State,
        block: Scenario.() -> Unit
    ) {
        val stateFlow = MutableStateFlow(initialState)
        val viewActionRecorder = ViewActionRecorder<PaymentMethodVerticalLayoutInteractor.ViewAction>()
        val interactor = createInteractor(stateFlow, viewActionRecorder)

        composeRule.setContent {
            PaymentMethodVerticalLayoutUI(interactor)
        }

        Scenario(viewActionRecorder).apply(block)
    }

    private fun createInteractor(
        stateFlow: StateFlow<PaymentMethodVerticalLayoutInteractor.State>,
        viewActionRecorder: ViewActionRecorder<PaymentMethodVerticalLayoutInteractor.ViewAction>,
    ): PaymentMethodVerticalLayoutInteractor {
        return object : PaymentMethodVerticalLayoutInteractor {
            override val state: StateFlow<PaymentMethodVerticalLayoutInteractor.State> = stateFlow

            override fun handleViewAction(viewAction: PaymentMethodVerticalLayoutInteractor.ViewAction) {
                viewActionRecorder.record(viewAction)
            }
        }
    }

    private data class Scenario(
        val viewActionRecorder: ViewActionRecorder<PaymentMethodVerticalLayoutInteractor.ViewAction>,
    )
}
