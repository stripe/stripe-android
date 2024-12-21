package com.stripe.android.paymentsheet.verticalmode

import android.os.Build
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.ViewActionRecorder
import com.stripe.android.paymentsheet.ui.TEST_TAG_DEFAULT_PAYMENT_METHOD_LABEL
import com.stripe.android.ui.core.elements.TEST_TAG_DIALOG_CONFIRM_BUTTON
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ManageOneSavedPaymentMethodUITest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun savedPaymentMethod_isDisplayed() {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        runScenario(paymentMethod, shouldShowDefaultBadge = false) {
            composeRule.onNodeWithTag(
                TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON + "_${paymentMethod.id}"
            ).assertExists()

            composeRule.onNodeWithTag(
                TEST_TAG_DEFAULT_PAYMENT_METHOD_LABEL, true
            ).assertDoesNotExist()
        }
    }

    @Test
    fun defaultSavedPaymentMethod_isDisplayed() {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        runScenario(paymentMethod, shouldShowDefaultBadge = true) {
            composeRule.onNodeWithTag(
                TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON + "_${paymentMethod.id}"
            ).assertExists()

            composeRule.onNodeWithTag(
                TEST_TAG_DEFAULT_PAYMENT_METHOD_LABEL, true
            ).assertExists()
        }
    }

    @Test
    fun clickingDeleteIcon_callsDeletePaymentMethod() {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        runScenario(paymentMethod, shouldShowDefaultBadge = false) {
            assertThat(viewActionRecorder.viewActions).isEmpty()

            composeRule.onNodeWithTag(
                TEST_TAG_MANAGE_SCREEN_DELETE_ICON + "_${paymentMethod.id}",
                useUnmergedTree = true,
            ).performClick()
            composeRule.onNodeWithTag(TEST_TAG_DIALOG_CONFIRM_BUTTON).performClick()

            viewActionRecorder.consume(
                ManageOneSavedPaymentMethodInteractor.ViewAction.DeletePaymentMethod
            )
            assertThat(viewActionRecorder.viewActions).isEmpty()
        }
    }

    private fun runScenario(
        paymentMethod: PaymentMethod,
        shouldShowDefaultBadge: Boolean,
        block: Scenario.() -> Unit,
    ) {
        val viewActionRecorder = ViewActionRecorder<ManageOneSavedPaymentMethodInteractor.ViewAction>()

        val manageScreenInteractor = FakeManageOneSavedPaymentMethodInteractor(
            paymentMethod = paymentMethod,
            viewActionRecorder = viewActionRecorder,
            shouldShowDefaultBadge = shouldShowDefaultBadge
        )

        composeRule.setContent {
            ManageOneSavedPaymentMethodUI(interactor = manageScreenInteractor)
        }

        Scenario(viewActionRecorder).apply(block)
    }

    private data class Scenario(
        val viewActionRecorder: ViewActionRecorder<ManageOneSavedPaymentMethodInteractor.ViewAction>,
    )
}
