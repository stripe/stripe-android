package com.stripe.android.paymentsheet.verticalmode

import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ViewActionRecorder
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor.Selection
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.utils.MockPaymentMethodsFactory
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class PaymentMethodEmbeddedLayoutUIRecoveryTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `selection failure is row-aligned, below mandate, and clears`() = runScenario {
        assertErrorPlacementAndMessage()
        clearError()
        assertErrorCleared()
    }

    private fun runScenario(block: TestScenario.() -> Unit) {
        val savedPaymentMethod = PaymentMethodFixtures.displayableCard()
        val newPaymentMethod = MockPaymentMethodsFactory.create().first().asDisplayablePaymentMethod(
            customerSavedPaymentMethods = emptyList(),
            incentive = null,
            onClick = {},
        )
        val interactor = FakePaymentMethodVerticalLayoutInteractor(
            initialState = PaymentMethodVerticalLayoutInteractor.State(
                displayablePaymentMethods = listOf(newPaymentMethod),
                isProcessing = false,
                selection = Selection.Saved,
                displayedSavedPaymentMethod = savedPaymentMethod,
                availableSavedPaymentMethodAction = SavedPaymentMethodAction.NONE,
                mandate = "Mandate".resolvableString,
                linkBrand = LinkBrand.Link,
            ),
            viewActionRecorder = ViewActionRecorder(),
        )

        composeRule.setContent {
            Column {
                PaymentMethodEmbeddedLayoutUI(
                    interactor = interactor,
                    embeddedViewDisplaysMandateText = true,
                    appearance = Embedded(Embedded.RowStyle.FloatingButton.default),
                )
            }
        }
        composeRule.waitForIdle()
        interactor.selectionErrorSource.value = IllegalStateException("Unable to update the Checkout Session.")
        composeRule.waitForIdle()

        TestScenario(
            savedPaymentMethodId = savedPaymentMethod.paymentMethod.id,
            newPaymentMethodCode = newPaymentMethod.code,
            interactor = interactor,
        ).block()
    }

    private inner class TestScenario(
        private val savedPaymentMethodId: String?,
        private val newPaymentMethodCode: String,
        private val interactor: FakePaymentMethodVerticalLayoutInteractor,
    ) {
        private val error
            get() = composeRule.onNodeWithTag(EMBEDDED_SAVED_PAYMENT_METHOD_SELECTION_ERROR_TEST_TAG)

        fun assertErrorPlacementAndMessage() {
            val expectedErrorMessage = ApplicationProvider.getApplicationContext<Context>()
                .getString(R.string.stripe_something_went_wrong)
            val errorBounds = error
                .assertIsDisplayed()
                .assertTextEquals(expectedErrorMessage)
                .fetchSemanticsNode()
                .boundsInRoot
            val savedPaymentMethodBounds = composeRule
                .onNodeWithTag("${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_$savedPaymentMethodId")
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .boundsInRoot
            val newPaymentMethodBounds = composeRule
                .onNodeWithTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_$newPaymentMethodCode")
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .boundsInRoot
            val mandateBounds = composeRule
                .onNodeWithTag(EMBEDDED_MANDATE_TEXT_TEST_TAG)
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .boundsInRoot

            assertThat(errorBounds.left).isWithin(0.1f).of(
                with(composeRule.density) { 0.dp.toPx() }
            )
            assertThat(errorBounds.top).isGreaterThan(savedPaymentMethodBounds.bottom)
            assertThat(errorBounds.top).isGreaterThan(newPaymentMethodBounds.bottom)
            assertThat(errorBounds.top).isGreaterThan(mandateBounds.bottom)
        }

        fun clearError() {
            interactor.stateSource.value = interactor.stateSource.value.copy(
                selection = Selection.New("cashapp"),
            )
            interactor.selectionErrorSource.value = null
            composeRule.waitForIdle()
        }

        fun assertErrorCleared() {
            composeRule.onAllNodesWithTag(
                EMBEDDED_SAVED_PAYMENT_METHOD_SELECTION_ERROR_TEST_TAG,
                useUnmergedTree = true,
            ).assertCountEquals(0)
        }
    }
}
