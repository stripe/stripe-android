package com.stripe.android.paymentsheet.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.toDisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.CardUpdateParams
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.ui.DefaultUpdatePaymentMethodInteractor.Companion.setDefaultPaymentMethodErrorMessage
import com.stripe.android.paymentsheet.ui.DefaultUpdatePaymentMethodInteractor.Companion.updateCardBrandErrorMessage
import com.stripe.android.paymentsheet.ui.DefaultUpdatePaymentMethodInteractor.Companion.updatesFailedErrorMessage
import com.stripe.android.testing.FeatureFlagTestRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@Suppress("LargeClass")
class DefaultUpdatePaymentMethodInteractorTest {

    @get:Rule
    val editSavedCardPaymentMethodEnabledFeatureFlagTestRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.editSavedCardPaymentMethodEnabled,
        isEnabled = false
    )

    @Test
    fun removeViewAction_removesPmAndNavigatesBack() {
        val paymentMethod = PaymentMethodFixtures.displayableCard()

        var removedPm: PaymentMethod? = null
        fun onRemovePaymentMethod(paymentMethod: PaymentMethod): Throwable? {
            removedPm = paymentMethod
            return null
        }

        runScenario(
            canRemove = true,
            displayableSavedPaymentMethod = paymentMethod,
            onRemovePaymentMethod = ::onRemovePaymentMethod,
        ) {
            interactor.handleViewAction(UpdatePaymentMethodInteractor.ViewAction.RemovePaymentMethod)

            assertThat(removedPm).isEqualTo(paymentMethod.paymentMethod)
        }
    }

    @Test
    fun creatingInteractorInLiveMode_setsTopBarStateCorrectly() = runScenario(
        isLiveMode = true,
    ) {
        assertThat(interactor.topBarState.showTestModeLabel).isFalse()
    }

    @Test
    fun creatingInteractorInTestMode_setsTopBarStateCorrectly() = runScenario(
        isLiveMode = false,
    ) {
        assertThat(interactor.topBarState.showTestModeLabel).isTrue()
    }

    @Test
    fun removingPaymentMethodFails_errorMessageIsSet() {
        val expectedError = IllegalStateException("Example error")

        @Suppress("UnusedParameter")
        fun onRemovePaymentMethod(paymentMethod: PaymentMethod): Throwable? {
            return expectedError
        }

        runScenario(
            canRemove = true,
            displayableSavedPaymentMethod = PaymentMethodFixtures.displayableCard(),
            onRemovePaymentMethod = ::onRemovePaymentMethod,
        ) {
            interactor.handleViewAction(UpdatePaymentMethodInteractor.ViewAction.RemovePaymentMethod)

            interactor.state.test {
                assertThat(awaitItem().error).isEqualTo(expectedError.stripeErrorMessage())
            }
        }
    }

    @Test
    fun expiredCard_displaysExpiredCardError() {
        runScenario(
            displayableSavedPaymentMethod = PaymentMethodFixtures
                .EXPIRED_CARD_PAYMENT_METHOD
                .toDisplayableSavedPaymentMethod()
        ) {
            interactor.state.test {
                assertThat(awaitItem().error).isEqualTo(
                    UpdatePaymentMethodInteractor.expiredErrorMessage
                )
            }
        }
    }

    @Test
    fun expiredCard_isNotModifiablePM() {
        runScenario(
            displayableSavedPaymentMethod = PaymentMethodFixtures
                .EXPIRED_CARD_PAYMENT_METHOD
                .toDisplayableSavedPaymentMethod()
        ) {
            assertThat(interactor.isModifiablePaymentMethod).isFalse()
        }
    }

    @Test
    fun nonExpiredCard_hasNoInitialError() {
        runScenario(
            displayableSavedPaymentMethod = PaymentMethodFixtures.displayableCard()
        ) {
            interactor.state.test {
                assertThat(awaitItem().error).isNull()
            }
        }
    }

    @Test
    fun initialCardBrand_forCardWithNetworks_isCorrect() {
        runScenario(
            displayableSavedPaymentMethod = PaymentMethodFixtures
                .CARD_WITH_NETWORKS_PAYMENT_METHOD
                .toDisplayableSavedPaymentMethod()
        ) {
            interactor.state.test {
                assertThat(awaitItem().cardBrandChoice.brand).isEqualTo(CardBrand.CartesBancaires)
            }
        }
    }

    @Test
    fun onCardBrandChoiceChanged_updatesCardBrandInState() {
        var callbackBrand: CardBrand? = null
        runScenario(
            displayableSavedPaymentMethod = PaymentMethodFixtures
                .CARD_WITH_NETWORKS_PAYMENT_METHOD
                .toDisplayableSavedPaymentMethod(),
            onBrandChoiceSelected = {
                callbackBrand = it
            }
        ) {
            val initialCardBrand = CardBrand.CartesBancaires
            val updatedCardBrand = CardBrand.Visa

            interactor.state.test {
                assertThat(awaitItem().cardBrandChoice.brand).isEqualTo(initialCardBrand)
            }

            interactor.handleViewAction(
                UpdatePaymentMethodInteractor.ViewAction.BrandChoiceChanged(
                    cardBrandChoice = CardBrandChoice(brand = updatedCardBrand, enabled = true)
                )
            )

            interactor.state.test {
                assertThat(awaitItem().cardBrandChoice.brand).isEqualTo(updatedCardBrand)
            }
            assertThat(callbackBrand).isEqualTo(updatedCardBrand)
        }
    }

    @Test
    fun onCardBrandChoiceChanged_doesNotInvokeCallback_whenNewCardBrandIsSameAsOld() {
        var callbackBrand: CardBrand? = null
        runScenario(
            displayableSavedPaymentMethod = PaymentMethodFixtures
                .CARD_WITH_NETWORKS_PAYMENT_METHOD
                .toDisplayableSavedPaymentMethod(),
            onBrandChoiceSelected = {
                callbackBrand = it
            }
        ) {
            val initialCardBrand = CardBrand.CartesBancaires

            interactor.state.test {
                assertThat(awaitItem().cardBrandChoice.brand).isEqualTo(initialCardBrand)
            }

            interactor.handleViewAction(
                UpdatePaymentMethodInteractor.ViewAction.BrandChoiceChanged(
                    cardBrandChoice = CardBrandChoice(brand = initialCardBrand, enabled = true)
                )
            )

            interactor.state.test {
                assertThat(awaitItem().cardBrandChoice.brand).isEqualTo(initialCardBrand)
            }
            assertThat(callbackBrand).isNull()
        }
    }

    @Test
    fun cbcEligibleCard_isModifiablePaymentMethod() {
        runScenario(
            displayableSavedPaymentMethod = PaymentMethodFixtures
                .CARD_WITH_NETWORKS_PAYMENT_METHOD
                .toDisplayableSavedPaymentMethod()
        ) {
            assertThat(interactor.isModifiablePaymentMethod).isTrue()
        }
    }

    @Test
    fun updatingCardBrand_callsOnUpdateSuccess() {
        var updateSuccessCalled = false
        fun onUpdateSuccess() {
            updateSuccessCalled = true
        }

        runScenario(
            displayableSavedPaymentMethod =
            PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD.toDisplayableSavedPaymentMethod(),
            updatePaymentMethodExecutor = { paymentMethod, _ -> Result.success(paymentMethod) },
            onUpdateSuccess = ::onUpdateSuccess,
        ) {
            interactor.state.test {
                assertThat(awaitItem().cardBrandChoice.brand).isEqualTo(CardBrand.CartesBancaires)
            }

            val expectedNewCardBrand = CardBrand.Visa
            interactor.handleViewAction(
                UpdatePaymentMethodInteractor.ViewAction.BrandChoiceChanged(
                    cardBrandChoice = CardBrandChoice(brand = expectedNewCardBrand, enabled = true)
                )
            )
            interactor.state.test {
                assertThat(awaitItem().isSaveButtonEnabled).isTrue()
            }

            interactor.handleViewAction(UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed)
            assertThat(updateSuccessCalled).isTrue()
        }
    }

    @Test
    fun updatingCardBrand_updatesStateAsExpected() {
        var updatedPaymentMethod: PaymentMethod? = null
        var newCardBrand: CardBrand? = null
        val initialPaymentMethod = PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD

        fun updatePaymentMethodExecutor(
            paymentMethod: PaymentMethod,
            cardUpdateParams: CardUpdateParams
        ): Result<PaymentMethod> {
            updatedPaymentMethod = paymentMethod
            newCardBrand = cardUpdateParams.cardBrand

            return Result.success(paymentMethod)
        }

        runScenario(
            displayableSavedPaymentMethod = initialPaymentMethod.toDisplayableSavedPaymentMethod(),
            updatePaymentMethodExecutor = ::updatePaymentMethodExecutor,
            onUpdateSuccess = {},
        ) {
            interactor.state.test {
                assertThat(awaitItem().cardBrandChoice.brand).isEqualTo(CardBrand.CartesBancaires)
            }

            val expectedNewCardBrand = CardBrand.Visa
            interactor.handleViewAction(
                UpdatePaymentMethodInteractor.ViewAction.BrandChoiceChanged(
                    cardBrandChoice = CardBrandChoice(brand = expectedNewCardBrand, enabled = true)
                )
            )
            interactor.state.test {
                assertThat(awaitItem().isSaveButtonEnabled).isTrue()
            }

            interactor.handleViewAction(UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed)

            assertThat(updatedPaymentMethod).isEqualTo(initialPaymentMethod)
            assertThat(newCardBrand).isEqualTo(expectedNewCardBrand)
            // The user should be able to change their card brand back to the original value if they wanted to at this
            // point.
            interactor.state.test {
                assertThat(awaitItem().isSaveButtonEnabled).isFalse()
            }

            // Re-clicking on the already saved card brand doesn't update cardBrandHasBeenChanged incorectly.
            interactor.handleViewAction(
                UpdatePaymentMethodInteractor.ViewAction.BrandChoiceChanged(
                    cardBrandChoice = CardBrandChoice(brand = expectedNewCardBrand, enabled = true)
                )
            )
            interactor.state.test {
                assertThat(awaitItem().isSaveButtonEnabled).isFalse()
            }
        }
    }

    @Test
    fun updatingCardBrand_cardBrandHasNotChanged_doesNotAttemptUpdate() {
        val initialPaymentMethod = PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD
        var cardBrandUpdateCalled = false

        @Suppress("UnusedParameter")
        fun updatePaymentMethodExecutor(
            paymentMethod: PaymentMethod,
            cardUpdateParams: CardUpdateParams
        ): Result<PaymentMethod> {
            cardBrandUpdateCalled = true

            return Result.success(paymentMethod)
        }

        runScenario(
            displayableSavedPaymentMethod = initialPaymentMethod.toDisplayableSavedPaymentMethod(),
            updatePaymentMethodExecutor = ::updatePaymentMethodExecutor,
        ) {
            interactor.handleViewAction(UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed)

            assertThat(cardBrandUpdateCalled).isFalse()
        }
    }

    @Test
    fun saveButtonClick_failure_displaysError() {
        @Suppress("UnusedParameter")
        fun updatePaymentMethodExecutor(
            paymentMethod: PaymentMethod,
            cardUpdateParams: CardUpdateParams
        ): Result<PaymentMethod> {
            return Result.failure(IllegalStateException("Not allowed."))
        }

        runScenario(
            displayableSavedPaymentMethod = PaymentMethodFixtures
                .CARD_WITH_NETWORKS_PAYMENT_METHOD
                .toDisplayableSavedPaymentMethod(),
            updatePaymentMethodExecutor = ::updatePaymentMethodExecutor,
        ) {
            val originalCardBrand = CardBrand.CartesBancaires
            interactor.state.test {
                assertThat(awaitItem().cardBrandChoice.brand).isEqualTo(originalCardBrand)
            }

            interactor.handleViewAction(
                UpdatePaymentMethodInteractor.ViewAction.BrandChoiceChanged(
                    cardBrandChoice = CardBrandChoice(brand = CardBrand.Visa, enabled = true)
                )
            )
            interactor.handleViewAction(UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed)

            interactor.state.test {
                val state = awaitItem()
                // Save button is still enabled -- the user could try again.
                assertThat(state.isSaveButtonEnabled).isTrue()
                assertThat(state.error).isEqualTo(updateCardBrandErrorMessage)
            }
        }
    }

    @Test
    fun setAsDefaultCheckbox_hiddenForSepaPm() = runScenario(
        displayableSavedPaymentMethod =
        PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD.toDisplayableSavedPaymentMethod(),
        shouldShowSetAsDefaultCheckbox = true,
    ) {
        assertThat(interactor.shouldShowSetAsDefaultCheckbox).isFalse()
    }

    @Test
    fun setAsDefaultCheckbox_shownForCards() = runScenario(
        displayableSavedPaymentMethod = PaymentMethodFixtures.displayableCard(),
        shouldShowSetAsDefaultCheckbox = true,
    ) {
        assertThat(interactor.shouldShowSetAsDefaultCheckbox).isTrue()
    }

    @Test
    fun setAsDefaultCheckbox_shownForUsBankAccount() = runScenario(
        displayableSavedPaymentMethod = PaymentMethodFixtures.US_BANK_ACCOUNT.toDisplayableSavedPaymentMethod(),
        shouldShowSetAsDefaultCheckbox = true,
    ) {
        assertThat(interactor.shouldShowSetAsDefaultCheckbox).isTrue()
    }

    @Test
    fun isDefaultPaymentMethod_saveButtonHidden() = runScenario(
        displayableSavedPaymentMethod = PaymentMethodFixtures.displayableCard(),
        shouldShowSetAsDefaultCheckbox = true,
        isDefaultPaymentMethod = true,
    ) {
        assertThat(interactor.shouldShowSaveButton).isFalse()
    }

    @Test
    fun isDefaultPaymentMethod_cbcEligibleCard_saveButtonShown() = runScenario(
        displayableSavedPaymentMethod = PaymentMethodFixtures
            .CARD_WITH_NETWORKS_PAYMENT_METHOD
            .toDisplayableSavedPaymentMethod(),
        shouldShowSetAsDefaultCheckbox = true,
        isDefaultPaymentMethod = true,
    ) {
        assertThat(interactor.shouldShowSaveButton).isTrue()
    }

    @Test
    fun isDefaultPaymentMethod_cbcEligibleCard_saveButtonEnabledCorrectly() = runScenario(
        displayableSavedPaymentMethod = PaymentMethodFixtures
            .CARD_WITH_NETWORKS_PAYMENT_METHOD
            .toDisplayableSavedPaymentMethod(),
        shouldShowSetAsDefaultCheckbox = true,
        isDefaultPaymentMethod = true,
    ) {
        interactor.state.test {
            assertThat(awaitItem().isSaveButtonEnabled).isFalse()
        }

        interactor.handleViewAction(
            UpdatePaymentMethodInteractor.ViewAction.BrandChoiceChanged(
                cardBrandChoice = CardBrandChoice(brand = CardBrand.Visa, enabled = true)
            )
        )

        interactor.state.test {
            assertThat(awaitItem().isSaveButtonEnabled).isTrue()
        }
    }

    @Test
    fun isDefaultPaymentMethod_setAsDefaultCheckboxChecked() = runScenario(
        displayableSavedPaymentMethod = PaymentMethodFixtures.displayableCard(),
        shouldShowSetAsDefaultCheckbox = true,
        isDefaultPaymentMethod = true,
    ) {
        interactor.state.test {
            assertThat(awaitItem().setAsDefaultCheckboxChecked).isTrue()
        }
    }

    @Test
    fun isDefaultPaymentMethod_setAsDefaultCheckboxDisabled() = runScenario(
        displayableSavedPaymentMethod = PaymentMethodFixtures.displayableCard(),
        shouldShowSetAsDefaultCheckbox = true,
        isDefaultPaymentMethod = true,
    ) {
        assertThat(interactor.setAsDefaultCheckboxEnabled).isFalse()
    }

    @Test
    fun isNotDefaultPaymentMethod_setAsDefaultCheckboxEnabled() = runScenario(
        displayableSavedPaymentMethod = PaymentMethodFixtures.displayableCard(),
        shouldShowSetAsDefaultCheckbox = true,
        isDefaultPaymentMethod = false,
    ) {
        assertThat(interactor.setAsDefaultCheckboxEnabled).isTrue()
    }

    @Test
    fun setAsDefaultCheckboxChangedViewAction_updatesState() = runScenario(
        shouldShowSetAsDefaultCheckbox = true,
    ) {
        val expectedInitialCheckedValue = false
        val expectedUpdatedCheckedValue = true
        interactor.state.test {
            assertThat(awaitItem().setAsDefaultCheckboxChecked).isEqualTo(expectedInitialCheckedValue)
        }

        interactor.handleViewAction(
            UpdatePaymentMethodInteractor.ViewAction.SetAsDefaultCheckboxChanged(
                isChecked = expectedUpdatedCheckedValue
            )
        )

        interactor.state.test {
            val newState = awaitItem()

            assertThat(newState.setAsDefaultCheckboxChecked).isEqualTo(expectedUpdatedCheckedValue)
            assertThat(newState.isSaveButtonEnabled).isTrue()
        }
    }

    @Test
    fun setAsDefaultNotChanged_pressSave_doesNotCallOnUpdateSuccess() {
        var updateSuccessCalled = false
        fun onUpdateSuccess() {
            updateSuccessCalled = true
        }

        runScenario(
            shouldShowSetAsDefaultCheckbox = true,
            onSetDefaultPaymentMethod = { Result.success(Unit) },
            onUpdateSuccess = ::onUpdateSuccess,
        ) {
            interactor.handleViewAction(UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed)

            assertThat(updateSuccessCalled).isFalse()
        }
    }

    @Test
    fun setAsDefault_success_callsOnUpdateSuccess() {
        var updateSuccessCalled = false
        fun onUpdateSuccess() {
            updateSuccessCalled = true
        }

        runScenario(
            shouldShowSetAsDefaultCheckbox = true,
            onSetDefaultPaymentMethod = { Result.success(Unit) },
            onUpdateSuccess = ::onUpdateSuccess,
        ) {
            interactor.handleViewAction(
                UpdatePaymentMethodInteractor.ViewAction.SetAsDefaultCheckboxChanged(
                    isChecked = true
                )
            )

            interactor.handleViewAction(UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed)

            assertThat(updateSuccessCalled).isTrue()
        }
    }

    @Test
    fun setAsDefault_failure_displaysErrorMessage() = runScenario(
        shouldShowSetAsDefaultCheckbox = true,
        onSetDefaultPaymentMethod = { Result.failure(IllegalStateException("Fake error")) }
    ) {
        interactor.handleViewAction(
            UpdatePaymentMethodInteractor.ViewAction.SetAsDefaultCheckboxChanged(
                isChecked = true
            )
        )

        interactor.handleViewAction(UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed)

        interactor.state.test {
            assertThat(awaitItem().error).isEqualTo(setDefaultPaymentMethodErrorMessage)
        }
    }

    @Test
    fun updateCardBrandAndDefault_cardBrandUpdateFails_displaysError() {
        var updateSuccessCalled = false
        fun onUpdateSuccess() {
            updateSuccessCalled = true
        }

        val expectedError = IllegalStateException("Fake error")

        runScenario(
            displayableSavedPaymentMethod = PaymentMethodFixtures
                .CARD_WITH_NETWORKS_PAYMENT_METHOD
                .toDisplayableSavedPaymentMethod(),
            shouldShowSetAsDefaultCheckbox = true,
            onSetDefaultPaymentMethod = { Result.success(Unit) },
            updatePaymentMethodExecutor = { _, _ -> Result.failure(expectedError) },
            onUpdateSuccess = ::onUpdateSuccess,
        ) {
            updateCardBrandAndDefaultPaymentMethod(interactor)

            assertThat(updateSuccessCalled).isFalse()
            interactor.state.test {
                assertThat(awaitItem().error).isEqualTo(updateCardBrandErrorMessage)
            }
        }
    }

    @Test
    fun updateCardBrandAndDefault_setDefaultFails_displaysError() {
        var updateSuccessCalled = false
        fun onUpdateSuccess() {
            updateSuccessCalled = true
        }

        runScenario(
            displayableSavedPaymentMethod = PaymentMethodFixtures
                .CARD_WITH_NETWORKS_PAYMENT_METHOD
                .toDisplayableSavedPaymentMethod(),
            shouldShowSetAsDefaultCheckbox = true,
            onSetDefaultPaymentMethod = { Result.failure(IllegalStateException("Fake error")) },
            updatePaymentMethodExecutor = { paymentMethod, _ -> Result.success(paymentMethod) },
            onUpdateSuccess = ::onUpdateSuccess,
        ) {
            updateCardBrandAndDefaultPaymentMethod(interactor)

            assertThat(updateSuccessCalled).isFalse()
            interactor.state.test {
                assertThat(awaitItem().error).isEqualTo(setDefaultPaymentMethodErrorMessage)
            }
        }
    }

    @Test
    fun updateCardBrandAndDefault_bothFail_displaysError() {
        var updateSuccessCalled = false
        fun onUpdateSuccess() {
            updateSuccessCalled = true
        }

        runScenario(
            displayableSavedPaymentMethod = PaymentMethodFixtures
                .CARD_WITH_NETWORKS_PAYMENT_METHOD
                .toDisplayableSavedPaymentMethod(),
            shouldShowSetAsDefaultCheckbox = true,
            onSetDefaultPaymentMethod = { Result.failure(IllegalStateException("Fake error")) },
            updatePaymentMethodExecutor = { _, _ -> Result.failure(IllegalStateException("Fake error")) },
            onUpdateSuccess = ::onUpdateSuccess,
        ) {
            updateCardBrandAndDefaultPaymentMethod(interactor)

            assertThat(updateSuccessCalled).isFalse()
            interactor.state.test {
                assertThat(awaitItem().error).isEqualTo(updatesFailedErrorMessage)
            }
        }
    }

    @Test
    fun shouldShowSaveButton_whenCardIsModifiable() = runScenario(
        displayableSavedPaymentMethod = PaymentMethodFixtures
            .CARD_WITH_NETWORKS_PAYMENT_METHOD
            .toDisplayableSavedPaymentMethod(),
    ) {
        assertThat(interactor.shouldShowSaveButton).isTrue()
    }

    @Test
    fun shouldShowSaveButton_whenSetAsDefaultCheckboxShown() = runScenario(
        displayableSavedPaymentMethod = PaymentMethodFixtures.displayableCard(),
        shouldShowSetAsDefaultCheckbox = true,
    ) {
        assertThat(interactor.shouldShowSaveButton).isTrue()
    }

    @Test
    fun shouldShowSaveButton_whenCardIsModifiable_andSetAsDefaultCheckboxShown() = runScenario(
        displayableSavedPaymentMethod = PaymentMethodFixtures
            .CARD_WITH_NETWORKS_PAYMENT_METHOD
            .toDisplayableSavedPaymentMethod(),
        shouldShowSetAsDefaultCheckbox = true,
    ) {
        assertThat(interactor.shouldShowSaveButton).isTrue()
    }

    @Test
    fun shouldNotShowSaveButton_whenNothingIsChangeable() = runScenario(
        displayableSavedPaymentMethod = PaymentMethodFixtures.displayableCard(),
        shouldShowSetAsDefaultCheckbox = false,
    ) {
        assertThat(interactor.shouldShowSaveButton).isFalse()
    }

    @Test
    fun shouldNotAllowCardEdit_whenCardEditFeatureFlagIsDisabled() = runScenario(
        editSavedCardPaymentMethodEnabled = false
    ) {
        assertThat(interactor.allowCardEdit).isFalse()
    }

    @Test
    fun shouldAllowCardEdit_whenCardEditFeatureFlagIsEnabled() = runScenario(
        editSavedCardPaymentMethodEnabled = true
    ) {
        assertThat(interactor.allowCardEdit).isTrue()
    }

    private fun updateCardBrandAndDefaultPaymentMethod(interactor: UpdatePaymentMethodInteractor) {
        interactor.handleViewAction(
            UpdatePaymentMethodInteractor.ViewAction.SetAsDefaultCheckboxChanged(
                isChecked = true
            )
        )

        interactor.handleViewAction(
            UpdatePaymentMethodInteractor.ViewAction.BrandChoiceChanged(
                cardBrandChoice = CardBrandChoice(brand = CardBrand.Visa, enabled = true)
            )
        )

        interactor.handleViewAction(UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed)
    }

    private val notImplemented: () -> Nothing = { throw AssertionError("Not implemented") }

    private fun runScenario(
        isLiveMode: Boolean = false,
        canRemove: Boolean = false,
        displayableSavedPaymentMethod: DisplayableSavedPaymentMethod = PaymentMethodFixtures.displayableCard(),
        onRemovePaymentMethod: (PaymentMethod) -> Throwable? = { notImplemented() },
        updatePaymentMethodExecutor: (
            PaymentMethod,
            CardUpdateParams
        ) -> Result<PaymentMethod> = { _, _ -> notImplemented() },
        onSetDefaultPaymentMethod: (PaymentMethod) -> Result<Unit> = { _ -> notImplemented() },
        onUpdateSuccess: () -> Unit = { notImplemented() },
        shouldShowSetAsDefaultCheckbox: Boolean = false,
        isDefaultPaymentMethod: Boolean = false,
        editSavedCardPaymentMethodEnabled: Boolean = false,
        onBrandChoiceSelected: (CardBrand) -> Unit = {},
        testBlock: suspend TestParams.() -> Unit
    ) {
        editSavedCardPaymentMethodEnabledFeatureFlagTestRule.setEnabled(editSavedCardPaymentMethodEnabled)
        val interactor = DefaultUpdatePaymentMethodInteractor(
            isLiveMode = isLiveMode,
            canRemove = canRemove,
            displayableSavedPaymentMethod = displayableSavedPaymentMethod,
            removeExecutor = onRemovePaymentMethod,
            updatePaymentMethodExecutor = updatePaymentMethodExecutor,
            setDefaultPaymentMethodExecutor = onSetDefaultPaymentMethod,
            workContext = UnconfinedTestDispatcher(),
            cardBrandFilter = DefaultCardBrandFilter,
            onBrandChoiceSelected = onBrandChoiceSelected,
            shouldShowSetAsDefaultCheckbox = shouldShowSetAsDefaultCheckbox,
            isDefaultPaymentMethod = isDefaultPaymentMethod,
            onUpdateSuccess = onUpdateSuccess,
        )

        TestParams(interactor).apply { runTest { testBlock() } }
    }

    private data class TestParams(
        val interactor: UpdatePaymentMethodInteractor,
    )
}
