package com.stripe.android.paymentsheet.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethodFixtures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class EditPaymentMethodViewModelTest {
    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `on init, view state should be initialized properly`() = runTest {
        val viewModel = createViewModel()

        viewModel.viewState.test {
            val state = awaitItem()

            assertThat(state).isEqualTo(
                EditPaymentViewState(
                    last4 = "4242",
                    canUpdate = false,
                    selectedBrand = CARTES_BANCAIRES_BRAND_CHOICE,
                    availableBrands = listOf(
                        VISA_BRAND_CHOICE,
                        CARTES_BANCAIRES_BRAND_CHOICE
                    )
                )
            )
        }
    }

    @Test
    fun `on selection changed, 'canUpdate' should be true & selected brand should be changed`() = runTest {
        val viewModel = createViewModel()

        viewModel.handleViewAction(
            EditPaymentMethodViewAction.OnBrandChoiceChanged(VISA_BRAND_CHOICE)
        )

        viewModel.viewState.test {
            val state = awaitItem()

            assertThat(state).isEqualTo(
                EditPaymentViewState(
                    last4 = "4242",
                    canUpdate = true,
                    selectedBrand = VISA_BRAND_CHOICE,
                    availableBrands = listOf(
                        VISA_BRAND_CHOICE,
                        CARTES_BANCAIRES_BRAND_CHOICE
                    )
                )
            )
        }
    }

    @Test
    fun `on selection changed & reverted, 'canUpdate' should be false`() = runTest {
        val viewModel = createViewModel()

        viewModel.handleViewAction(
            EditPaymentMethodViewAction.OnBrandChoiceChanged(VISA_BRAND_CHOICE)
        )

        viewModel.handleViewAction(
            EditPaymentMethodViewAction.OnBrandChoiceChanged(CARTES_BANCAIRES_BRAND_CHOICE)
        )

        viewModel.viewState.test {
            val state = awaitItem()

            assertThat(state).isEqualTo(
                EditPaymentViewState(
                    last4 = "4242",
                    canUpdate = false,
                    selectedBrand = CARTES_BANCAIRES_BRAND_CHOICE,
                    availableBrands = listOf(
                        VISA_BRAND_CHOICE,
                        CARTES_BANCAIRES_BRAND_CHOICE
                    )
                )
            )
        }
    }

    @Test
    fun `on remove pressed, should send effect to begin removal process`() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.handleViewAction(EditPaymentMethodViewAction.OnRemovePressed)

            val effect = awaitItem()

            assertThat(effect).isEqualTo(
                EditPaymentMethodViewEffect.OnRemoveRequested(CARD_WITH_NETWORKS_PAYMENT_METHOD)
            )
        }
    }

    @Test
    fun `on update pressed with selection change, should send effect to begin update process`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.handleViewAction(
                EditPaymentMethodViewAction.OnBrandChoiceChanged(VISA_BRAND_CHOICE)
            )

            viewModel.handleViewAction(EditPaymentMethodViewAction.OnUpdatePressed)

            val effect = awaitItem()

            assertThat(effect).isEqualTo(
                EditPaymentMethodViewEffect.OnUpdateRequested(
                    CARD_WITH_NETWORKS_PAYMENT_METHOD,
                    CardBrand.Visa
                )
            )
        }
    }

    private fun createViewModel(): EditPaymentMethodViewModel {
        return EditPaymentMethodViewModel(
            paymentMethod = CARD_WITH_NETWORKS_PAYMENT_METHOD
        )
    }

    private companion object {
        private val CARD_WITH_NETWORKS_PAYMENT_METHOD = PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD

        val CARTES_BANCAIRES_BRAND_CHOICE = EditPaymentViewState.CardBrandChoice(
            brand = CardBrand.CartesBancaires
        )

        private val VISA_BRAND_CHOICE = EditPaymentViewState.CardBrandChoice(
            brand = CardBrand.Visa
        )
    }
}
