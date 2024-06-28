package com.stripe.android.paymentsheet.viewmodels

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.elements.CvcConfig
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrimaryButtonUiStateMapperTest {

    @Test
    fun `Chooses custom button over default one`() = runTest {
        val usBankButton = PrimaryButton.UIState(
            label = resolvableString("US Bank Account FTW"),
            onClick = {},
            enabled = false,
            lockVisible = true,
        )

        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = stateFlowOf(PaymentSheetScreen.SelectSavedPaymentMethods()),
            buttonsEnabledFlow = stateFlowOf(true),
            amountFlow = stateFlowOf(Amount(value = 1234, currencyCode = "usd")),
            selectionFlow = stateFlowOf(null),
            customPrimaryButtonUiStateFlow = stateFlowOf(usBankButton),
            cvcFlow = stateFlowOf(true)
        )

        val result = mapper.forCompleteFlow()

        result.test {
            assertThat(awaitItem()).isEqualTo(usBankButton)
        }
    }

    @Test
    fun `Enables button if selection is not null`() = runTest {
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = stateFlowOf(PaymentSheetScreen.SelectSavedPaymentMethods()),
            buttonsEnabledFlow = stateFlowOf(true),
            amountFlow = stateFlowOf(Amount(value = 1234, currencyCode = "usd")),
            selectionFlow = stateFlowOf(cardSelection()),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(true)
        )

        val result = mapper.forCompleteFlow()

        result.test {
            assertThat(awaitItem()?.enabled).isTrue()
        }
    }

    @Test
    fun `Disables button if selection is null`() = runTest {
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = stateFlowOf(PaymentSheetScreen.SelectSavedPaymentMethods()),
            buttonsEnabledFlow = stateFlowOf(true),
            amountFlow = stateFlowOf(Amount(value = 1234, currencyCode = "usd")),
            selectionFlow = stateFlowOf(null),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(true)
        )

        val result = mapper.forCompleteFlow()

        result.test {
            assertThat(awaitItem()?.enabled).isFalse()
        }
    }

    @Test
    fun `Disables button if editing or processing, even if selection is not null`() = runTest {
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = stateFlowOf(PaymentSheetScreen.SelectSavedPaymentMethods()),
            buttonsEnabledFlow = stateFlowOf(false),
            amountFlow = stateFlowOf(Amount(value = 1234, currencyCode = "usd")),
            selectionFlow = stateFlowOf(cardSelection()),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(true)
        )

        val result = mapper.forCompleteFlow()

        result.test {
            assertThat(awaitItem()?.enabled).isFalse()
        }
    }

    @Test
    fun `Hides button in complete flow if on a screen that isn't supposed to show it`() = runTest {
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = stateFlowOf(PaymentSheetScreen.Loading),
            buttonsEnabledFlow = stateFlowOf(false),
            amountFlow = stateFlowOf(Amount(value = 1234, currencyCode = "usd")),
            selectionFlow = stateFlowOf(cardSelection()),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(true)
        )

        val result = mapper.forCompleteFlow()

        result.test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `Hides button in custom flow if on a screen that isn't supposed to show it`() = runTest {
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = stateFlowOf(PaymentSheetScreen.SelectSavedPaymentMethods()),
            buttonsEnabledFlow = stateFlowOf(false),
            selectionFlow = stateFlowOf(cardSelection()),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(true)
        )

        val result = mapper.forCustomFlow()

        result.test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `Shows button in custom flow if selected payment method requires confirmation`() = runTest {
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = stateFlowOf(PaymentSheetScreen.SelectSavedPaymentMethods()),
            buttonsEnabledFlow = stateFlowOf(false),
            selectionFlow = stateFlowOf(usBankAccountSelection()),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(true)
        )

        val result = mapper.forCustomFlow()

        result.test {
            assertThat(awaitItem()).isNotNull()
        }
    }

    @Test
    fun `Shows lock icon correctly based on the flow type`() = runTest {
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = stateFlowOf(PaymentSheetScreen.SelectSavedPaymentMethods()),
            buttonsEnabledFlow = stateFlowOf(false),
            amountFlow = stateFlowOf(Amount(value = 1234, currencyCode = "usd")),
            selectionFlow = stateFlowOf(usBankAccountSelection()),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(true)
        )

        val resultWithLock = mapper.forCompleteFlow()
        val resultWithoutLock = mapper.forCustomFlow()

        resultWithLock.test {
            assertThat(awaitItem()?.lockVisible).isTrue()
        }

        resultWithoutLock.test {
            assertThat(awaitItem()?.lockVisible).isFalse()
        }
    }

    @Test
    fun `Disables button if cvc is required and input is incomplete`() = runTest {
        val cvcFlow = stateFlowOf(CvcController(CvcConfig(), stateFlowOf(CardBrand.Unknown)))
        val screen = stateFlowOf(
            PaymentSheetScreen.SelectSavedPaymentMethods(
                PaymentSheetScreen.SelectSavedPaymentMethods.CvcRecollectionState.Required(cvcFlow)
            )
        )
        val selection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.createCard()
        )
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = screen,
            buttonsEnabledFlow = stateFlowOf(true),
            selectionFlow = stateFlowOf(selection),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(false)
        )

        val result = mapper.forCompleteFlow()

        result.test {
            assertThat(awaitItem()?.enabled).isFalse()
        }
    }

    @Test
    fun `Enables button if cvc is required and input is complete`() = runTest {
        val cvcFlow = stateFlowOf(CvcController(CvcConfig(), stateFlowOf(CardBrand.Unknown)))
        val screen = stateFlowOf(
            PaymentSheetScreen.SelectSavedPaymentMethods(
                PaymentSheetScreen.SelectSavedPaymentMethods.CvcRecollectionState.Required(cvcFlow)
            )
        )
        val selection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.createCard()
        )
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = screen,
            buttonsEnabledFlow = stateFlowOf(true),
            selectionFlow = stateFlowOf(selection),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(true)
        )

        val result = mapper.forCompleteFlow()

        result.test {
            assertThat(awaitItem()?.enabled).isTrue()
        }
    }

    @Test
    fun `Enables button if cvc is not required and input is incomplete`() = runTest {
        val screen = stateFlowOf(PaymentSheetScreen.SelectSavedPaymentMethods())
        val selection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.createCard()
        )
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = screen,
            buttonsEnabledFlow = stateFlowOf(true),
            selectionFlow = stateFlowOf(selection),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(false)
        )

        val result = mapper.forCompleteFlow()

        result.test {
            assertThat(awaitItem()?.enabled).isTrue()
        }
    }

    @Test
    fun `Enables button if cvc is required and selection is not saved card`() = runTest {
        val cvcFlow = stateFlowOf(CvcController(CvcConfig(), stateFlowOf(CardBrand.Unknown)))
        val screen = stateFlowOf(
            PaymentSheetScreen.SelectSavedPaymentMethods(
                PaymentSheetScreen.SelectSavedPaymentMethods.CvcRecollectionState.Required(cvcFlow)
            )
        )
        val selection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.PAYPAL_PAYMENT_METHOD
        )
        val mapper = createMapper(
            isProcessingPayment = true,
            currentScreenFlow = screen,
            buttonsEnabledFlow = stateFlowOf(true),
            selectionFlow = stateFlowOf(selection),
            customPrimaryButtonUiStateFlow = stateFlowOf(null),
            cvcFlow = stateFlowOf(false)
        )

        val result = mapper.forCompleteFlow()

        result.test {
            assertThat(awaitItem()?.enabled).isTrue()
        }
    }

    private fun createMapper(
        isProcessingPayment: Boolean,
        config: PaymentSheet.Configuration = PaymentSheet.Configuration("Some Name"),
        currentScreenFlow: StateFlow<PaymentSheetScreen>,
        buttonsEnabledFlow: StateFlow<Boolean>,
        amountFlow: StateFlow<Amount?> = stateFlowOf(null),
        selectionFlow: StateFlow<PaymentSelection?>,
        customPrimaryButtonUiStateFlow: StateFlow<PrimaryButton.UIState?>,
        cvcFlow: StateFlow<Boolean>,
    ): PrimaryButtonUiStateMapper {
        return PrimaryButtonUiStateMapper(
            config = config,
            isProcessingPayment = isProcessingPayment,
            currentScreenFlow = currentScreenFlow,
            buttonsEnabledFlow = buttonsEnabledFlow,
            amountFlow = amountFlow,
            selectionFlow = selectionFlow,
            customPrimaryButtonUiStateFlow = customPrimaryButtonUiStateFlow,
            cvcCompleteFlow = cvcFlow,
            onClick = {},
        )
    }

    private fun cardSelection(): PaymentSelection {
        return PaymentSelection.New.Card(
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            brand = CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
        )
    }

    private fun usBankAccountSelection(): PaymentSelection {
        return PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.US_BANK_ACCOUNT,
        )
    }
}
