package com.stripe.android.paymentsheet.ui

import android.os.Build
import android.os.Parcel
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.toDisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.ViewActionRecorder
import com.stripe.android.ui.core.elements.TEST_TAG_DIALOG_CONFIRM_BUTTON
import com.stripe.android.ui.core.elements.TEST_TAG_SIMPLE_DIALOG
import com.stripe.android.uicore.elements.DROPDOWN_MENU_CLICKABLE_TEST_TAG
import com.stripe.android.uicore.elements.TEST_TAG_DROP_DOWN_CHOICE
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class UpdatePaymentMethodUITest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun canRemoveIsFalse_removeButtonHidden() = runScenario(
        canRemove = false,
    ) {
        composeRule.onNodeWithTag(UPDATE_PM_REMOVE_BUTTON_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun canRemoveIsTrue_removeButtonHidden() = runScenario(
        canRemove = true,
    ) {
        composeRule.onNodeWithTag(UPDATE_PM_REMOVE_BUTTON_TEST_TAG).assertExists()
    }

    @Test
    fun clickingRemoveButton_displaysDialog_deletesOnConfirm() = runScenario(canRemove = true) {
        composeRule.onNodeWithTag(UPDATE_PM_REMOVE_BUTTON_TEST_TAG).assertExists()
        composeRule.onNodeWithTag(UPDATE_PM_REMOVE_BUTTON_TEST_TAG).performClick()

        composeRule.onNodeWithTag(TEST_TAG_DIALOG_CONFIRM_BUTTON).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_DIALOG_CONFIRM_BUTTON).performClick()

        viewActionRecorder.consume(
            UpdatePaymentMethodInteractor.ViewAction.RemovePaymentMethod
        )
        assertThat(viewActionRecorder.viewActions).isEmpty()
        composeRule.onNodeWithTag(TEST_TAG_SIMPLE_DIALOG).assertDoesNotExist()
    }

    @Test
    fun isModifiablePMIsFalse_saveButtonHidden() = runScenario(
        isModifiablePaymentMethod = false,
    ) {
        composeRule.onNodeWithTag(UPDATE_PM_SAVE_BUTTON_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun shouldShowSaveButton_saveButtonIsDisplayed() = runScenario(
        shouldShowSaveButton = true,
    ) {
        composeRule.onNodeWithTag(UPDATE_PM_SAVE_BUTTON_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun shouldNotShowSaveButton_saveButtonIsHidden() = runScenario(
        shouldShowSaveButton = false,
    ) {
        composeRule.onNodeWithTag(UPDATE_PM_SAVE_BUTTON_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun isSaveButtonEnabled_saveButtonIsEnabled() = runScenario(
        shouldShowSaveButton = true,
        isSaveButtonEnabled = true,
    ) {
        composeRule.onNodeWithTag(UPDATE_PM_SAVE_BUTTON_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(UPDATE_PM_SAVE_BUTTON_TEST_TAG).onChildren().assertAll(isEnabled())
    }

    @Test
    fun isSaveButtonEnabledIsFalse_saveButtonIsNotEnabled() = runScenario(
        shouldShowSaveButton = true,
        isSaveButtonEnabled = false,
    ) {
        composeRule.onNodeWithTag(UPDATE_PM_SAVE_BUTTON_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(UPDATE_PM_SAVE_BUTTON_TEST_TAG).onChildren().assertAll(isNotEnabled())
    }

    @Test
    fun stateHasError_errorMessageIsDisplayed() {
        val errorMessage = "Something went wrong"

        runScenario(errorMessage = errorMessage.resolvableString) {
            composeRule.onNodeWithTag(UPDATE_PM_ERROR_MESSAGE_TEST_TAG).assertTextEquals(errorMessage)
        }
    }

    @Test
    fun cardPaymentMethod_cardUIIsShown() {
        val card = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        runScenario(
            displayableSavedPaymentMethod = card.toDisplayableSavedPaymentMethod(),
        ) {
            composeRule.onNodeWithTag(UPDATE_PM_CARD_TEST_TAG).assertExists()
        }
    }

    @Test
    fun usBankAccountPaymentMethod_usBankAccountUIIsShown() {
        val usBankAccount = PaymentMethodFixtures.US_BANK_ACCOUNT

        runScenario(
            displayableSavedPaymentMethod = usBankAccount.toDisplayableSavedPaymentMethod(),
        ) {
            composeRule.onNodeWithTag(UPDATE_PM_US_BANK_ACCOUNT_TEST_TAG).assertExists()
        }
    }

    @Test
    fun sepaDebitPaymentMethod_sepaDebitUIIsShown() {
        val sepaDebit = PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD

        runScenario(
            displayableSavedPaymentMethod = sepaDebit.toDisplayableSavedPaymentMethod(),
        ) {
            composeRule.onNodeWithTag(UPDATE_PM_SEPA_DEBIT_TEST_TAG).assertExists()
        }
    }

    @Test
    fun cardPaymentMethod_cardDetailsCannotBeChangedTextShown() {
        runScenario(
            displayableSavedPaymentMethod = PaymentMethodFixtures.displayableCard()
        ) {
            composeRule.onNodeWithTag(UPDATE_PM_DETAILS_SUBTITLE_TEST_TAG).assertTextEquals(
                "Card details cannot be changed."
            )
        }
    }

    @Test
    fun cardPaymentMethod_cbcEligible_onlyCardBrandCanBeChangedTextShown() {
        runScenario(
            displayableSavedPaymentMethod = PaymentMethodFixtures
                .CARD_WITH_NETWORKS_PAYMENT_METHOD
                .toDisplayableSavedPaymentMethod()
        ) {
            composeRule.onNodeWithTag(UPDATE_PM_DETAILS_SUBTITLE_TEST_TAG).assertTextEquals(
                "Only card brand can be changed."
            )
        }
    }

    @Test
    fun cardPaymentMethod_cbcEligible_filteredBrands_cardDetailsCannotBeChanged() {
        val cardBrandFilter = object : CardBrandFilter {
            override fun isAccepted(cardBrand: CardBrand): Boolean {
                return cardBrand in listOf(CardBrand.CartesBancaires)
            }

            override fun describeContents(): Int {
                throw NotImplementedError()
            }

            override fun writeToParcel(p0: Parcel, p1: Int) {
                throw NotImplementedError()
            }
        }
        runScenario(
            displayableSavedPaymentMethod = PaymentMethodFixtures
                .CARD_WITH_NETWORKS_PAYMENT_METHOD
                .toDisplayableSavedPaymentMethod(),
            cardBrandFilter = cardBrandFilter,
            hasValidBrandChoices = false
        ) {
            composeRule.onNodeWithTag(UPDATE_PM_DETAILS_SUBTITLE_TEST_TAG).assertTextEquals(
                "Card details cannot be changed."
            )
        }
    }

    @Test
    fun sepaPaymentMethod_sepaDetailsCannotBeChangedTextShown() {
        runScenario(
            displayableSavedPaymentMethod = PaymentMethodFixtures
                .SEPA_DEBIT_PAYMENT_METHOD
                .toDisplayableSavedPaymentMethod()
        ) {
            composeRule.onNodeWithTag(UPDATE_PM_DETAILS_SUBTITLE_TEST_TAG).assertTextEquals(
                "SEPA debit details cannot be changed."
            )
        }
    }

    @Test
    fun bankAccountPaymentMethod_bankAccountDetailsCannotBeChangedTextShown() {
        runScenario(
            displayableSavedPaymentMethod = PaymentMethodFixtures.US_BANK_ACCOUNT.toDisplayableSavedPaymentMethod()
        ) {
            composeRule.onNodeWithTag(UPDATE_PM_DETAILS_SUBTITLE_TEST_TAG).assertTextEquals(
                "Bank account details cannot be changed."
            )
        }
    }

    @Test
    fun expiredCard_hidesDetailsCantBeChangedText() {
        runScenario(
            displayableSavedPaymentMethod = PaymentMethodFixtures
                .EXPIRED_CARD_PAYMENT_METHOD
                .toDisplayableSavedPaymentMethod(),
            isExpiredCard = true,
        ) {
            composeRule.onNodeWithTag(UPDATE_PM_DETAILS_SUBTITLE_TEST_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun bankAccount_cbcDropdownIsNotShown() {
        runScenario(
            displayableSavedPaymentMethod = PaymentMethodFixtures.US_BANK_ACCOUNT.toDisplayableSavedPaymentMethod()
        ) {
            composeRule.onNodeWithTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun `When should show set as default checkbox, checkbox is visible and enabled`() {
        runScenario(
            shouldShowSetAsDefaultCheckbox = true,
        ) {
            val setAsDefaultCheckbox = composeRule.onNodeWithTag(UPDATE_PM_SET_AS_DEFAULT_CHECKBOX_TEST_TAG)

            setAsDefaultCheckbox.assertExists()
        }
    }

    @Test
    fun `setAsDefaultCheckboxEnabled true -- set as default checkbox is enabled`() {
        runScenario(
            shouldShowSetAsDefaultCheckbox = true,
            setAsDefaultCheckboxEnabled = true,
        ) {
            val setAsDefaultCheckbox = composeRule.onNodeWithTag(UPDATE_PM_SET_AS_DEFAULT_CHECKBOX_TEST_TAG)

            setAsDefaultCheckbox.assertExists()
            setAsDefaultCheckbox.assertIsEnabled()
        }
    }

    @Test
    fun `setAsDefaultCheckboxEnabled false -- set as default checkbox is not enabled`() {
        runScenario(
            shouldShowSetAsDefaultCheckbox = true,
            setAsDefaultCheckboxEnabled = false,
        ) {
            val setAsDefaultCheckbox = composeRule.onNodeWithTag(UPDATE_PM_SET_AS_DEFAULT_CHECKBOX_TEST_TAG)

            setAsDefaultCheckbox.assertExists()
            setAsDefaultCheckbox.assertIsNotEnabled()
        }
    }

    @Test
    fun `Clicking set as default checkbox sends SetDefaultCheckboxChanged view action`() {
        val initialCheckedValue = false
        runScenario(
            shouldShowSetAsDefaultCheckbox = true,
            setAsDefaultCheckboxChecked = initialCheckedValue,
        ) {
            composeRule.onNodeWithTag(UPDATE_PM_SET_AS_DEFAULT_CHECKBOX_TEST_TAG).performClick()

            viewActionRecorder.consume(
                UpdatePaymentMethodInteractor.ViewAction.SetAsDefaultCheckboxChanged(
                    isChecked = !initialCheckedValue
                )
            )
            assertThat(viewActionRecorder.viewActions).isEmpty()
        }
    }

    private fun runScenario(
        displayableSavedPaymentMethod: DisplayableSavedPaymentMethod = PaymentMethodFixtures.displayableCard(),
        isExpiredCard: Boolean = false,
        errorMessage: ResolvableString? = null,
        canRemove: Boolean = true,
        isModifiablePaymentMethod: Boolean = true,
        hasValidBrandChoices: Boolean = true,
        setAsDefaultCheckboxChecked: Boolean = false,
        setAsDefaultCheckboxEnabled: Boolean = true,
        cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
        shouldShowSetAsDefaultCheckbox: Boolean = false,
        shouldShowSaveButton: Boolean = false,
        isSaveButtonEnabled: Boolean = false,
        testBlock: Scenario.() -> Unit,
    ) {
        val viewActionRecorder = ViewActionRecorder<UpdatePaymentMethodInteractor.ViewAction>()
        val interactor = FakeUpdatePaymentMethodInteractor(
            displayableSavedPaymentMethod = displayableSavedPaymentMethod,
            canRemove = canRemove,
            isExpiredCard = isExpiredCard,
            isModifiablePaymentMethod = isModifiablePaymentMethod,
            cardBrandFilter = cardBrandFilter,
            viewActionRecorder = viewActionRecorder,
            hasValidBrandChoices = hasValidBrandChoices,
            shouldShowSetAsDefaultCheckbox = shouldShowSetAsDefaultCheckbox,
            shouldShowSaveButton = shouldShowSaveButton,
            setAsDefaultCheckboxEnabled = setAsDefaultCheckboxEnabled,
            initialState = UpdatePaymentMethodInteractor.State(
                error = errorMessage,
                status = UpdatePaymentMethodInteractor.Status.Idle,
                setAsDefaultCheckboxChecked = setAsDefaultCheckboxChecked,
                isSaveButtonEnabled = isSaveButtonEnabled,
            ),
        )

        composeRule.setContent {
            UpdatePaymentMethodUI(interactor = interactor, modifier = Modifier)
        }

        Scenario(viewActionRecorder).apply(testBlock)
    }

    private data class Scenario(
        val viewActionRecorder: ViewActionRecorder<UpdatePaymentMethodInteractor.ViewAction>,
    )
}
