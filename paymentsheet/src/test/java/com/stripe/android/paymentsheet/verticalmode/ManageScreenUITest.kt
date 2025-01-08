package com.stripe.android.paymentsheet.verticalmode

import android.os.Build
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescriptionExactly
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.toDisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.ViewActionRecorder
import com.stripe.android.paymentsheet.ui.TEST_TAG_DEFAULT_PAYMENT_METHOD_LABEL
import com.stripe.android.testing.PaymentMethodFactory
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
            currentSelection = null,
            isEditing = false,
            canEdit = true,
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
    fun savedPaymentMethod_hasCorrectContentDescription() {
        val savedCard = PaymentMethodFactory.card(last4 = "4242", addCbcNetworks = false)
        runScenario(
            initialState = ManageScreenInteractor.State(
                paymentMethods = listOf(
                    savedCard.toDisplayableSavedPaymentMethod()
                ),
                currentSelection = null,
                isEditing = false,
                canEdit = true,
            )
        ) {
            composeRule.onNodeWithTag(
                "${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_${savedCard.id}"
            ).assert(
                hasContentDescriptionExactly("Visa ending in 4 2 4 2 ")
            )
        }
    }

    @Test
    fun allSavedPaymentMethodsAreShownWithDefault() = runScenario(
        initialState = ManageScreenInteractor.State(
            paymentMethods = displayableSavedPaymentMethodsWithDefault,
            currentSelection = null,
            isEditing = false,
            canEdit = true,
        )
    ) {
        assertThat(
            composeRule.onNodeWithTag(TEST_TAG_MANAGE_SCREEN_SAVED_PMS_LIST).onChildren().fetchSemanticsNodes().size
        ).isEqualTo(displayableSavedPaymentMethodsWithDefault.size)

        for (savedPaymentMethod in displayableSavedPaymentMethodsWithDefault) {
            composeRule.onNodeWithTag(
                "${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_${savedPaymentMethod.paymentMethod.id}"
            ).assertExists()
        }

        // Only 1 default payment method label
        composeRule.onAllNodesWithTag(
            TEST_TAG_DEFAULT_PAYMENT_METHOD_LABEL,
            useUnmergedTree = true,
        ).assertCountEquals(1)
    }

    @Test
    fun paymentMethod_correctlyShowsAsDefault() {
        runScenario(
            initialState = ManageScreenInteractor.State(
                paymentMethods = listOf(
                    PaymentMethodFixtures.defaultDisplayableCard()
                ),
                currentSelection = null,
                isEditing = true,
                canEdit = true,
            )
        ) {
            composeRule.onNodeWithTag(
                TEST_TAG_DEFAULT_PAYMENT_METHOD_LABEL,
                useUnmergedTree = true,
            ).assertExists()
        }
    }

    @Test
    fun paymentMethod_correctlyShowsAsNotDefault_whenNotDefault() {
        runScenario(
            initialState = ManageScreenInteractor.State(
                paymentMethods = listOf(
                    PaymentMethodFixtures.displayableCard()
                ),
                currentSelection = null,
                isEditing = true,
                canEdit = true,
            )
        ) {
            composeRule.onNodeWithTag(
                TEST_TAG_DEFAULT_PAYMENT_METHOD_LABEL,
                useUnmergedTree = true,
            ).assertDoesNotExist()
        }
    }

    @Test
    fun allSavedPaymentMethodsAreShown_inEditMode() = runScenario(
        initialState = ManageScreenInteractor.State(
            paymentMethods = displayableSavedPaymentMethods,
            currentSelection = null,
            isEditing = true,
            canEdit = true,
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
                currentSelection = null,
                isEditing = false,
                canEdit = true,
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
    fun clickingPaymentMethod_inEditMode_sendsUpdatePaymentMethodViewAction() =
        runScenario(
            initialState = ManageScreenInteractor.State(
                paymentMethods = displayableSavedPaymentMethods,
                currentSelection = null,
                isEditing = true,
                canEdit = true,
            ),
        ) {
            assertThat(viewActionRecorder.viewActions).isEmpty()

            composeRule.onNodeWithTag(
                "${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_${displayableSavedPaymentMethods[0].paymentMethod.id}"
            ).performClick()

            viewActionRecorder.consume(
                ManageScreenInteractor.ViewAction.UpdatePaymentMethod(displayableSavedPaymentMethods[0])
            )
            assertThat(viewActionRecorder.viewActions).isEmpty()
        }

    @Test
    fun initiallySelectedPm_isSelectedInUi() = runScenario(
        initialState = ManageScreenInteractor.State(
            paymentMethods = displayableSavedPaymentMethods,
            currentSelection = displayableSavedPaymentMethods[1],
            isEditing = false,
            canEdit = true,
        )
    ) {
        composeRule.onNodeWithTag(
            "${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_${displayableSavedPaymentMethods[1].paymentMethod.id}"
        )
            // The selected node is the PaymentMethodRowButton which is a child of the SavedPaymentMethodRowButton
            .assertIsSelected()
    }

    @Test
    fun correctIconsAreShown_inEditMode() = runScenario(
        initialState = ManageScreenInteractor.State(
            paymentMethods = displayableSavedPaymentMethods,
            currentSelection = null,
            isEditing = true,
            canEdit = true,
        ),
    ) {
        getChevronIcon(displayableSavedPaymentMethods[0]).assertExists()
        getChevronIcon(displayableSavedPaymentMethods[1]).assertExists()
        getChevronIcon(displayableSavedPaymentMethods[2]).assertExists()
    }

    private fun getChevronIcon(paymentMethod: DisplayableSavedPaymentMethod): SemanticsNodeInteraction {
        return composeRule.onNodeWithTag(
            "${TEST_TAG_MANAGE_SCREEN_CHEVRON_ICON}_${paymentMethod.paymentMethod.id}",
            useUnmergedTree = true,
        )
    }

    private val displayableSavedPaymentMethods =
        PaymentMethodFixtures.createCards(2)
            .plus(PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD)
            .map {
                it.toDisplayableSavedPaymentMethod(shouldShowDefaultBadge = false)
            }

    private val displayableSavedPaymentMethodsWithDefault =
        PaymentMethodFixtures.createCards(3)
            .plus(PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD)
            .mapIndexed { idx, it ->
                if (idx == 0) {
                    it.toDisplayableSavedPaymentMethod(shouldShowDefaultBadge = true)
                } else {
                    it.toDisplayableSavedPaymentMethod(shouldShowDefaultBadge = false)
                }
            }

    private fun runScenario(
        initialState: ManageScreenInteractor.State,
        block: Scenario.() -> Unit
    ) {
        val viewActionRecorder = ViewActionRecorder<ManageScreenInteractor.ViewAction>()

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
        val viewActionRecorder: ViewActionRecorder<ManageScreenInteractor.ViewAction>,
    )
}
