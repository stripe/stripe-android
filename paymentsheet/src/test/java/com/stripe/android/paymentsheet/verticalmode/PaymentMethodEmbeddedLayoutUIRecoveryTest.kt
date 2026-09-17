package com.stripe.android.paymentsheet.verticalmode

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.ViewActionRecorder
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor.Selection
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
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
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `selection failure is row-aligned, list-owned, and clears after a new selection`() {
        val savedPaymentMethod = PaymentMethodFixtures.displayableCard()
        val interactor = FakePaymentMethodVerticalLayoutInteractor(
            initialState = PaymentMethodVerticalLayoutInteractor.State(
                displayablePaymentMethods = emptyList(),
                isProcessing = false,
                selection = Selection.Saved,
                displayedSavedPaymentMethod = savedPaymentMethod,
                availableSavedPaymentMethodAction = SavedPaymentMethodAction.NONE,
                mandate = "Mandate".resolvableString,
                linkBrand = LinkBrand.Link,
                pendingSavedPaymentMethodId = null,
                selectionError = IllegalStateException("Unable to update the Checkout Session."),
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

        val error = composeRule.onNodeWithTag(EMBEDDED_SAVED_PAYMENT_METHOD_SELECTION_ERROR_TEST_TAG)
        val errorPosition = error.assertIsDisplayed().fetchSemanticsNode().positionInRoot
        val savedPaymentMethodPosition = composeRule
            .onNodeWithTag("${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_${savedPaymentMethod.paymentMethod.id}")
            .fetchSemanticsNode()
            .positionInRoot
        val mandatePosition = composeRule
            .onNodeWithTag(EMBEDDED_MANDATE_TEXT_TEST_TAG)
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .positionInRoot

        assertThat(errorPosition.x).isWithin(0.1f).of(
            with(composeRule.density) { 12.dp.toPx() }
        )
        assertThat(errorPosition.y).isGreaterThan(savedPaymentMethodPosition.y)
        assertThat(mandatePosition.y).isGreaterThan(errorPosition.y)

        interactor.stateSource.value = interactor.stateSource.value.copy(
            selection = Selection.New("cashapp"),
            selectionError = null,
        )
        composeRule.waitForIdle()

        error.assertDoesNotExist()
    }
}
