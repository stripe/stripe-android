package com.stripe.android.paymentsheet.verticalmode

import android.os.Build
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.definitions.CardDefinition
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.ViewActionRecorder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal class PaymentMethodLayoutUITest(
    private val paymentMethodsTag: String,
    private val allPaymentMethodsChildCount: Int,
    private val layoutUI:
    @Composable (interactor: FakePaymentMethodVerticalLayoutInteractor, modifier: Modifier) -> Unit
) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun clickingOnViewMore_transitionsToManageScreen() = runScenario(
        PaymentMethodVerticalLayoutInteractor.State(
            displayablePaymentMethods = emptyList(),
            isProcessing = false,
            selection = null,
            displayedSavedPaymentMethod = PaymentMethodFixtures.displayableCard(),
            availableSavedPaymentMethodAction =
            PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL,
        )
    ) {
        assertThat(viewActionRecorder.viewActions).isEmpty()
        composeRule.onNodeWithTag(TEST_TAG_VIEW_MORE).performClick()
        viewActionRecorder.consume(
            PaymentMethodVerticalLayoutInteractor.ViewAction.TransitionToManageSavedPaymentMethods
        )
        assertThat(viewActionRecorder.viewActions).isEmpty()
    }

    @Test
    fun oneSavedPm_canBeRemoved_buttonIsEdit_callsOnManageOneSavedPm() = runScenario(
        PaymentMethodVerticalLayoutInteractor.State(
            displayablePaymentMethods = emptyList(),
            isProcessing = false,
            selection = null,
            displayedSavedPaymentMethod = PaymentMethodFixtures.displayableCard(),
            availableSavedPaymentMethodAction =
            PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ONE,
        )
    ) {
        assertThat(viewActionRecorder.viewActions).isEmpty()
        composeRule.onNodeWithTag(TEST_TAG_EDIT_SAVED_CARD).performClick()
        viewActionRecorder.consume(
            PaymentMethodVerticalLayoutInteractor.ViewAction.OnManageOneSavedPaymentMethod(
                PaymentMethodFixtures.displayableCard()
            )
        )
        assertThat(viewActionRecorder.viewActions).isEmpty()
    }

    @Test
    fun oneSavedPm_cannotBeEdited_noSavedPaymentMethodButton() = runScenario(
        PaymentMethodVerticalLayoutInteractor.State(
            displayablePaymentMethods = emptyList(),
            isProcessing = false,
            selection = null,
            displayedSavedPaymentMethod = PaymentMethodFixtures.displayableCard(),
            availableSavedPaymentMethodAction =
            PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.NONE,
        )
    ) {
        composeRule.onNodeWithTag(
            TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON + "_${PaymentMethodFixtures.displayableCard().paymentMethod.id}"
        ).assertExists()

        composeRule.onNodeWithTag(TEST_TAG_EDIT_SAVED_CARD).assertDoesNotExist()
        composeRule.onNodeWithTag(TEST_TAG_VIEW_MORE).assertDoesNotExist()
    }

    @Test
    fun clickingOnNewPaymentMethod_callsOnClick() {
        var onClickCalled = false
        runScenario(
            PaymentMethodVerticalLayoutInteractor.State(
                displayablePaymentMethods = listOf(
                    CardDefinition.uiDefinitionFactory().supportedPaymentMethod(CardDefinition, emptyList())!!
                        .asDisplayablePaymentMethod(
                            customerSavedPaymentMethods = emptyList(),
                            incentive = null,
                            onClick = { onClickCalled = true },
                        ),
                ),
                isProcessing = false,
                selection = null,
                displayedSavedPaymentMethod = null,
                availableSavedPaymentMethodAction =
                PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL,
            )
        ) {
            assertThat(onClickCalled).isFalse()
            composeRule.onNodeWithTag(TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON + "_card").performClick()
            assertThat(onClickCalled).isTrue()
            assertThat(viewActionRecorder.viewActions).isEmpty()
        }
    }

    @Test
    fun clickingSavedPaymentMethod_callsSelectSavedPaymentMethod() {
        val savedPaymentMethod = PaymentMethodFixtures.displayableCard()
        runScenario(
            PaymentMethodVerticalLayoutInteractor.State(
                displayablePaymentMethods = emptyList(),
                isProcessing = false,
                selection = null,
                displayedSavedPaymentMethod = savedPaymentMethod,
                availableSavedPaymentMethodAction = PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.NONE,
            )
        ) {
            assertThat(viewActionRecorder.viewActions).isEmpty()
            composeRule.onNodeWithTag(
                TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON + "_${savedPaymentMethod.paymentMethod.id}"
            ).performClick()
            viewActionRecorder.consume(
                PaymentMethodVerticalLayoutInteractor.ViewAction.SavedPaymentMethodSelected(
                    savedPaymentMethod.paymentMethod
                )
            )
            assertThat(viewActionRecorder.viewActions).isEmpty()
        }
    }

    @Test
    fun allPaymentMethodsAreShown() = runScenario(
        PaymentMethodVerticalLayoutInteractor.State(
            displayablePaymentMethods = PaymentMethodMetadataFactory.create(
                PaymentIntentFixtures.PI_WITH_PAYMENT_METHOD!!.copy(
                    paymentMethodTypes = listOf("card", "cashapp", "klarna")
                )
            ).sortedSupportedPaymentMethods().map {
                it.asDisplayablePaymentMethod(
                    customerSavedPaymentMethods = emptyList(),
                    incentive = null,
                    onClick = {},
                )
            },
            isProcessing = false,
            selection = null,
            displayedSavedPaymentMethod = PaymentMethodFixtures.displayableCard(),
            availableSavedPaymentMethodAction =
            PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL,
        )
    ) {
        assertThat(
            composeRule.onNodeWithTag(paymentMethodsTag)
                .onChildren().fetchSemanticsNodes().size
        ).isEqualTo(allPaymentMethodsChildCount)

        composeRule.onNodeWithTag(TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON + "_card").assertExists()
        composeRule.onNodeWithTag(TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON + "_cashapp").assertExists()
        composeRule.onNodeWithTag(TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON + "_klarna").assertExists()

        composeRule.onNodeWithTag(
            TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON + "_${PaymentMethodFixtures.displayableCard().paymentMethod.id}"
        ).assertExists()
    }

    @Test
    fun savedPaymentMethodIsSelected_whenSelectionIsSavedPm() = runScenario(
        PaymentMethodVerticalLayoutInteractor.State(
            displayablePaymentMethods = PaymentMethodMetadataFactory.create(
                PaymentIntentFixtures.PI_WITH_PAYMENT_METHOD!!.copy(
                    paymentMethodTypes = listOf("card", "cashapp", "klarna")
                )
            ).sortedSupportedPaymentMethods().map {
                it.asDisplayablePaymentMethod(
                    customerSavedPaymentMethods = emptyList(),
                    incentive = null,
                    onClick = {},
                )
            },
            isProcessing = false,
            selection = PaymentMethodVerticalLayoutInteractor.Selection.Saved,
            displayedSavedPaymentMethod = PaymentMethodFixtures.displayableCard(),
            availableSavedPaymentMethodAction =
            PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL,
        )
    ) {
        composeRule.onNodeWithTag(
            TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON + "_${PaymentMethodFixtures.displayableCard().paymentMethod.id}"
        ).assertExists()
            .assert(isSelected())

        composeRule.onNodeWithTag(TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON + "_card")
            .assertExists()
            .onChildren().assertAll(isSelected().not())
        composeRule.onNodeWithTag(TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON + "_cashapp")
            .assertExists()
            .onChildren().assertAll(isSelected().not())
        composeRule.onNodeWithTag(TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON + "_klarna")
            .assertExists()
            .onChildren().assertAll(isSelected().not())
    }

    @Test
    fun correctLPMIsSelected() {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            PaymentIntentFixtures.PI_WITH_PAYMENT_METHOD!!.copy(
                paymentMethodTypes = listOf("card", "cashapp", "klarna")
            )
        )
        val supportedPaymentMethods = paymentMethodMetadata.sortedSupportedPaymentMethods()
        val selection = PaymentMethodVerticalLayoutInteractor.Selection.New(supportedPaymentMethods[1].code)
        runScenario(
            PaymentMethodVerticalLayoutInteractor.State(
                displayablePaymentMethods = supportedPaymentMethods.map {
                    it.asDisplayablePaymentMethod(
                        customerSavedPaymentMethods = emptyList(),
                        incentive = null,
                        onClick = {},
                    )
                },
                isProcessing = false,
                selection = selection,
                displayedSavedPaymentMethod = null,
                availableSavedPaymentMethodAction =
                PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL,
            )
        ) {
            assertThat(
                composeRule.onNodeWithTag(paymentMethodsTag)
                    .onChildren().fetchSemanticsNodes().size
            ).isEqualTo(3)

            composeRule.onNodeWithTag(TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON + "_card")
                .assertExists()
                .onChildren().assertAll(isSelected().not())
            composeRule.onNodeWithTag(TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON + "_cashapp")
                .assertExists()
                .assert(isSelected())
            composeRule.onNodeWithTag(TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON + "_klarna")
                .assertExists()
                .onChildren().assertAll(isSelected().not())
        }
    }

    private fun runScenario(
        initialState: PaymentMethodVerticalLayoutInteractor.State,
        block: Scenario.() -> Unit
    ) {
        val viewActionRecorder = ViewActionRecorder<PaymentMethodVerticalLayoutInteractor.ViewAction>()
        val interactor = FakePaymentMethodVerticalLayoutInteractor(
            initialState = initialState,
            viewActionRecorder = viewActionRecorder,
        )

        composeRule.setContent {
            layoutUI.invoke(interactor, Modifier.padding(horizontal = 20.dp))
        }

        Scenario(viewActionRecorder).apply(block)
    }

    private data class Scenario(
        val viewActionRecorder: ViewActionRecorder<PaymentMethodVerticalLayoutInteractor.ViewAction>,
    )

    private companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters
        fun data() = listOf(
            parameters(
                paymentMethodsTag = TEST_TAG_NEW_PAYMENT_METHOD_VERTICAL_LAYOUT_UI,
                allPaymentMethodsChildCount = 3,
                layoutUI = { interactor, modifier ->
                    PaymentMethodVerticalLayoutUI(interactor, modifier)
                }
            ),
            parameters(
                paymentMethodsTag = TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT,
                allPaymentMethodsChildCount = 4,
                layoutUI = { interactor, modifier ->
                    PaymentMethodEmbeddedLayoutUI(interactor, modifier, Embedded.RowStyle.FloatingButton.default)
                }
            )
        )

        private fun parameters(
            paymentMethodsTag: String,
            allPaymentMethodsChildCount: Int,
            layoutUI: @Composable (interactor: FakePaymentMethodVerticalLayoutInteractor, modifier: Modifier) -> Unit
        ) = arrayOf(paymentMethodsTag, allPaymentMethodsChildCount, layoutUI)
    }
}
