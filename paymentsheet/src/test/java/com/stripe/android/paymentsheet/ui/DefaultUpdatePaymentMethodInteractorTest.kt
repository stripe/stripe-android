package com.stripe.android.paymentsheet.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.toDisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultUpdatePaymentMethodInteractorTest {

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
        runScenario(
            displayableSavedPaymentMethod = PaymentMethodFixtures
                .CARD_WITH_NETWORKS_PAYMENT_METHOD
                .toDisplayableSavedPaymentMethod()
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
    fun updatingCardBrand_updatesStateAsExpected() {
        var updatedPaymentMethod: PaymentMethod? = null
        var newCardBrand: CardBrand? = null
        val initialPaymentMethod = PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD

        fun updateExecutor(paymentMethod: PaymentMethod, brand: CardBrand): Result<PaymentMethod> {
            updatedPaymentMethod = paymentMethod
            newCardBrand = brand

            return Result.success(paymentMethod)
        }

        runScenario(
            displayableSavedPaymentMethod = initialPaymentMethod.toDisplayableSavedPaymentMethod(),
            onSavePaymentMethod = ::updateExecutor,
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
                assertThat(awaitItem().cardBrandHasBeenChanged).isTrue()
            }

            interactor.handleViewAction(UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed)

            assertThat(updatedPaymentMethod).isEqualTo(initialPaymentMethod)
            assertThat(newCardBrand).isEqualTo(expectedNewCardBrand)
            // We reset the cardBrandHasBeenChanged value when saving a new card brand, so that a user could change
            // their card brand back to the original value if they wanted to.
            interactor.state.test {
                assertThat(awaitItem().cardBrandHasBeenChanged).isFalse()
            }

            // Re-clicking on the already saved card brand doesn't update cardBrandHasBeenChanged incorectly.
            interactor.handleViewAction(
                UpdatePaymentMethodInteractor.ViewAction.BrandChoiceChanged(
                    cardBrandChoice = CardBrandChoice(brand = expectedNewCardBrand, enabled = true)
                )
            )
            interactor.state.test {
                assertThat(awaitItem().cardBrandHasBeenChanged).isFalse()
            }
        }
    }

    @Test
    fun saveButtonClick_failure_displaysError() {
        val updateException = IllegalStateException("Not allowed.")

        @Suppress("UnusedParameter")
        fun updateExecutor(paymentMethod: PaymentMethod, brand: CardBrand): Result<PaymentMethod> {
            return Result.failure(updateException)
        }

        runScenario(
            displayableSavedPaymentMethod = PaymentMethodFixtures
                .CARD_WITH_NETWORKS_PAYMENT_METHOD
                .toDisplayableSavedPaymentMethod(),
            onSavePaymentMethod = ::updateExecutor,
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
                // Card brand has been changed is still true -- the user could try again.
                assertThat(state.cardBrandHasBeenChanged).isTrue()
                assertThat(state.error).isEqualTo(updateException.stripeErrorMessage())
            }
        }
    }

    private val notImplemented: () -> Nothing = { throw AssertionError("Not implemented") }

    private fun runScenario(
        canRemove: Boolean = false,
        displayableSavedPaymentMethod: DisplayableSavedPaymentMethod = PaymentMethodFixtures.displayableCard(),
        onRemovePaymentMethod: (PaymentMethod) -> Throwable? = { notImplemented() },
        onSavePaymentMethod: (PaymentMethod, CardBrand) -> Result<PaymentMethod> = { _, _ -> notImplemented() },
        testBlock: suspend TestParams.() -> Unit
    ) {
        val interactor = DefaultUpdatePaymentMethodInteractor(
            isLiveMode = false,
            canRemove = canRemove,
            displayableSavedPaymentMethod = displayableSavedPaymentMethod,
            removeExecutor = onRemovePaymentMethod,
            updateExecutor = onSavePaymentMethod,
            workContext = UnconfinedTestDispatcher(),
            cardBrandFilter = DefaultCardBrandFilter,
            onBrandChoiceOptionsShown = {},
            onBrandChoiceOptionsDismissed = {},
        )

        TestParams(interactor).apply { runTest { testBlock() } }
    }

    private data class TestParams(
        val interactor: UpdatePaymentMethodInteractor,
    )
}
