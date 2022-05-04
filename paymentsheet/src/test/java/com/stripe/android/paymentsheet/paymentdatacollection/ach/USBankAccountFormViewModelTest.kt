package com.stripe.android.paymentsheet.paymentdatacollection.ach

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.model.PaymentIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResponse
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.elements.IdentifierSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class USBankAccountFormViewModelTest {
    private val defaultArgs = USBankAccountFormViewModel.Args(
        formArgs = FormFragmentArguments(
            paymentMethod = SupportedPaymentMethod.USBankAccount,
            showCheckbox = false,
            showCheckboxControlledFields = false,
            merchantName = MERCHANT_NAME,
            amount = Amount(5099, "usd"),
            injectorKey = INJECTOR_KEY
        ),
        completePayment = true,
        clientSecret = PaymentIntentClientSecret("pi_12345")
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

            viewModel.nameElement.setRawValue(mapOf(IdentifierSpec.Name to CUSTOMER_NAME))
            viewModel.emailElement.setRawValue(mapOf(IdentifierSpec.Email to CUSTOMER_EMAIL))

            assertThat(viewModel.name.stateIn(viewModel.viewModelScope).value).isNotEmpty()
            assertThat(viewModel.email.stateIn(viewModel.viewModelScope).value).isNotEmpty()

            assertThat(viewModel.requiredFields.stateIn(viewModel.viewModelScope).value).isTrue()
        }

    @Test
    fun `collect bank account is callable with initial screen state`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel()
            viewModel.collectBankAccountLauncher = collectBankAccountLauncher
            val currentScreenState = viewModel.currentScreenState.stateIn(viewModel.viewModelScope).value

            assertThat(
                currentScreenState
            ).isInstanceOf(
                USBankAccountFormScreenState.NameAndEmailCollection::class.java
            )

            (currentScreenState as USBankAccountFormScreenState.NameAndEmailCollection).primaryButtonOnClick()

            verify(collectBankAccountLauncher).presentWithPaymentIntent(any(), any(), any())
        }

    @Test
    fun `when payment sheet, unverified bank account, then confirm intent callable`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel()

            viewModel.handleCollectBankAccountResult(
                mockUnverifiedBankAccount()
            )

            val currentScreenState = viewModel.currentScreenState.stateIn(viewModel.viewModelScope).value
            assertThat(currentScreenState)
                .isInstanceOf(USBankAccountFormScreenState.VerifyWithMicrodeposits::class.java)

            (currentScreenState as USBankAccountFormScreenState.VerifyWithMicrodeposits).primaryButtonOnClick()

            val newScreenState = viewModel.currentScreenState.stateIn(viewModel.viewModelScope).value
            assertThat(newScreenState)
                .isInstanceOf(USBankAccountFormScreenState.ConfirmIntent::class.java)
        }

    @Test
    fun `when payment sheet, verified bank account, then confirm intent callable`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel()

            viewModel.handleCollectBankAccountResult(
                mockVerifiedBankAccount()
            )

            val currentScreenState = viewModel.currentScreenState.stateIn(viewModel.viewModelScope).value
            assertThat(currentScreenState)
                .isInstanceOf(USBankAccountFormScreenState.MandateCollection::class.java)

            (currentScreenState as USBankAccountFormScreenState.MandateCollection).primaryButtonOnClick()

            val newScreenState = viewModel.currentScreenState.stateIn(viewModel.viewModelScope).value
            assertThat(newScreenState)
                .isInstanceOf(USBankAccountFormScreenState.ConfirmIntent::class.java)
        }

    @Test
    fun `when payment options, unverified bank account, then finished`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(defaultArgs.copy(completePayment = false))

            viewModel.handleCollectBankAccountResult(
                mockUnverifiedBankAccount()
            )

            val currentScreenState = viewModel.currentScreenState.stateIn(viewModel.viewModelScope).value
            assertThat(currentScreenState)
                .isInstanceOf(USBankAccountFormScreenState.VerifyWithMicrodeposits::class.java)

            (currentScreenState as USBankAccountFormScreenState.VerifyWithMicrodeposits).primaryButtonOnClick()

            val newScreenState = viewModel.currentScreenState.stateIn(viewModel.viewModelScope).value
            assertThat(newScreenState)
                .isInstanceOf(USBankAccountFormScreenState.Finished::class.java)
        }

    @Test
    fun `when payment options, verified bank account, then finished`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(defaultArgs.copy(completePayment = false))

            viewModel.handleCollectBankAccountResult(
                mockVerifiedBankAccount()
            )

            val currentScreenState = viewModel.currentScreenState.stateIn(viewModel.viewModelScope).value
            assertThat(currentScreenState)
                .isInstanceOf(USBankAccountFormScreenState.MandateCollection::class.java)

            (currentScreenState as USBankAccountFormScreenState.MandateCollection).primaryButtonOnClick()

            val newScreenState = viewModel.currentScreenState.stateIn(viewModel.viewModelScope).value
            assertThat(newScreenState)
                .isInstanceOf(USBankAccountFormScreenState.Finished::class.java)
        }

    private fun createViewModel(
        args: USBankAccountFormViewModel.Args = defaultArgs
    ): USBankAccountFormViewModel {
        val paymentConfiguration = PaymentConfiguration(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        return USBankAccountFormViewModel(
            args = args,
            application = ApplicationProvider.getApplicationContext(),
            stripeRepository = stripeRepository,
            lazyPaymentConfig = { paymentConfiguration },
            savedStateHandle = savedStateHandle
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
            stripeRepository.attachFinancialConnectionsSessionToPaymentIntent(any(), any(), any(), any())
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
                supportedPaymentMethodTypes = listOf(),
            )
        )
        whenever(
            stripeRepository.attachFinancialConnectionsSessionToPaymentIntent(any(), any(), any(), any())
        ).thenReturn(paymentIntent)

        return CollectBankAccountResult.Completed(
            CollectBankAccountResponse(
                intent = paymentIntent,
                financialConnectionsSession = financialConnectionsSession
            )
        )
    }

    private companion object {
        const val INJECTOR_KEY = "injectorKey"
        const val MERCHANT_NAME = "merchantName"
        const val CUSTOMER_NAME = "customer name"
        const val CUSTOMER_EMAIL = "customer@email.com"
    }
}
