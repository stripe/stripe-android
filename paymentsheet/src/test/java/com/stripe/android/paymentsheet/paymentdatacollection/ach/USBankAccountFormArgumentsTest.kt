package com.stripe.android.paymentsheet.paymentdatacollection.ach

import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.PrimaryButtonIcon
import com.stripe.android.paymentsheet.verticalmode.BankFormInteractor
import com.stripe.android.paymentsheet.viewmodels.FakeBaseSheetViewModel
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class USBankAccountFormArgumentsTest {
    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Test
    fun `icon follows button UI state even when arguments are created while loading`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create()
        val viewModel = FakeBaseSheetViewModel.create(
            paymentMethodMetadata = metadata,
            initialScreen = PaymentSheetScreen.Loading,
            canGoBack = false,
        )
        val viewModelStore = ViewModelStore().apply { put("sheet", viewModel) }

        try {
            assertThat(viewModel.primaryButtonUiState.value).isNull()
            val arguments = USBankAccountFormArguments.create(
                viewModel = viewModel,
                paymentMethodMetadata = metadata,
                hostedSurface = "test_surface",
                selectedPaymentMethodCode = PaymentMethod.Type.USBankAccount.code,
                bankFormInteractor = BankFormInteractor.create(viewModel),
            )

            arguments.primaryButtonIcon.test {
                assertThat(awaitItem()).isEqualTo(PrimaryButtonIcon.None)

                val button = PrimaryButton.UIState(
                    label = "Continue".resolvableString,
                    onClick = {},
                    enabled = true,
                    icon = PrimaryButtonIcon.Lock,
                )
                viewModel.primaryButtonUiStateSource.value = button
                assertThat(awaitItem()).isEqualTo(PrimaryButtonIcon.Lock)

                viewModel.primaryButtonUiStateSource.value = button.copy(icon = PrimaryButtonIcon.None)
                assertThat(awaitItem()).isEqualTo(PrimaryButtonIcon.None)
            }
        } finally {
            viewModelStore.clear()
        }
    }
}
