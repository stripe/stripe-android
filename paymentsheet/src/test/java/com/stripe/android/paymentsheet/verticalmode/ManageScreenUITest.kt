package com.stripe.android.paymentsheet.verticalmode

import android.os.Build
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescriptionExactly
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.toDisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.ViewActionRecorder
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.ui.core.elements.TEST_TAG_DIALOG_CONFIRM_BUTTON
import com.stripe.android.ui.core.elements.TEST_TAG_DIALOG_DISMISS_BUTTON
import com.stripe.android.ui.core.elements.TEST_TAG_SIMPLE_DIALOG
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ManageScreenUITest {

    @get:Rule
    val featureFlagTestRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.useNewUpdateCardScreen,
        isEnabled = false
    )

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun allSavedPaymentMethodsAreShown() = runScenario(
        initialState = ManageScreenInteractor.State(
            paymentMethods = displayableSavedPaymentMethods,
            currentSelection = null,
            isEditing = false,
            canRemove = true,
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
                canRemove = true,
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
    fun allSavedPaymentMethodsAreShown_inEditMode() = runScenario(
        initialState = ManageScreenInteractor.State(
            paymentMethods = displayableSavedPaymentMethods,
            currentSelection = null,
            isEditing = true,
            canRemove = true,
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
                canRemove = true,
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
    fun clickingPaymentMethod_inEditMode_doesNothing() =
        runScenario(
            initialState = ManageScreenInteractor.State(
                paymentMethods = displayableSavedPaymentMethods,
                currentSelection = null,
                isEditing = true,
                canRemove = true,
                canEdit = true,
            )
        ) {
            assertThat(viewActionRecorder.viewActions).isEmpty()

            composeRule.onNodeWithTag(
                "${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_${displayableSavedPaymentMethods[0].paymentMethod.id}"
            ).performClick()

            assertThat(viewActionRecorder.viewActions).isEmpty()
        }

    @Test
    fun clickingPaymentMethod_inEditMode_useNewUpdateCardScreen_updatesCard() =
        runScenario(
            initialState = ManageScreenInteractor.State(
                paymentMethods = displayableSavedPaymentMethods,
                currentSelection = null,
                isEditing = true,
                canRemove = true,
                canEdit = true,
            ),
            enableUseNewUpdateScreen = true,
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
            canRemove = true,
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
            canRemove = true,
            canEdit = true,
        )
    ) {
        getDeleteIcon(displayableSavedPaymentMethods[0]).assertExists()
        getDeleteIcon(displayableSavedPaymentMethods[1]).assertExists()
        getDeleteIcon(displayableSavedPaymentMethods[2]).assertExists()

        getEditIcon(displayableSavedPaymentMethods[0]).assertDoesNotExist()
        getEditIcon(displayableSavedPaymentMethods[1]).assertDoesNotExist()
        getEditIcon(displayableSavedPaymentMethods[2]).assertExists()
    }

    @Test
    fun correctIconsAreShown_usesNewUpdateScreen_inEditMode() = runScenario(
        initialState = ManageScreenInteractor.State(
            paymentMethods = displayableSavedPaymentMethods,
            currentSelection = null,
            isEditing = true,
            canRemove = true,
            canEdit = true,
        ),
        enableUseNewUpdateScreen = true,
    ) {
        getChevronIcon(displayableSavedPaymentMethods[0]).assertExists()
        getChevronIcon(displayableSavedPaymentMethods[1]).assertExists()
        getChevronIcon(displayableSavedPaymentMethods[2]).assertExists()
    }

    @Test
    fun correctIconsAreShown_whenCanNotDelete() = runScenario(
        initialState = ManageScreenInteractor.State(
            paymentMethods = listOf(cbcEligibleSavedPaymentMethod),
            currentSelection = null,
            isEditing = true,
            canRemove = false,
            canEdit = true,
        )
    ) {
        getDeleteIcon(cbcEligibleSavedPaymentMethod).assertDoesNotExist()
        getEditIcon(cbcEligibleSavedPaymentMethod).assertExists()
    }

    @Test
    fun clickingDeleteIcon_displaysDialog_deletesOnConfirm() = runScenario(
        initialState = ManageScreenInteractor.State(
            paymentMethods = displayableSavedPaymentMethods,
            currentSelection = null,
            isEditing = true,
            canRemove = true,
            canEdit = true,
        )
    ) {
        getDeleteIcon(displayableSavedPaymentMethods[0]).assertExists()
        getDeleteIcon(displayableSavedPaymentMethods[0]).performClick()

        composeRule.onNodeWithTag(TEST_TAG_DIALOG_CONFIRM_BUTTON).performClick()

        viewActionRecorder.consume(
            ManageScreenInteractor.ViewAction.DeletePaymentMethod(displayableSavedPaymentMethods[0])
        )
        assertThat(viewActionRecorder.viewActions).isEmpty()
        composeRule.onNodeWithTag(TEST_TAG_SIMPLE_DIALOG).assertDoesNotExist()
    }

    @Test
    fun clickingDeleteIcon_displaysDialog_doesNothingOnDismiss() = runScenario(
        initialState = ManageScreenInteractor.State(
            paymentMethods = displayableSavedPaymentMethods,
            currentSelection = null,
            isEditing = true,
            canRemove = true,
            canEdit = true,
        )
    ) {
        getDeleteIcon(displayableSavedPaymentMethods[0]).assertExists()
        getDeleteIcon(displayableSavedPaymentMethods[0]).performClick()

        composeRule.onNodeWithTag(TEST_TAG_DIALOG_DISMISS_BUTTON).performClick()

        assertThat(viewActionRecorder.viewActions).isEmpty()
        composeRule.onNodeWithTag(TEST_TAG_SIMPLE_DIALOG).assertDoesNotExist()
    }

    @Test
    fun clickingEditDialog_editsPaymentMethod() = runScenario(
        initialState = ManageScreenInteractor.State(
            paymentMethods = displayableSavedPaymentMethods,
            currentSelection = null,
            isEditing = true,
            canRemove = true,
            canEdit = true,
        )
    ) {
        val editablePaymentMethod = displayableSavedPaymentMethods[2]
        getEditIcon(editablePaymentMethod).assertExists()
        getEditIcon(editablePaymentMethod).performClick()

        viewActionRecorder.consume(
            ManageScreenInteractor.ViewAction.EditPaymentMethod(editablePaymentMethod)
        )
        assertThat(viewActionRecorder.viewActions).isEmpty()
    }

    private fun getDeleteIcon(paymentMethod: DisplayableSavedPaymentMethod): SemanticsNodeInteraction {
        return composeRule.onNodeWithTag(
            "${TEST_TAG_MANAGE_SCREEN_DELETE_ICON}_${paymentMethod.paymentMethod.id}",
            useUnmergedTree = true,
        )
    }

    private fun getEditIcon(paymentMethod: DisplayableSavedPaymentMethod): SemanticsNodeInteraction {
        return composeRule.onNodeWithTag(
            "${TEST_TAG_MANAGE_SCREEN_EDIT_ICON}_${paymentMethod.paymentMethod.id}",
            useUnmergedTree = true,
        )
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
                DisplayableSavedPaymentMethod.create(
                    displayName = it.card!!.last4!!.resolvableString,
                    paymentMethod = it,
                    isCbcEligible = true
                )
            }

    private val cbcEligibleSavedPaymentMethod = displayableSavedPaymentMethods[2]

    private fun runScenario(
        initialState: ManageScreenInteractor.State,
        enableUseNewUpdateScreen: Boolean = false,
        block: Scenario.() -> Unit
    ) {
        featureFlagTestRule.setEnabled(enableUseNewUpdateScreen)
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
