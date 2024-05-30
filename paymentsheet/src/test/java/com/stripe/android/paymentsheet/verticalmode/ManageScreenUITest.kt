package com.stripe.android.paymentsheet.verticalmode

import android.os.Build
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.FakeManageScreenInteractor
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ManageScreenUITest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun allSavedPaymentMethodsAreShown() = runScenario(
        initialState = ManageScreenInteractor.State(
            paymentMethods = displayableSavedPaymentMethods,
            currentSelection = null
        )
    ) {
        assertThat(
            composeRule.onNodeWithTag(TEST_TAG_MANAGE_SCREEN_SAVED_PMS_LIST).onChildren().fetchSemanticsNodes().size
        ).isEqualTo(displayableSavedPaymentMethods.size)

        for (savedPaymentMethod in displayableSavedPaymentMethods) {
            composeRule.onNodeWithTag(
                "${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_${savedPaymentMethod.paymentMethod.id}"
            ).assertExists()
        }
    }

    @Test
    fun clickingPaymentMethod_selectsPaymentMethod() =
        runScenario(
            initialState = ManageScreenInteractor.State(
                paymentMethods = displayableSavedPaymentMethods,
                currentSelection = null
            )
        ) {
            assertThat(viewActionRecorder.viewActions).isEmpty()

            composeRule.onNodeWithTag(
                "${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_${displayableSavedPaymentMethods[0].paymentMethod.id}"
            ).performClick()

            viewActionRecorder.consume(
                ManageScreenInteractor.ViewAction.SelectPaymentMethod(displayableSavedPaymentMethods[0])
            )
            assertThat(viewActionRecorder.viewActions).isEmpty()
        }

    @Test
    fun initiallySelectedPm_isSelectedInUi() = runScenario(
        initialState = ManageScreenInteractor.State(
            paymentMethods = displayableSavedPaymentMethods,
            currentSelection = displayableSavedPaymentMethods[1],
        )
    ) {
        composeRule.onNodeWithTag(
            "${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_${displayableSavedPaymentMethods[1].paymentMethod.id}"
        )
            // The selected node is the PaymentMethodRowButton which is a child of the SavedPaymentMethodRowButton
            .onChild()
            .assertIsSelected()
    }

    private val displayableSavedPaymentMethods = PaymentMethodFixtures.createCards(4).map {
        DisplayableSavedPaymentMethod(
            displayName = it.card!!.last4!!,
            paymentMethod = it,
            isCbcEligible = false
        )
    }

    private fun runScenario(
        initialState: ManageScreenInteractor.State,
        block: Scenario.() -> Unit
    ) {
        val viewActionRecorder = FakeManageScreenInteractor.ViewActionRecorder()

        val manageScreenInteractor = FakeManageScreenInteractor(
            initialState = initialState,
            viewActionRecorder = viewActionRecorder,
        )

        composeRule.setContent {
            ManageScreenUI(interactor = manageScreenInteractor)
        }

        Scenario(viewActionRecorder).apply(block)
    }

    private data class Scenario(
        val viewActionRecorder: FakeManageScreenInteractor.ViewActionRecorder,
    )
}
