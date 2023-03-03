package com.stripe.android.paymentsheet.viewmodels

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.ui.core.Amount
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrimaryButtonUiStateMapperTest {

    @Test
    fun `Chooses custom button over default one`() = runTest {
        val mapper = createMapper(isProcessingPayment = true)

        val usBankButton = PrimaryButton.UIState(
            label = "US Bank Account FTW",
            onClick = {},
            enabled = false,
            lockVisible = true,
        )

        val result = mapper.forCompleteFlow(
            currentScreenFlow = flowOf(PaymentSheetScreen.SelectSavedPaymentMethods),
            buttonsEnabledFlow = flowOf(true),
            amountFlow = flowOf(Amount(value = 1234, currencyCode = "usd")),
            selectionFlow = flowOf(null),
            customPrimaryButtonUiStateFlow = flowOf(usBankButton),
        )

        result.test {
            assertThat(awaitItem()).isEqualTo(usBankButton)
            awaitComplete()
        }
    }

    @Test
    fun `Enables button if selection is not null`() = runTest {
        val mapper = createMapper(isProcessingPayment = true)

        val result = mapper.forCompleteFlow(
            currentScreenFlow = flowOf(PaymentSheetScreen.SelectSavedPaymentMethods),
            buttonsEnabledFlow = flowOf(true),
            amountFlow = flowOf(Amount(value = 1234, currencyCode = "usd")),
            selectionFlow = flowOf(cardSelection()),
            customPrimaryButtonUiStateFlow = flowOf(null),
        )

        result.test {
            assertThat(awaitItem()?.enabled).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `Disables button if selection is null`() = runTest {
        val mapper = createMapper(isProcessingPayment = true)

        val result = mapper.forCompleteFlow(
            currentScreenFlow = flowOf(PaymentSheetScreen.SelectSavedPaymentMethods),
            buttonsEnabledFlow = flowOf(true),
            amountFlow = flowOf(Amount(value = 1234, currencyCode = "usd")),
            selectionFlow = flowOf(null),
            customPrimaryButtonUiStateFlow = flowOf(null),
        )

        result.test {
            assertThat(awaitItem()?.enabled).isFalse()
            awaitComplete()
        }
    }

    @Test
    fun `Disables button if editing or processing, even if selection is not null`() = runTest {
        val mapper = createMapper(isProcessingPayment = true)

        val result = mapper.forCompleteFlow(
            currentScreenFlow = flowOf(PaymentSheetScreen.SelectSavedPaymentMethods),
            buttonsEnabledFlow = flowOf(false),
            amountFlow = flowOf(Amount(value = 1234, currencyCode = "usd")),
            selectionFlow = flowOf(cardSelection()),
            customPrimaryButtonUiStateFlow = flowOf(null),
        )

        result.test {
            assertThat(awaitItem()?.enabled).isFalse()
            awaitComplete()
        }
    }

    @Test
    fun `Hides button in complete flow if on a screen that isn't supposed to show it`() = runTest {
        val mapper = createMapper(isProcessingPayment = true)

        val result = mapper.forCompleteFlow(
            currentScreenFlow = flowOf(PaymentSheetScreen.Loading),
            buttonsEnabledFlow = flowOf(false),
            amountFlow = flowOf(Amount(value = 1234, currencyCode = "usd")),
            selectionFlow = flowOf(cardSelection()),
            customPrimaryButtonUiStateFlow = flowOf(null),
        )

        result.test {
            assertThat(awaitItem()).isNull()
            awaitComplete()
        }
    }

    @Test
    fun `Hides button in custom flow if on a screen that isn't supposed to show it`() = runTest {
        val mapper = createMapper(isProcessingPayment = true)

        val result = mapper.forCustomFlow(
            currentScreenFlow = flowOf(PaymentSheetScreen.SelectSavedPaymentMethods),
            buttonsEnabledFlow = flowOf(false),
            selectionFlow = flowOf(cardSelection()),
            customPrimaryButtonUiStateFlow = flowOf(null),
        )

        result.test {
            assertThat(awaitItem()).isNull()
            awaitComplete()
        }
    }

    @Test
    fun `Shows button in custom flow if selected payment method requires confirmation`() = runTest {
        val mapper = createMapper(isProcessingPayment = true)

        val result = mapper.forCustomFlow(
            currentScreenFlow = flowOf(PaymentSheetScreen.SelectSavedPaymentMethods),
            buttonsEnabledFlow = flowOf(false),
            selectionFlow = flowOf(usBankAccountSelection()),
            customPrimaryButtonUiStateFlow = flowOf(null),
        )

        result.test {
            assertThat(awaitItem()).isNotNull()
            awaitComplete()
        }
    }

    @Test
    fun `Shows lock icon correctly based on the flow type`() = runTest {
        val mapper = createMapper(isProcessingPayment = true)

        val resultWithLock = mapper.forCompleteFlow(
            currentScreenFlow = flowOf(PaymentSheetScreen.SelectSavedPaymentMethods),
            buttonsEnabledFlow = flowOf(false),
            amountFlow = flowOf(Amount(value = 1234, currencyCode = "usd")),
            selectionFlow = flowOf(usBankAccountSelection()),
            customPrimaryButtonUiStateFlow = flowOf(null),
        )

        val resultWithoutLock = mapper.forCustomFlow(
            currentScreenFlow = flowOf(PaymentSheetScreen.SelectSavedPaymentMethods),
            buttonsEnabledFlow = flowOf(false),
            selectionFlow = flowOf(usBankAccountSelection()),
            customPrimaryButtonUiStateFlow = flowOf(null),
        )

        resultWithLock.test {
            assertThat(awaitItem()?.lockVisible).isTrue()
            awaitComplete()
        }

        resultWithoutLock.test {
            assertThat(awaitItem()?.lockVisible).isFalse()
            awaitComplete()
        }
    }

    private fun createMapper(
        isProcessingPayment: Boolean,
        config: PaymentSheet.Configuration? = null,
    ): PrimaryButtonUiStateMapper {
        return PrimaryButtonUiStateMapper(
            context = ApplicationProvider.getApplicationContext(),
            config = config,
            isProcessingPayment = isProcessingPayment,
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
            isGooglePay = false,
        )
    }
}
