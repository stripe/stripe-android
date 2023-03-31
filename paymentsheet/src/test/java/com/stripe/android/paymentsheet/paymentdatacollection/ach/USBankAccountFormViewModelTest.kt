package com.stripe.android.paymentsheet.paymentdatacollection.ach

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResponse
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.ui.core.Amount
import com.stripe.android.uicore.address.AddressRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class USBankAccountFormViewModelTest {

    private val onConfirmStripeIntent: (ConfirmStripeIntentParams) -> Unit = mock()
    private val onUpdateSelectionAndFinish: (PaymentSelection) -> Unit = mock()

    private val defaultArgs = USBankAccountFormViewModel.Args(
        formArgs = FormArguments(
            paymentMethodCode = PaymentMethod.Type.USBankAccount.code,
            showCheckbox = false,
            showCheckboxControlledFields = false,
            merchantName = MERCHANT_NAME,
            amount = Amount(5099, "usd"),
            billingDetails = PaymentSheet.BillingDetails(
                name = CUSTOMER_NAME,
                email = CUSTOMER_EMAIL
            )
        ),
        isCompleteFlow = true,
        clientSecret = PaymentIntentClientSecret("pi_12345_secret_54321"),
        savedPaymentMethod = null,
        shippingDetails = null,
        onConfirmStripeIntent = onConfirmStripeIntent,
        onUpdateSelectionAndFinish = onUpdateSelectionAndFinish
    )

    private val stripeRepository = mock<StripeRepository>()
    private val collectBankAccountLauncher = mock<CollectBankAccountLauncher>()
    private val savedStateHandle = SavedStateHandle()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when email and name is valid then required fields are filled`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel()

            assertThat(viewModel.name.stateIn(viewModel.viewModelScope).value).isEqualTo(
                CUSTOMER_NAME
            )
            assertThat(viewModel.email.stateIn(viewModel.viewModelScope).value).isEqualTo(
                CUSTOMER_EMAIL
            )

            assertThat(viewModel.requiredFields.stateIn(viewModel.viewModelScope).value).isTrue()
        }

    @Test
    fun `when email and name is invalid then required fields are not filled`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel()

            viewModel.nameController.onRawValueChange("      ")
            viewModel.emailController.onRawValueChange(CUSTOMER_EMAIL)

            assertThat(viewModel.requiredFields.stateIn(viewModel.viewModelScope).value).isFalse()

            viewModel.nameController.onRawValueChange(CUSTOMER_NAME)
            viewModel.emailController.onRawValueChange(CUSTOMER_EMAIL)

            assertThat(viewModel.requiredFields.stateIn(viewModel.viewModelScope).value).isTrue()

            viewModel.nameController.onRawValueChange(CUSTOMER_NAME)
            viewModel.emailController.onRawValueChange("")

            assertThat(viewModel.requiredFields.stateIn(viewModel.viewModelScope).value).isFalse()
        }

    @Test
    fun `collect bank account is callable with initial screen state`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel()
            viewModel.collectBankAccountLauncher = collectBankAccountLauncher
            val currentScreenState =
                viewModel.currentScreenState.stateIn(viewModel.viewModelScope).value

            assertThat(
                currentScreenState
            ).isInstanceOf(
                USBankAccountFormScreenState.BillingDetailsCollection::class.java
            )

            viewModel.handlePrimaryButtonClick(
                currentScreenState as USBankAccountFormScreenState.BillingDetailsCollection
            )

            verify(collectBankAccountLauncher).presentWithPaymentIntent(any(), any(), any(), any())
        }

    @Test
    fun `when payment sheet, unverified bank account, then confirm intent callable`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel()

            viewModel.handleCollectBankAccountResult(
                mockUnverifiedBankAccount()
            )

            val currentScreenState =
                viewModel.currentScreenState.stateIn(viewModel.viewModelScope).value
            assertThat(currentScreenState)
                .isInstanceOf(USBankAccountFormScreenState.VerifyWithMicrodeposits::class.java)

            viewModel.handlePrimaryButtonClick(currentScreenState as USBankAccountFormScreenState.VerifyWithMicrodeposits)

            val newScreenState =
                viewModel.currentScreenState.stateIn(viewModel.viewModelScope).value
            assertThat(newScreenState)
                .isInstanceOf(USBankAccountFormScreenState.VerifyWithMicrodeposits::class.java)

            verify(onConfirmStripeIntent).invoke(any())
        }

    @Test
    fun `when payment sheet, verified bank account, then confirm intent callable`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel()

            viewModel.handleCollectBankAccountResult(
                mockVerifiedBankAccount()
            )

            val currentScreenState =
                viewModel.currentScreenState.stateIn(viewModel.viewModelScope).value
            assertThat(currentScreenState)
                .isInstanceOf(USBankAccountFormScreenState.MandateCollection::class.java)

            viewModel.handlePrimaryButtonClick(currentScreenState as USBankAccountFormScreenState.MandateCollection)

            val newScreenState =
                viewModel.currentScreenState.stateIn(viewModel.viewModelScope).value
            assertThat(newScreenState)
                .isInstanceOf(USBankAccountFormScreenState.MandateCollection::class.java)

            verify(onConfirmStripeIntent).invoke(any())
        }

    @Test
    fun `when payment options, unverified bank account, then finished`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(defaultArgs.copy(isCompleteFlow = false))
            val bankAccount = mockUnverifiedBankAccount()

            viewModel.handleCollectBankAccountResult(bankAccount)

            val currentScreenState =
                viewModel.currentScreenState.stateIn(viewModel.viewModelScope).value
            assertThat(currentScreenState)
                .isInstanceOf(USBankAccountFormScreenState.VerifyWithMicrodeposits::class.java)

            viewModel.handlePrimaryButtonClick(currentScreenState as USBankAccountFormScreenState.VerifyWithMicrodeposits)

            val newScreenState =
                viewModel.currentScreenState.stateIn(viewModel.viewModelScope).value
            assertThat(newScreenState)
                .isInstanceOf(USBankAccountFormScreenState.VerifyWithMicrodeposits::class.java)

            val session =
                (bankAccount as CollectBankAccountResult.Completed).response.financialConnectionsSession
            val expectedBankAccount = session.paymentAccount as BankAccount

            val argumentCaptor = argumentCaptor<PaymentSelection>()
            verify(onUpdateSelectionAndFinish).invoke(argumentCaptor.capture())

            val actualBankAccount = argumentCaptor.firstValue as PaymentSelection.New.USBankAccount
            assertThat(expectedBankAccount.last4).isEqualTo(actualBankAccount.last4)
        }

    @Test
    fun `when payment options, verified bank account, then finished`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(defaultArgs.copy(isCompleteFlow = false))
            val bankAccount = mockVerifiedBankAccount()

            viewModel.handleCollectBankAccountResult(bankAccount)

            val currentScreenState =
                viewModel.currentScreenState.stateIn(viewModel.viewModelScope).value
            assertThat(currentScreenState)
                .isInstanceOf(USBankAccountFormScreenState.MandateCollection::class.java)

            viewModel.handlePrimaryButtonClick(currentScreenState as USBankAccountFormScreenState.MandateCollection)

            val newScreenState =
                viewModel.currentScreenState.stateIn(viewModel.viewModelScope).value
            assertThat(newScreenState)
                .isInstanceOf(USBankAccountFormScreenState.MandateCollection::class.java)

            val session =
                (bankAccount as CollectBankAccountResult.Completed).response.financialConnectionsSession
            val expectedBankAccount = session.paymentAccount as FinancialConnectionsAccount

            val argumentCaptor = argumentCaptor<PaymentSelection>()
            verify(onUpdateSelectionAndFinish).invoke(argumentCaptor.capture())

            val actualBankAccount = argumentCaptor.firstValue as PaymentSelection.New.USBankAccount
            assertThat(expectedBankAccount.last4).isEqualTo(actualBankAccount.last4)
        }

    @Test
    fun `when reset, primary button launches bank account collection`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel()
            viewModel.collectBankAccountLauncher = collectBankAccountLauncher
            viewModel.reset()

            val currentScreenState =
                viewModel.currentScreenState.stateIn(viewModel.viewModelScope).value

            viewModel.handlePrimaryButtonClick(
                currentScreenState as USBankAccountFormScreenState.BillingDetailsCollection
            )

            verify(collectBankAccountLauncher).presentWithPaymentIntent(any(), any(), any(), any())
        }

    @Test
    fun `when reset, save for future usage should be true`() {
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel()
            viewModel.collectBankAccountLauncher = collectBankAccountLauncher

            viewModel.saveForFutureUseElement.controller.onValueChange(false)

            assertThat(
                viewModel.saveForFutureUseElement.controller.saveForFutureUse.stateIn(
                    viewModel.viewModelScope
                ).value
            ).isFalse()

            viewModel.reset()

            assertThat(
                viewModel.saveForFutureUseElement.controller.saveForFutureUse.stateIn(
                    viewModel.viewModelScope
                ).value
            ).isTrue()
        }
    }

    @Test
    fun `when saved payment method is USBankAccount SavedAccount is emitted`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(
                defaultArgs.copy(
                    savedPaymentMethod = PaymentSelection.New.USBankAccount(
                        labelResource = "Test",
                        iconResource = 0,
                        bankName = "Test",
                        last4 = "Test",
                        financialConnectionsSessionId = "1234",
                        intentId = "1234",
                        paymentMethodCreateParams = mock(),
                        customerRequestedSave = mock()
                    )
                )
            )

            val currentScreenState =
                viewModel.currentScreenState.stateIn(viewModel.viewModelScope).value

            assertThat(
                currentScreenState
            ).isInstanceOf(
                USBankAccountFormScreenState.SavedAccount::class.java
            )
        }

    private fun createViewModel(
        args: USBankAccountFormViewModel.Args = defaultArgs
    ): USBankAccountFormViewModel {
        val paymentConfiguration = PaymentConfiguration(
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            STRIPE_ACCOUNT_ID
        )
        return USBankAccountFormViewModel(
            args = args,
            application = ApplicationProvider.getApplicationContext(),
            stripeRepository = stripeRepository,
            lazyPaymentConfig = { paymentConfiguration },
            savedStateHandle = savedStateHandle,
            addressRepository = createAddressRepository(),
        )
    }

    private suspend fun mockUnverifiedBankAccount(): CollectBankAccountResult {
        val paymentIntent = mock<PaymentIntent>()
        val financialConnectionsSession = mock<FinancialConnectionsSession>()
        whenever(paymentIntent.id).thenReturn(defaultArgs.clientSecret?.value)
        whenever(financialConnectionsSession.id).thenReturn("123")
        whenever(financialConnectionsSession.paymentAccount).thenReturn(
            BankAccount(
                id = "123",
                last4 = "4567",
                bankName = "Test",
                routingNumber = "123"
            )
        )
        whenever(
            stripeRepository.attachFinancialConnectionsSessionToPaymentIntent(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(paymentIntent)

        return CollectBankAccountResult.Completed(
            CollectBankAccountResponse(
                intent = paymentIntent,
                financialConnectionsSession = financialConnectionsSession
            )
        )
    }

    private suspend fun mockVerifiedBankAccount(): CollectBankAccountResult {
        val paymentIntent = mock<PaymentIntent>()
        val financialConnectionsSession = mock<FinancialConnectionsSession>()
        whenever(paymentIntent.id).thenReturn(defaultArgs.clientSecret?.value)
        whenever(financialConnectionsSession.id).thenReturn("123")
        whenever(financialConnectionsSession.paymentAccount).thenReturn(
            FinancialConnectionsAccount(
                created = 123,
                id = "123",
                institutionName = "Test",
                livemode = false,
                last4 = "4567",
                supportedPaymentMethodTypes = listOf()
            )
        )
        whenever(
            stripeRepository.attachFinancialConnectionsSessionToPaymentIntent(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(paymentIntent)

        return CollectBankAccountResult.Completed(
            CollectBankAccountResponse(
                intent = paymentIntent,
                financialConnectionsSession = financialConnectionsSession
            )
        )
    }

    private companion object {
        const val MERCHANT_NAME = "merchantName"
        const val CUSTOMER_NAME = "Jenny Rose"
        const val CUSTOMER_EMAIL = "email@email.com"
        const val STRIPE_ACCOUNT_ID = "stripe_account_id"
    }
}

private fun createAddressRepository(): AddressRepository {
    return AddressRepository(
        resources = ApplicationProvider.getApplicationContext<Application>().resources,
        workContext = Dispatchers.Unconfined,
    )
}
