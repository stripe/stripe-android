package com.stripe.android.link.ui.wallet

import androidx.lifecycle.Lifecycle
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.IntentConfirmationInterceptor
import com.stripe.android.R
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkActivityResult.Canceled.Reason
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.confirmation.ConfirmationManager
import com.stripe.android.link.confirmation.PaymentConfirmationCallback
import com.stripe.android.link.injection.SignedInViewModelSubcomponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.Navigator
import com.stripe.android.link.model.PaymentDetailsFixtures.CONSUMER_PAYMENT_DETAILS
import com.stripe.android.link.model.StripeIntentFixtures
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.CvcCheck
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.testing.FakeIntentConfirmationInterceptor
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import javax.inject.Provider
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
class WalletViewModelTest {

    private lateinit var linkAccountManager: LinkAccountManager
    private val navigator = mock<Navigator>()
    private val confirmationManager = mock<ConfirmationManager>()
    private val logger = Logger.noop()
    private val intentConfirmationInterceptor = FakeIntentConfirmationInterceptor()

    private val args = LinkActivityContract.Args(
        configuration = LinkPaymentLauncher.Configuration(
            stripeIntent = StripeIntentFixtures.PI_SUCCEEDED,
            merchantName = "Merchant, Inc.",
            customerName = null,
            customerPhone = null,
            customerEmail = null,
            customerBillingCountryCode = null,
            shippingValues = null,
        ),
        prefilledCardParams = null,
        injectionParams = null,
    )

    @Before
    fun before() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val mockLinkAccount = mock<LinkAccount>().apply {
            whenever(clientSecret).thenReturn(CLIENT_SECRET)
            whenever(email).thenReturn("email@stripe.com")
        }
        linkAccountManager = mock<LinkAccountManager>().apply {
            whenever(linkAccount).thenReturn(MutableStateFlow(mockLinkAccount))
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `On initialization start collecting CardEdit result`() = runTest {
        createViewModel()

        verify(navigator).getResultFlow<PaymentDetailsResult>(any())
    }

    @Test
    fun `On initialization payment details are loaded`() = runTest {
        val card1 = mock<ConsumerPaymentDetails.Card>()
        val card2 = mock<ConsumerPaymentDetails.Card>()
        val paymentDetails = mock<ConsumerPaymentDetails>()
        whenever(paymentDetails.paymentDetails).thenReturn(listOf(card1, card2))

        whenever(linkAccountManager.listPaymentDetails())
            .thenReturn(Result.success(paymentDetails))

        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value.paymentDetailsList).containsExactly(card1, card2)
    }

    @Test
    fun `On initialization when no payment details then navigate to AddPaymentMethod`() = runTest {
        val response = mock<ConsumerPaymentDetails>()
        whenever(response.paymentDetails).thenReturn(emptyList())

        whenever(linkAccountManager.listPaymentDetails())
            .thenReturn(Result.success(response))

        createViewModel()

        verify(navigator).navigateTo(argWhere { it is LinkScreen.PaymentMethod }, eq(true))
    }

    @Test
    fun `On initialization when prefilledCardParams is not null then navigate to AddPaymentMethod`() =
        runTest {
            whenever(linkAccountManager.listPaymentDetails())
                .thenReturn(Result.success(CONSUMER_PAYMENT_DETAILS))

            val argsWithPrefilledCardParams = args.copy(prefilledCardParams = mock())
            createViewModel(argsWithPrefilledCardParams)

            verify(navigator).navigateTo(
                argWhere {
                    it.route == LinkScreen.PaymentMethod(true).route
                },
                eq(false)
            )
        }

    @Test
    fun `onSelectedPaymentDetails starts payment confirmation`() = runTest {
        val paymentDetails = CONSUMER_PAYMENT_DETAILS.paymentDetails.first()
        whenever(linkAccountManager.listPaymentDetails())
            .thenReturn(Result.success(CONSUMER_PAYMENT_DETAILS))

        val viewModel = createViewModel()

        viewModel.onItemSelected(paymentDetails)
        viewModel.onConfirmPayment()

        val confirmParams = mock<ConfirmPaymentIntentParams>()
        intentConfirmationInterceptor.enqueueConfirmStep(confirmParams)

        verify(confirmationManager).confirmStripeIntent(eq(confirmParams), any())
    }

    @Test
    fun `when shippingValues are passed ConfirmPaymentIntentParams has shipping`() = runTest {
        whenever(linkAccountManager.listPaymentDetails())
            .thenReturn(Result.success(CONSUMER_PAYMENT_DETAILS))

        val argsWithShippingValues = args.copy(
            configuration = args.configuration.copy(
                shippingValues = mapOf(
                    IdentifierSpec.Name to "Test Name",
                    IdentifierSpec.Country to "US",
                ),
            )
        )

        val mockInterceptor = mock<IntentConfirmationInterceptor>()

        val viewModel = createViewModel(
            args = argsWithShippingValues,
            intentConfirmationInterceptor = mockInterceptor,
        )

        viewModel.onConfirmPayment()

        val paramsCaptor = argumentCaptor<ConfirmPaymentIntentParams.Shipping>()

        verify(mockInterceptor).interceptNewPayment(
            clientSecret = anyOrNull(),
            paymentMethodCreateParams = any(),
            shippingValues = paramsCaptor.capture(),
            setupForFutureUsage = isNull(),
        )

        assertThat(paramsCaptor.firstValue.toParamMap()).isEqualTo(
            mapOf(
                "address" to mapOf("country" to "US"),
                "name" to "Test Name",
            )
        )
    }

    @Test
    fun `onItemSelected updates selected item`() {
        val paymentDetails = CONSUMER_PAYMENT_DETAILS.paymentDetails.first()
        val viewModel = createViewModel()

        viewModel.onItemSelected(paymentDetails)

        assertThat(viewModel.uiState.value.selectedItem).isEqualTo(paymentDetails)
    }

    @Test
    fun `when selected item is removed then default item is selected`() = runTest {
        val deletedPaymentDetails =
            CONSUMER_PAYMENT_DETAILS.paymentDetails[1]
        val viewModel = createViewModel()
        viewModel.onItemSelected(deletedPaymentDetails)

        assertThat(viewModel.uiState.value.selectedItem).isEqualTo(deletedPaymentDetails)

        whenever(linkAccountManager.deletePaymentDetails(anyOrNull()))
            .thenReturn(Result.success(Unit))
        whenever(linkAccountManager.listPaymentDetails())
            .thenReturn(
                Result.success(
                    CONSUMER_PAYMENT_DETAILS.copy(
                        paymentDetails = CONSUMER_PAYMENT_DETAILS.paymentDetails
                            .filter { it != deletedPaymentDetails }
                    )
                )
            )

        viewModel.deletePaymentMethod(deletedPaymentDetails)

        assertThat(viewModel.uiState.value.selectedItem)
            .isEqualTo(CONSUMER_PAYMENT_DETAILS.paymentDetails.first())
    }

    @Test
    fun `when default item is not supported then first supported item is selected`() = runTest {
        whenever(linkAccountManager.listPaymentDetails())
            .thenReturn(Result.success(CONSUMER_PAYMENT_DETAILS))

        val argsWithFundingSources = args.copy(
            configuration = args.configuration.copy(
                stripeIntent = StripeIntentFixtures.PI_SUCCEEDED.copy(
                    linkFundingSources = listOf(ConsumerPaymentDetails.BankAccount.type),
                ),
            )
        )
        val viewModel = createViewModel(argsWithFundingSources)

        val bankAccount = CONSUMER_PAYMENT_DETAILS.paymentDetails[2]
        assertThat(viewModel.uiState.value.selectedItem).isEqualTo(bankAccount)
    }

    @Test
    fun `when payment confirmation fails then an error message is shown`() {
        val errorThrown = "Error message"
        val viewModel = createViewModel()

        viewModel.onItemSelected(CONSUMER_PAYMENT_DETAILS.paymentDetails.first())
        viewModel.onConfirmPayment()

        val confirmParams = mock<ConfirmPaymentIntentParams>()
        intentConfirmationInterceptor.enqueueConfirmStep(confirmParams)

        val callbackCaptor = argumentCaptor<PaymentConfirmationCallback>()
        verify(confirmationManager).confirmStripeIntent(eq(confirmParams), callbackCaptor.capture())

        callbackCaptor.firstValue(Result.success(PaymentResult.Failed(RuntimeException(errorThrown))))

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo(ErrorMessage.Raw(errorThrown))
    }

    @Test
    fun `deletePaymentMethod fetches payment details and stays expanded when successful`() =
        runTest {
            val paymentDetails = CONSUMER_PAYMENT_DETAILS
            whenever(linkAccountManager.listPaymentDetails())
                .thenReturn(Result.success(paymentDetails))

            val viewModel = createViewModel()
            verify(linkAccountManager).listPaymentDetails()
            clearInvocations(linkAccountManager)
            viewModel.setExpanded(true)

            // Initially has two elements
            assertThat(viewModel.uiState.value.paymentDetailsList)
                .containsExactlyElementsIn(paymentDetails.paymentDetails)

            whenever(linkAccountManager.deletePaymentDetails(anyOrNull()))
                .thenReturn(Result.success(Unit))

            // Delete the first element
            viewModel.deletePaymentMethod(paymentDetails.paymentDetails.first())

            // Fetches payment details again
            verify(linkAccountManager).listPaymentDetails()

            assertThat(viewModel.uiState.value.isExpanded).isTrue()
        }

    @Test
    fun `when selected payment method is not supported then wallet is expanded`() =
        runTest {
            // One card and one bank account
            val paymentDetails = CONSUMER_PAYMENT_DETAILS.copy(
                paymentDetails = listOf(
                    CONSUMER_PAYMENT_DETAILS.paymentDetails[1],
                    CONSUMER_PAYMENT_DETAILS.paymentDetails[2]
                )
            )
            whenever(linkAccountManager.listPaymentDetails())
                .thenReturn(Result.success(paymentDetails))

            val viewModel = createViewModel()
            assertThat(viewModel.uiState.value.paymentDetailsList)
                .containsExactlyElementsIn(paymentDetails.paymentDetails)

            // First item is default, so it should be selected and the list should be collapsed
            val defaultItem = paymentDetails.paymentDetails.first()
            assertThat(viewModel.uiState.value.selectedItem).isEqualTo(defaultItem)
            assertThat(viewModel.uiState.value.isExpanded).isFalse()

            whenever(linkAccountManager.deletePaymentDetails(anyOrNull()))
                .thenReturn(Result.success(Unit))

            // Only the bank account is returned, which is not supported
            whenever(linkAccountManager.listPaymentDetails()).thenReturn(
                Result.success(
                    paymentDetails.copy(paymentDetails = listOf(paymentDetails.paymentDetails[1]))
                )
            )

            // Delete the selected item
            viewModel.deletePaymentMethod(defaultItem)

            assertThat(viewModel.uiState.value.isExpanded).isTrue()
        }

    @Test
    fun `when payment method deletion fails then an error message is shown`() = runTest {
        whenever(linkAccountManager.listPaymentDetails())
            .thenReturn(Result.success(CONSUMER_PAYMENT_DETAILS))

        val errorThrown = "Error message"
        val viewModel = createViewModel()

        whenever(linkAccountManager.deletePaymentDetails(anyOrNull()))
            .thenReturn(Result.failure(RuntimeException(errorThrown)))

        viewModel.deletePaymentMethod(CONSUMER_PAYMENT_DETAILS.paymentDetails.first())

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo(ErrorMessage.Raw(errorThrown))
    }

    @Test
    fun `onSelectedPaymentDetails dismisses on success`() = runTest {
        whenever(confirmationManager.confirmStripeIntent(any(), any())).thenAnswer { invocation ->
            (invocation.getArgument(1) as? PaymentConfirmationCallback)?.let {
                it(Result.success(PaymentResult.Completed))
            }
        }
        whenever(linkAccountManager.listPaymentDetails())
            .thenReturn(Result.success(CONSUMER_PAYMENT_DETAILS))

        val viewModel = createViewModel()
        viewModel.onConfirmPayment()

        val confirmParams = mock<ConfirmPaymentIntentParams>()
        intentConfirmationInterceptor.enqueueConfirmStep(confirmParams)
        assertThat(viewModel.uiState.value.primaryButtonState).isEqualTo(PrimaryButtonState.Completed)

        advanceTimeBy(PrimaryButtonState.COMPLETED_DELAY_MS + 1)
        verify(navigator).dismiss(LinkActivityResult.Completed)
    }

    @Test
    fun `Pay another way dismisses Link`() {
        val viewModel = createViewModel()

        viewModel.payAnotherWay()

        verify(navigator).cancel(reason = eq(Reason.PayAnotherWay))
        verify(linkAccountManager, never()).logout()
    }

    @Test
    fun `Add new payment method navigates to AddPaymentMethod screen`() {
        val viewModel = createViewModel()

        viewModel.addNewPaymentMethod()

        verify(navigator).navigateTo(argWhere { it is LinkScreen.PaymentMethod }, eq(false))
    }

    @Test
    fun `Update payment method navigates to CardEdit screen`() = runTest {
        val paymentDetails = CONSUMER_PAYMENT_DETAILS
        whenever(linkAccountManager.listPaymentDetails())
            .thenReturn(Result.success(paymentDetails))

        val viewModel = createViewModel()

        viewModel.editPaymentMethod(paymentDetails.paymentDetails.first())

        verify(navigator).navigateTo(
            argWhere {
                it.route.startsWith(LinkScreen.CardEdit.route.substringBefore('?'))
            },
            any()
        )
    }

    @Test
    fun `On CardEdit result successful then it reloads payment details`() = runTest {
        val flow = MutableStateFlow<PaymentDetailsResult?>(null)
        whenever(navigator.getResultFlow<PaymentDetailsResult>(any())).thenReturn(flow)

        createViewModel()
        verify(linkAccountManager).listPaymentDetails()
        clearInvocations(linkAccountManager)

        flow.emit(PaymentDetailsResult.Success(""))
        verify(linkAccountManager).listPaymentDetails()
    }

    @Test
    fun `On CardEdit result failure then it shows error`() = runTest {
        val flow = MutableStateFlow<PaymentDetailsResult?>(null)
        whenever(navigator.getResultFlow<PaymentDetailsResult>(any())).thenReturn(flow)

        val viewModel = createViewModel()

        val error = ErrorMessage.Raw("Error message")
        flow.emit(PaymentDetailsResult.Failure(error))

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo(error)
    }

    @Test
    fun `Sends CVC if paying with card that requires CVC recollection`() = runTest {
        val paymentDetails = mockCard(cvcCheck = CvcCheck.Fail)
        val mockInterceptor = mock<IntentConfirmationInterceptor>()

        val viewModel = createViewModel(intentConfirmationInterceptor = mockInterceptor)
        val cvcInput = "123"

        viewModel.onItemSelected(paymentDetails)
        viewModel.cvcController.onRawValueChange(cvcInput)
        viewModel.onConfirmPayment()

        val paramsCaptor = argumentCaptor<PaymentMethodCreateParams>()

        verify(mockInterceptor).interceptNewPayment(
            clientSecret = anyOrNull(),
            paymentMethodCreateParams = paramsCaptor.capture(),
            shippingValues = anyOrNull(),
            setupForFutureUsage = anyOrNull(),
        )

        val paramsMap = paramsCaptor.firstValue.toParamMap()

        val link = paramsMap["link"] as Map<*, *>
        val card = link["card"] as Map<*, *>
        val cvc = card["cvc"]

        assertThat(cvc).isEqualTo(cvcInput)
    }

    @Test
    fun `Does not send CVC if paying with card that does not require CVC recollection`() = runTest {
        whenever(linkAccountManager.listPaymentDetails())
            .thenReturn(Result.success(CONSUMER_PAYMENT_DETAILS))

        val paymentDetails = mockCard(cvcCheck = CvcCheck.Pass)
        val mockInterceptor = mock<IntentConfirmationInterceptor>()

        val viewModel = createViewModel(intentConfirmationInterceptor = mockInterceptor)
        viewModel.onItemSelected(paymentDetails)
        viewModel.onConfirmPayment()

        val paramsCaptor = argumentCaptor<PaymentMethodCreateParams>()
        verify(mockInterceptor).interceptNewPayment(
            clientSecret = anyOrNull(),
            paymentMethodCreateParams = paramsCaptor.capture(),
            shippingValues = anyOrNull(),
            setupForFutureUsage = anyOrNull(),
        )

        val paramsMap = paramsCaptor.firstValue.toParamMap()

        val link = paramsMap["link"] as Map<*, *>
        assertThat(link).doesNotContainKey("card")
    }

    @Test
    fun `Updates payment details when paying with expired card`() = runTest {
        val expiredCard = mockCard(isExpired = true)
        val viewModel = createViewModel()

        viewModel.onItemSelected(expiredCard)
        viewModel.expiryDateController.onRawValueChange("1230")
        viewModel.cvcController.onRawValueChange("123")

        viewModel.onConfirmPayment()

        val paramsCaptor = argumentCaptor<ConsumerPaymentDetailsUpdateParams>()
        verify(linkAccountManager).updatePaymentDetails(paramsCaptor.capture())

        val paramsMap = paramsCaptor.firstValue.toParamMap()
        assertThat(paramsMap["exp_month"]).isEqualTo("12")
        assertThat(paramsMap["exp_year"]).isEqualTo("2030")
    }

    @Test
    fun `Shows alert dialog if updating expired card info fails`() = runTest {
        whenever(linkAccountManager.updatePaymentDetails(any()))
            .thenReturn(Result.failure(APIConnectionException()))

        val expiredCard = mockCard(isExpired = true)
        val viewModel = createViewModel()

        viewModel.onItemSelected(expiredCard)
        viewModel.expiryDateController.onRawValueChange("1230")
        viewModel.cvcController.onRawValueChange("123")

        viewModel.onConfirmPayment()

        assertThat(viewModel.uiState.value.alertMessage).isEqualTo(
            ErrorMessage.FromResources(R.string.stripe_failure_connection_error)
        )
    }

    @Test
    fun `Resets expiry date and CVC controllers when new payment method is selected`() = runTest {
        val paymentDetails = CONSUMER_PAYMENT_DETAILS.paymentDetails[1]
        whenever(linkAccountManager.listPaymentDetails())
            .thenReturn(Result.success(CONSUMER_PAYMENT_DETAILS))

        val viewModel = createViewModel().apply {
            expiryDateController.onRawValueChange("1230")
            cvcController.onRawValueChange("123")
        }

        viewModel.onItemSelected(paymentDetails)

        val uiState = viewModel.uiState.value
        assertThat(uiState.expiryDateInput).isEqualTo(FormFieldEntry(value = ""))
        assertThat(uiState.cvcInput).isEqualTo(FormFieldEntry(value = ""))
    }

    @Test
    fun `Expiry date and CVC values are kept when existing payment method is selected`() = runTest {
        val paymentDetails = CONSUMER_PAYMENT_DETAILS.paymentDetails.first()
        whenever(linkAccountManager.listPaymentDetails())
            .thenReturn(Result.success(CONSUMER_PAYMENT_DETAILS))

        val viewModel = createViewModel().apply {
            expiryDateController.onRawValueChange("1230")
            cvcController.onRawValueChange("123")
        }

        viewModel.onItemSelected(paymentDetails)

        val uiState = viewModel.uiState.value
        assertThat(uiState.expiryDateInput.value).isEqualTo("1230")
        assertThat(uiState.cvcInput.value).isEqualTo("123")
    }

    @Test
    fun `Updates payment method default selection correctly`() = runTest {
        val originalPaymentDetails = mockCard(isDefault = false)
        val updatedPaymentDetails = originalPaymentDetails.copy(isDefault = true)

        val originalResponse = CONSUMER_PAYMENT_DETAILS.copy(
            paymentDetails = CONSUMER_PAYMENT_DETAILS.paymentDetails.dropLast(1) + originalPaymentDetails
        )
        val updateResponse = CONSUMER_PAYMENT_DETAILS.copy(
            paymentDetails = listOf(updatedPaymentDetails)
        )

        whenever(linkAccountManager.listPaymentDetails())
            .thenReturn(Result.success(originalResponse))

        whenever(linkAccountManager.updatePaymentDetails(any()))
            .thenReturn(Result.success(updateResponse))

        val viewModel = createViewModel()
        viewModel.uiState.test {
            // We need to skip the initial UI state
            skipItems(1)

            viewModel.setDefault(originalPaymentDetails)

            val loadingUiState = awaitItem()
            assertThat(loadingUiState.paymentMethodIdBeingUpdated).isEqualTo(originalPaymentDetails.id)

            val finalUiState = awaitItem()
            assertThat(finalUiState.paymentMethodIdBeingUpdated).isNull()

            assertThat(
                finalUiState.paymentDetailsList.filter { it.isDefault }.size
            ).isEqualTo(1)

            assertThat(
                finalUiState.paymentDetailsList.single { it.isDefault }
            ).isEqualTo(updatedPaymentDetails)
        }
    }

    @Test
    fun `Confirms intent if confirmation interceptor returns unconfirmed intent`() = runTest {
        val args = args.copy(
            configuration = args.configuration.copy(
                stripeIntent = StripeIntentFixtures.PI_SUCCEEDED.copy(clientSecret = null),
            ),
        )

        val viewModel = createViewModel(args)
        val paymentDetails = CONSUMER_PAYMENT_DETAILS.paymentDetails.first()

        viewModel.onItemSelected(paymentDetails)
        viewModel.onConfirmPayment()

        val confirmParams = mock<ConfirmPaymentIntentParams>()
        intentConfirmationInterceptor.enqueueConfirmStep(confirmParams)

        verify(confirmationManager).confirmStripeIntent(eq(confirmParams), any())
    }

    @Test
    fun `Finishes with success if confirmation interceptor returns confirmed intent`() = runTest {
        whenever(confirmationManager.confirmStripeIntent(any(), any())).thenAnswer { invocation ->
            (invocation.getArgument(1) as? PaymentConfirmationCallback)?.let {
                it(Result.success(PaymentResult.Completed))
            }
        }
        whenever(linkAccountManager.listPaymentDetails())
            .thenReturn(Result.success(CONSUMER_PAYMENT_DETAILS))

        val stripeIntent = StripeIntentFixtures.PI_SUCCEEDED

        val args = args.copy(
            configuration = args.configuration.copy(
                stripeIntent = stripeIntent.copy(clientSecret = null),
            ),
        )

        val viewModel = createViewModel(args)
        val paymentDetails = CONSUMER_PAYMENT_DETAILS.paymentDetails.first()

        viewModel.onItemSelected(paymentDetails)
        viewModel.onConfirmPayment()

        val confirmParams = mock<ConfirmPaymentIntentParams>()
        intentConfirmationInterceptor.enqueueConfirmStep(confirmParams)

        advanceTimeBy(PrimaryButtonState.COMPLETED_DELAY_MS + 1)
        verify(navigator).dismiss(eq(LinkActivityResult.Completed))
    }

    @Test
    fun `Displays error if confirmation interceptor returns a failure`() = runTest {
        val stripeIntent = StripeIntentFixtures.PI_SUCCEEDED

        val args = args.copy(
            configuration = args.configuration.copy(
                stripeIntent = stripeIntent.copy(clientSecret = null),
            ),
        )

        val viewModel = createViewModel(args)
        val paymentDetails = CONSUMER_PAYMENT_DETAILS.paymentDetails.first()

        viewModel.onItemSelected(paymentDetails)
        viewModel.onConfirmPayment()

        val errorMessage = "oops, this one failed"
        intentConfirmationInterceptor.enqueueFailureStep(
            cause = Exception("some technical explanation that we don't show to the user"),
            message = errorMessage,
        )

        viewModel.uiState.test {
            assertThat(awaitItem().errorMessage).isEqualTo(ErrorMessage.Raw(errorMessage))
        }
    }

    @Test
    fun `Handles next action if confirmation interceptor returns an intent with an outstanding action`() = runTest {
        val stripeIntent = StripeIntentFixtures.PI_SUCCEEDED.copy(clientSecret = null)
        val args = args.copy(configuration = args.configuration.copy(stripeIntent = stripeIntent))

        val viewModel = createViewModel(args)
        val paymentDetails = CONSUMER_PAYMENT_DETAILS.paymentDetails.first()

        viewModel.onItemSelected(paymentDetails)
        viewModel.onConfirmPayment()

        val clientSecret = "pi_1234_secret_4321"
        intentConfirmationInterceptor.enqueueNextActionStep(clientSecret = clientSecret)

        verify(confirmationManager).handleNextAction(
            clientSecret = eq(clientSecret),
            stripeIntent = eq(stripeIntent),
            onResult = any(),
        )
    }

    @Test
    fun `Factory gets initialized by Injector`() {
        val mockBuilder = mock<SignedInViewModelSubcomponent.Builder>()
        val mockSubComponent = mock<SignedInViewModelSubcomponent>()
        val vmToBeReturned = mock<WalletViewModel>()

        whenever(mockBuilder.linkAccount(any())).thenReturn(mockBuilder)
        whenever(mockBuilder.build()).thenReturn(mockSubComponent)
        whenever((mockSubComponent.walletViewModel)).thenReturn(vmToBeReturned)

        val mockSavedStateRegistryOwner = mock<SavedStateRegistryOwner>()
        val mockSavedStateRegistry = mock<SavedStateRegistry>()
        val mockLifeCycle = mock<Lifecycle>()

        whenever(mockSavedStateRegistryOwner.savedStateRegistry).thenReturn(mockSavedStateRegistry)
        whenever(mockSavedStateRegistryOwner.lifecycle).thenReturn(mockLifeCycle)
        whenever(mockLifeCycle.currentState).thenReturn(Lifecycle.State.CREATED)

        val injector = object : NonFallbackInjector {
            override fun inject(injectable: Injectable<*>) {
                val factory = injectable as WalletViewModel.Factory
                factory.subComponentBuilderProvider = Provider { mockBuilder }
            }
        }

        val factory = WalletViewModel.Factory(
            mock(),
            injector
        )
        val factorySpy = spy(factory)
        val createdViewModel = factorySpy.create(WalletViewModel::class.java)
        assertThat(createdViewModel).isEqualTo(vmToBeReturned)
    }

    private fun createViewModel(
        args: LinkActivityContract.Args = this.args,
        intentConfirmationInterceptor: IntentConfirmationInterceptor = this.intentConfirmationInterceptor,
    ): WalletViewModel = WalletViewModel(
        args = args,
        linkAccountManager = linkAccountManager,
        navigator = navigator,
        confirmationManager = confirmationManager,
        logger = logger,
        intentConfirmationInterceptor = intentConfirmationInterceptor,
    )

    private fun mockCard(
        cvcCheck: CvcCheck = CvcCheck.Pass,
        isExpired: Boolean = false,
        isDefault: Boolean = true
    ): ConsumerPaymentDetails.Card {
        val id = Random.nextInt()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val expiryYear = if (isExpired) currentYear - 1 else currentYear + 1

        return ConsumerPaymentDetails.Card(
            id = "id_$id",
            isDefault = isDefault,
            expiryYear = expiryYear,
            expiryMonth = 12,
            brand = CardBrand.Visa,
            last4 = "4242",
            cvcCheck = cvcCheck
        )
    }

    companion object {
        const val CLIENT_SECRET = "client_secret"
    }
}
