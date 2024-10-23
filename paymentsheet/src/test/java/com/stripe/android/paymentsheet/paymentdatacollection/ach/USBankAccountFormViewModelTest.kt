package com.stripe.android.paymentsheet.paymentdatacollection.ach

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResponseInternal
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class USBankAccountFormViewModelTest {

    private val defaultArgs = USBankAccountFormViewModel.Args(
        instantDebits = false,
        formArgs = FormArguments(
            paymentMethodCode = PaymentMethod.Type.USBankAccount.code,
            merchantName = MERCHANT_NAME,
            amount = Amount(5099, "usd"),
            billingDetails = PaymentSheet.BillingDetails(
                name = CUSTOMER_NAME,
                email = CUSTOMER_EMAIL
            ),
            cbcEligibility = CardBrandChoiceEligibility.Ineligible,
            hasIntentToSetup = false,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
        ),
        showCheckbox = false,
        isCompleteFlow = true,
        isPaymentFlow = true,
        stripeIntentId = "id_12345",
        clientSecret = "pi_12345_secret_54321",
        onBehalfOf = "on_behalf_of_id",
        savedPaymentMethod = null,
        shippingDetails = null,
        hostedSurface = CollectBankAccountLauncher.HOSTED_SURFACE_PAYMENT_ELEMENT,
        linkMode = null,
    )

    private val mockCollectBankAccountLauncher = mock<CollectBankAccountLauncher>()
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

            assertThat(viewModel.name.value).isEqualTo(
                CUSTOMER_NAME
            )
            assertThat(viewModel.email.value).isEqualTo(
                CUSTOMER_EMAIL
            )

            assertThat(viewModel.requiredFields.value).isTrue()
        }

    @Test
    fun `when name is valid then required fields are filled for instant debits flow`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(
                args = defaultArgs.copy(instantDebits = true),
            )

            assertThat(viewModel.requiredFields.value).isTrue()
        }

    @Test
    fun `when email and name is invalid then required fields are not filled`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel()

            viewModel.nameController.onRawValueChange("      ")
            viewModel.emailController.onRawValueChange(CUSTOMER_EMAIL)

            assertThat(viewModel.requiredFields.value).isFalse()

            viewModel.nameController.onRawValueChange(CUSTOMER_NAME)
            viewModel.emailController.onRawValueChange(CUSTOMER_EMAIL)

            assertThat(viewModel.requiredFields.value).isTrue()

            viewModel.nameController.onRawValueChange(CUSTOMER_NAME)
            viewModel.emailController.onRawValueChange("")

            assertThat(viewModel.requiredFields.value).isFalse()
        }

    @Test
    fun `collect bank account is callable with initial screen state`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel()
            viewModel.collectBankAccountLauncher = mockCollectBankAccountLauncher
            val currentScreenState =
                viewModel.currentScreenState.value

            assertThat(
                currentScreenState
            ).isInstanceOf<
                USBankAccountFormScreenState.BillingDetailsCollection
                >()

            viewModel.handlePrimaryButtonClick(
                currentScreenState as USBankAccountFormScreenState.BillingDetailsCollection
            )

            verify(mockCollectBankAccountLauncher).presentWithPaymentIntent(any(), any(), any(), any())
        }

    @Test
    fun `when payment sheet, unverified bank account, then confirm intent callable`() = runTest {
        val viewModel = createViewModel()

        viewModel.result.test {
            viewModel.handleCollectBankAccountResult(mockUnverifiedBankAccount())

            val currentScreenState = viewModel.currentScreenState.value
            viewModel.handlePrimaryButtonClick(currentScreenState as USBankAccountFormScreenState.VerifyWithMicrodeposits)

            assertThat(awaitItem()?.screenState).isEqualTo(currentScreenState)
        }
    }

    @Test
    fun `when payment sheet, verified bank account, then confirm intent callable`() = runTest {
        val viewModel = createViewModel()

        viewModel.result.test {
            viewModel.handleCollectBankAccountResult(mockVerifiedBankAccount())

            val currentScreenState = viewModel.currentScreenState.value
            viewModel.handlePrimaryButtonClick(currentScreenState as USBankAccountFormScreenState.MandateCollection)

            assertThat(awaitItem()?.screenState).isEqualTo(currentScreenState)
        }
    }

    @Test
    fun `when payment options, unverified bank account, then finished`() = runTest {
        val viewModel = createViewModel(defaultArgs.copy(isCompleteFlow = false))
        val bankAccount = mockUnverifiedBankAccount()

        viewModel.result.test {
            viewModel.handleCollectBankAccountResult(bankAccount)

            val currentScreenState = viewModel.currentScreenState.value
            viewModel.handlePrimaryButtonClick(currentScreenState as USBankAccountFormScreenState.VerifyWithMicrodeposits)

            assertThat(awaitItem()?.screenState).isEqualTo(currentScreenState)
        }
    }

    @Test
    fun `when payment options, verified bank account, then finished`() = runTest {
        val viewModel = createViewModel(defaultArgs.copy(isCompleteFlow = false))
        val bankAccount = mockVerifiedBankAccount()

        viewModel.result.test {
            viewModel.handleCollectBankAccountResult(bankAccount)

            val currentScreenState = viewModel.currentScreenState.value
            viewModel.handlePrimaryButtonClick(currentScreenState as USBankAccountFormScreenState.MandateCollection)

            assertThat(awaitItem()?.screenState).isEqualTo(currentScreenState)
        }
    }

    @Test
    fun `when payment options, verified bank account, then result has correct paymentMethodOptionsParams`() = runTest {
        val viewModel = createViewModel(
            defaultArgs.copy(
                formArgs = defaultArgs.formArgs,
                showCheckbox = true,
            )
        )
        val bankAccount = mockVerifiedBankAccount()

        viewModel.result.test {
            viewModel.handleCollectBankAccountResult(bankAccount)

            val currentScreenState = viewModel.currentScreenState.value
            viewModel.handlePrimaryButtonClick(currentScreenState as USBankAccountFormScreenState.MandateCollection)

            assertThat(awaitItem()?.paymentMethodOptionsParams).isEqualTo(
                PaymentMethodOptionsParams.USBankAccount(
                    setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.Blank
                )
            )
        }
    }

    @Test
    fun `when reset, primary button launches bank account collection`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel()
            viewModel.collectBankAccountLauncher = mockCollectBankAccountLauncher
            viewModel.reset()

            val currentScreenState =
                viewModel.currentScreenState.value

            viewModel.handlePrimaryButtonClick(
                currentScreenState as USBankAccountFormScreenState.BillingDetailsCollection
            )

            verify(mockCollectBankAccountLauncher).presentWithPaymentIntent(any(), any(), any(), any())
        }

    @Test
    fun `when reset, save for future usage should be false`() = runTest {
        val viewModel = createViewModel()
        viewModel.collectBankAccountLauncher = mockCollectBankAccountLauncher

        viewModel.saveForFutureUseElement.controller.onValueChange(false)

        viewModel.saveForFutureUseElement.controller.saveForFutureUse.test {
            assertThat(awaitItem()).isFalse()
            viewModel.reset()
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `Correctly restores input when re-opening screen`() = runTest {
        val input = PaymentSelection.New.USBankAccount.Input(
            name = "Some One",
            email = "someone@email.com",
            phone = "1112223456",
            address = Address(
                line1 = "123 Not Main Street",
                line2 = "Apt 123",
                city = "San Francisco",
                state = "CA",
                postalCode = "94111",
                country = "US",
            ),
            saveForFutureUse = true,
        )

        val viewModel = createViewModel(
            defaultArgs.copy(
                formArgs = defaultArgs.formArgs.copy(billingDetails = null),
                savedPaymentMethod = PaymentSelection.New.USBankAccount(
                    labelResource = "Test",
                    iconResource = 0,
                    paymentMethodCreateParams = mock(),
                    customerRequestedSave = mock(),
                    input = input,
                    instantDebits = null,
                    screenState = USBankAccountFormScreenState.SavedAccount(
                        financialConnectionsSessionId = "session_1234",
                        intentId = "intent_1234",
                        bankName = "Stripe Bank",
                        last4 = "6789",
                        primaryButtonText = "Continue".resolvableString,
                        mandateText = null,
                    ),
                )
            )
        )

        assertThat(viewModel.name.value).isEqualTo(input.name)
        assertThat(viewModel.email.value).isEqualTo(input.email)
        assertThat(viewModel.phone.value).isEqualTo(input.phone)
        assertThat(viewModel.address.value).isEqualTo(input.address)
    }

    @Test
    fun `Prioritizes previous input over default values`() = runTest {
        val input = PaymentSelection.New.USBankAccount.Input(
            name = "Some One",
            email = "someone@email.com",
            phone = "1112223456",
            address = Address(
                line1 = "123 Not Main Street",
                line2 = "Apt 123",
                city = "San Francisco",
                state = "CA",
                postalCode = "94111",
                country = "US",
            ),
            saveForFutureUse = true,
        )

        val viewModel = createViewModel(
            defaultArgs.copy(
                formArgs = defaultArgs.formArgs.copy(
                    billingDetails = PaymentSheet.BillingDetails(
                        name = CUSTOMER_NAME,
                        email = CUSTOMER_EMAIL,
                        phone = CUSTOMER_PHONE,
                        address = CUSTOMER_ADDRESS,
                    ),
                ),
                savedPaymentMethod = PaymentSelection.New.USBankAccount(
                    labelResource = "Test",
                    iconResource = 0,
                    paymentMethodCreateParams = mock(),
                    customerRequestedSave = mock(),
                    input = input,
                    instantDebits = null,
                    screenState = USBankAccountFormScreenState.SavedAccount(
                        financialConnectionsSessionId = "session_1234",
                        intentId = "intent_1234",
                        bankName = "Stripe Bank",
                        last4 = "6789",
                        primaryButtonText = "Continue".resolvableString,
                        mandateText = null,
                    ),
                )
            )
        )

        assertThat(viewModel.name.value).isEqualTo(input.name)
        assertThat(viewModel.email.value).isEqualTo(input.email)
        assertThat(viewModel.phone.value).isEqualTo(input.phone)
        assertThat(viewModel.address.value).isEqualTo(input.address)
    }

    @Test
    fun `Uses default values if no previous input is provided`() = runTest {
        val viewModel = createViewModel(
            defaultArgs.copy(
                formArgs = defaultArgs.formArgs.copy(
                    billingDetails = PaymentSheet.BillingDetails(
                        name = CUSTOMER_NAME,
                        email = CUSTOMER_EMAIL,
                        phone = CUSTOMER_PHONE_COUNTRY_CODE + CUSTOMER_PHONE,
                        address = CUSTOMER_ADDRESS,
                    ),
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        phone = CollectionMode.Always,
                        address = AddressCollectionMode.Full,
                    ),
                ),
            )
        )

        assertThat(viewModel.name.value).isEqualTo(CUSTOMER_NAME)
        assertThat(viewModel.email.value).isEqualTo(CUSTOMER_EMAIL)
        assertThat(viewModel.phoneController.getCountryCode()).isEqualTo(CUSTOMER_COUNTRY)
        assertThat(viewModel.phone.value).isEqualTo(CUSTOMER_PHONE)
        assertThat(viewModel.address.value).isEqualTo(CUSTOMER_ADDRESS.asAddressModel())
    }

    @Test
    fun `Restores screen state when re-opening screen`() = runTest {
        val continueMandate = USBankAccountTextBuilder.buildMandateText(
            merchantName = MERCHANT_NAME,
            isSaveForFutureUseSelected = true,
            isInstantDebits = false,
            isSetupFlow = false,
        )

        val continueWithMicrodepositsMandate = USBankAccountTextBuilder.buildMandateAndMicrodepositsText(
            merchantName = MERCHANT_NAME,
            isVerifyingMicrodeposits = true,
            isSaveForFutureUseSelected = true,
            isInstantDebits = false,
            isSetupFlow = false,
        )

        val screenStates = listOf(
            USBankAccountFormScreenState.BillingDetailsCollection(
                primaryButtonText = "Continue".resolvableString,
                isProcessing = false,
            ),
            USBankAccountFormScreenState.MandateCollection(
                resultIdentifier = USBankAccountFormScreenState.ResultIdentifier.Session("session_1234"),
                intentId = "intent_1234",
                bankName = "Stripe Bank",
                last4 = null,
                primaryButtonText = "Continue".resolvableString,
                mandateText = continueMandate,
            ),
            USBankAccountFormScreenState.VerifyWithMicrodeposits(
                financialConnectionsSessionId = "session_1234",
                intentId = "intent_1234",
                paymentAccount = BankAccount(
                    id = "bank_id",
                    last4 = "6789",
                ),
                primaryButtonText = "Continue".resolvableString,
                mandateText = continueWithMicrodepositsMandate,
            ),
            USBankAccountFormScreenState.SavedAccount(
                financialConnectionsSessionId = "session_1234",
                intentId = "intent_1234",
                bankName = "Stripe Bank",
                last4 = "6789",
                primaryButtonText = "Continue".resolvableString,
                mandateText = continueMandate,
            ),
        )

        for (screenState in screenStates) {
            val viewModel = createViewModel(
                defaultArgs.copy(
                    savedPaymentMethod = PaymentSelection.New.USBankAccount(
                        labelResource = "Test",
                        iconResource = 0,
                        paymentMethodCreateParams = mock(),
                        customerRequestedSave = mock(),
                        input = PaymentSelection.New.USBankAccount.Input(
                            name = "Some One",
                            email = "someone@email.com",
                            phone = "1112223456",
                            address = CUSTOMER_ADDRESS.asAddressModel(),
                            saveForFutureUse = true,
                        ),
                        instantDebits = null,
                        screenState = screenState,
                    )
                )
            )

            assertThat(viewModel.currentScreenState.value).isEqualTo(screenState)
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
                        paymentMethodCreateParams = mock(),
                        customerRequestedSave = mock(),
                        input = PaymentSelection.New.USBankAccount.Input(
                            name = "",
                            email = null,
                            phone = null,
                            address = null,
                            saveForFutureUse = false,
                        ),
                        instantDebits = null,
                        screenState = USBankAccountFormScreenState.SavedAccount(
                            financialConnectionsSessionId = "session_1234",
                            intentId = "intent_1234",
                            bankName = "Stripe Bank",
                            last4 = "6789",
                            primaryButtonText = "Continue".resolvableString,
                            mandateText = null,
                        ),
                    )
                )
            )

            val currentScreenState =
                viewModel.currentScreenState.value

            assertThat(
                currentScreenState
            ).isInstanceOf<
                USBankAccountFormScreenState.SavedAccount
                >()
        }

    @Test
    fun `Test defaults are used when not collecting fields if they are attached`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(
                defaultArgs.copy(
                    formArgs = defaultArgs.formArgs.copy(
                        billingDetails = PaymentSheet.BillingDetails(
                            name = CUSTOMER_NAME,
                            email = CUSTOMER_EMAIL,
                            phone = CUSTOMER_PHONE_COUNTRY_CODE + CUSTOMER_PHONE,
                            address = CUSTOMER_ADDRESS,
                        ),
                        billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                            attachDefaultsToPaymentMethod = true,
                            name = CollectionMode.Never,
                            email = CollectionMode.Never,
                            phone = CollectionMode.Never,
                            address = AddressCollectionMode.Never,
                        )
                    ),
                ),
            )

            assertThat(viewModel.name.value)
                .isEqualTo(CUSTOMER_NAME)
            assertThat(viewModel.email.value)
                .isEqualTo(CUSTOMER_EMAIL)
            assertThat(viewModel.phone.value)
                .isEqualTo(CUSTOMER_PHONE)
            assertThat(viewModel.phoneController.getCountryCode()).isEqualTo(CUSTOMER_COUNTRY)
            assertThat(viewModel.address.value)
                .isEqualTo(CUSTOMER_ADDRESS.asAddressModel())
            assertThat(viewModel.requiredFields.value).isTrue()
        }

    @Test
    fun `Test defaults are only used for fields that are being collected`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(
                defaultArgs.copy(
                    formArgs = defaultArgs.formArgs.copy(
                        billingDetails = PaymentSheet.BillingDetails(
                            name = CUSTOMER_NAME,
                            email = CUSTOMER_EMAIL,
                            phone = CUSTOMER_PHONE,
                            address = CUSTOMER_ADDRESS,
                        ),
                        billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                            attachDefaultsToPaymentMethod = false,
                            name = CollectionMode.Always,
                            email = CollectionMode.Always,
                            phone = CollectionMode.Never,
                            address = AddressCollectionMode.Never,
                        )
                    ),
                ),
            )

            assertThat(viewModel.name.value)
                .isEqualTo(CUSTOMER_NAME)
            assertThat(viewModel.email.value)
                .isEqualTo(CUSTOMER_EMAIL)
            assertThat(viewModel.phone.value).isNull()
            assertThat(viewModel.address.value).isNull()
            assertThat(viewModel.requiredFields.value).isTrue()
        }

    @Test
    fun `Test defaults are not used when not collecting fields if not attached`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(
                defaultArgs.copy(
                    formArgs = defaultArgs.formArgs.copy(
                        billingDetails = PaymentSheet.BillingDetails(
                            name = CUSTOMER_NAME,
                            email = CUSTOMER_EMAIL,
                            phone = CUSTOMER_PHONE,
                            address = CUSTOMER_ADDRESS,
                        ),
                        billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                            attachDefaultsToPaymentMethod = false,
                            name = CollectionMode.Automatic,
                            email = CollectionMode.Automatic,
                            phone = CollectionMode.Never,
                            address = AddressCollectionMode.Never,
                        )
                    ),
                ),
            )

            assertThat(viewModel.name.value)
                .isEqualTo("Jenny Rose")
            assertThat(viewModel.email.value)
                .isEqualTo("email@email.com")
            assertThat(viewModel.phone.value).isNull()
            assertThat(viewModel.address.value).isNull()
            assertThat(viewModel.requiredFields.value).isTrue()
        }

    @Test
    fun `Test all collected fields are required`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(
                defaultArgs.copy(
                    formArgs = defaultArgs.formArgs.copy(
                        billingDetails = PaymentSheet.BillingDetails(
                            name = CUSTOMER_NAME,
                            email = CUSTOMER_EMAIL,
                        ),
                        billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                            attachDefaultsToPaymentMethod = true,
                            name = CollectionMode.Always,
                            email = CollectionMode.Always,
                            phone = CollectionMode.Always,
                            address = AddressCollectionMode.Full,
                        )
                    ),
                ),
            )

            assertThat(viewModel.name.value)
                .isEqualTo(CUSTOMER_NAME)
            assertThat(viewModel.email.value)
                .isEqualTo(CUSTOMER_EMAIL)
            assertThat(viewModel.phone.value).isNull()
            assertThat(viewModel.address.value).isNull()
            assertThat(viewModel.requiredFields.value).isFalse()
        }

    @Test
    fun `Test phone country changes with country`() = runTest(UnconfinedTestDispatcher()) {
        val billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            name = CollectionMode.Always,
            email = CollectionMode.Always,
            phone = CollectionMode.Always,
            address = AddressCollectionMode.Full,
            attachDefaultsToPaymentMethod = false,
        )

        val viewModel = createViewModel(
            defaultArgs.copy(
                formArgs = defaultArgs.formArgs.copy(
                    billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
                ),
            )
        )

        viewModel.addressElement.countryElement.controller.onRawValueChange("CA")
        assertThat(viewModel.phoneController.countryDropdownController.rawFieldValue.value).isEqualTo("CA")
    }

    @Test
    fun `Doesn't save for future use by default`() = runTest {
        val viewModel = createViewModel(
            args = defaultArgs.copy(
                formArgs = defaultArgs.formArgs,
                showCheckbox = true,
            ),
        )
        assertThat(viewModel.saveForFutureUse.value).isFalse()
    }

    @Test
    fun `Produces correct lastTextFieldIdentifier for default config`() = runTest {
        val viewModel = createViewModel(
            args = defaultArgs.copy(
                formArgs = defaultArgs.formArgs,
                showCheckbox = true,
            ),
        )

        viewModel.lastTextFieldIdentifier.test {
            assertThat(awaitItem()).isEqualTo(IdentifierSpec.Email)
        }
    }

    @Test
    fun `Produces correct lastTextFieldIdentifier when collecting phone number`() = runTest {
        val billingDetailsConfig = PaymentSheet.BillingDetailsCollectionConfiguration(
            name = CollectionMode.Automatic,
            email = CollectionMode.Automatic,
            phone = CollectionMode.Always,
            address = AddressCollectionMode.Never,
        )

        val viewModel = createViewModel(
            args = defaultArgs.copy(
                formArgs = defaultArgs.formArgs.copy(
                    billingDetailsCollectionConfiguration = billingDetailsConfig,
                ),
                showCheckbox = true,
            ),
        )

        viewModel.lastTextFieldIdentifier.test {
            assertThat(awaitItem()).isEqualTo(IdentifierSpec.Phone)
        }
    }

    @Test
    fun `Produces correct lastTextFieldIdentifier when collecting address`() = runTest {
        val billingDetailsConfig = PaymentSheet.BillingDetailsCollectionConfiguration(
            name = CollectionMode.Automatic,
            email = CollectionMode.Automatic,
            phone = CollectionMode.Always,
            address = AddressCollectionMode.Full,
            attachDefaultsToPaymentMethod = true,
        )

        val viewModel = createViewModel(
            args = defaultArgs.copy(
                formArgs = defaultArgs.formArgs.copy(
                    billingDetailsCollectionConfiguration = billingDetailsConfig,
                    billingDetails = PaymentSheet.BillingDetails(
                        name = "My myself and I",
                        email = "myself@me.com",
                    ),
                ),
                showCheckbox = true,
            ),
        )

        viewModel.lastTextFieldIdentifier.test {
            assertThat(awaitItem()).isEqualTo(IdentifierSpec.PostalCode)
        }
    }

    @Test
    fun `Produces correct lastTextFieldIdentifier when only address`() = runTest {
        val billingDetailsConfig = PaymentSheet.BillingDetailsCollectionConfiguration(
            name = CollectionMode.Never,
            email = CollectionMode.Never,
            phone = CollectionMode.Never,
            address = AddressCollectionMode.Full,
            attachDefaultsToPaymentMethod = true,
        )

        val viewModel = createViewModel(
            args = defaultArgs.copy(
                formArgs = defaultArgs.formArgs.copy(
                    billingDetailsCollectionConfiguration = billingDetailsConfig,
                    billingDetails = PaymentSheet.BillingDetails(
                        name = "My myself and I",
                        email = "myself@me.com",
                    ),
                ),
                showCheckbox = true,
            ),
        )

        viewModel.lastTextFieldIdentifier.test {
            assertThat(awaitItem()).isEqualTo(IdentifierSpec.PostalCode)
        }
    }

    @Test
    fun `Produces correct lastTextFieldIdentifier when collecting no fields`() = runTest {
        val billingDetailsConfig = PaymentSheet.BillingDetailsCollectionConfiguration(
            name = CollectionMode.Never,
            email = CollectionMode.Never,
            phone = CollectionMode.Never,
            address = AddressCollectionMode.Never,
            attachDefaultsToPaymentMethod = true,
        )

        val viewModel = createViewModel(
            args = defaultArgs.copy(
                formArgs = defaultArgs.formArgs.copy(
                    billingDetailsCollectionConfiguration = billingDetailsConfig,
                    billingDetails = PaymentSheet.BillingDetails(
                        name = "My myself and I",
                        email = "myself@me.com",
                    ),
                ),
                showCheckbox = true,
            ),
        )

        viewModel.lastTextFieldIdentifier.test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `Launches collect bank account for deferred payment screen when deferred payment`() = runTest {
        val viewModel = createViewModel(
            defaultArgs.copy(
                clientSecret = null,
                linkMode = LinkMode.LinkPaymentMethod,
            )
        )

        viewModel.collectBankAccountLauncher = mockCollectBankAccountLauncher

        val currentScreenState =
            viewModel.currentScreenState.value

        assertThat(
            currentScreenState
        ).isInstanceOf<USBankAccountFormScreenState.BillingDetailsCollection>()

        viewModel.handlePrimaryButtonClick(
            currentScreenState as USBankAccountFormScreenState.BillingDetailsCollection
        )

        verify(mockCollectBankAccountLauncher).presentWithDeferredPayment(
            publishableKey = any(),
            stripeAccountId = any(),
            configuration = eq(
                CollectBankAccountConfiguration.USBankAccountInternal(
                    name = "Jenny Rose",
                    email = "email@email.com",
                    elementsSessionContext = ElementsSessionContext(
                        initializationMode = ElementsSessionContext.InitializationMode.DeferredIntent,
                        amount = 5099,
                        currency = "usd",
                        linkMode = LinkMode.LinkPaymentMethod,
                    ),
                )
            ),
            elementsSessionId = any(),
            customerId = anyOrNull(),
            onBehalfOf = any(),
            amount = any(),
            currency = any()
        )
    }

    @Test
    fun `Launches collect bank account for deferred setup screen when deferred setup`() = runTest {
        val viewModel = createViewModel(
            defaultArgs.copy(
                clientSecret = null,
                isPaymentFlow = false,
                linkMode = LinkMode.LinkPaymentMethod,
                formArgs = defaultArgs.formArgs.copy(
                    amount = null,
                )
            )
        )

        viewModel.collectBankAccountLauncher = mockCollectBankAccountLauncher

        val currentScreenState =
            viewModel.currentScreenState.value

        assertThat(
            currentScreenState
        ).isInstanceOf<USBankAccountFormScreenState.BillingDetailsCollection>()

        viewModel.handlePrimaryButtonClick(
            currentScreenState as USBankAccountFormScreenState.BillingDetailsCollection
        )

        verify(mockCollectBankAccountLauncher).presentWithDeferredSetup(
            publishableKey = any(),
            stripeAccountId = any(),
            configuration = eq(
                CollectBankAccountConfiguration.USBankAccountInternal(
                    name = "Jenny Rose",
                    email = "email@email.com",
                    elementsSessionContext = ElementsSessionContext(
                        initializationMode = ElementsSessionContext.InitializationMode.DeferredIntent,
                        amount = null,
                        currency = null,
                        linkMode = LinkMode.LinkPaymentMethod,
                    ),
                )
            ),
            elementsSessionId = any(),
            customerId = anyOrNull(),
            onBehalfOf = any(),
        )
    }

    @Test
    fun `When form destroyed, collect bank account result is null and screen is not reset`() = runTest {
        val viewModel = createViewModel(
            defaultArgs.copy(
                clientSecret = null,
                isPaymentFlow = false
            )
        )

        viewModel.result.test {
            viewModel.handleCollectBankAccountResult(
                result = mockVerifiedBankAccount()
            )

            viewModel.onDestroy()

            assertThat(awaitItem()).isNull()

            val currentScreenState =
                viewModel.currentScreenState.value

            assertThat(currentScreenState)
                .isInstanceOf<USBankAccountFormScreenState.MandateCollection>()
        }
    }

    @Test
    fun `When the primary button is pressed, the primary button state moves to processing`() = runTest {
        val viewModel = createViewModel()

        viewModel.currentScreenState.test {
            assertThat(awaitItem().isProcessing)
                .isFalse()

            viewModel.handlePrimaryButtonClick(viewModel.currentScreenState.value)

            assertThat(awaitItem().isProcessing)
                .isTrue()
        }
    }

    @Test
    fun `When collect bank account is returned from FC SDK, the result is emitted`() = runTest {
        val viewModel = createViewModel()

        viewModel.collectBankAccountResult.test {
            val verifiedAccount = mockVerifiedBankAccount()
            viewModel.handleCollectBankAccountResult(
                result = verifiedAccount
            )

            assertThat(awaitItem())
                .isEqualTo(verifiedAccount)

            viewModel.handleCollectBankAccountResult(
                result = CollectBankAccountResultInternal.Cancelled
            )

            assertThat(awaitItem())
                .isEqualTo(CollectBankAccountResultInternal.Cancelled)
            // Reset was called, so the result should be null
            assertThat(awaitItem())
                .isNull()

            val failure = CollectBankAccountResultInternal.Failed(
                IllegalArgumentException("Failed")
            )
            viewModel.handleCollectBankAccountResult(
                result = failure
            )

            assertThat(awaitItem())
                .isEqualTo(failure)
            // Reset was called, so the result should be null
            assertThat(awaitItem())
                .isNull()
        }
    }

    @Test
    fun `When the view model is reset, collect bank account result should be null`() = runTest {
        val viewModel = createViewModel()

        viewModel.collectBankAccountResult.test {
            val verifiedAccount = mockVerifiedBankAccount()
            viewModel.handleCollectBankAccountResult(
                result = verifiedAccount
            )

            assertThat(awaitItem())
                .isEqualTo(verifiedAccount)

            viewModel.reset()

            assertThat(awaitItem())
                .isEqualTo(null)
        }
    }

    @Test
    fun `Should be reset after confirming bank account and attempting to reset`() = runTest {
        val viewModel = createViewModel()

        viewModel.currentScreenState.test {
            assertThat(awaitItem())
                .isInstanceOf<USBankAccountFormScreenState.BillingDetailsCollection>()

            val verifiedAccount = mockVerifiedBankAccount()
            viewModel.handleCollectBankAccountResult(
                result = verifiedAccount
            )

            assertThat(awaitItem())
                .isInstanceOf<USBankAccountFormScreenState.MandateCollection>()

            viewModel.handlePrimaryButtonClick(viewModel.currentScreenState.value)
            viewModel.reset()

            assertThat(expectMostRecentItem())
                .isInstanceOf<USBankAccountFormScreenState.BillingDetailsCollection>()
        }
    }

    @Test
    fun `Should not be reset after attempting to reset on the billing details screen`() = runTest {
        val viewModel = createViewModel()

        viewModel.currentScreenState.test {
            assertThat(awaitItem())
                .isInstanceOf<USBankAccountFormScreenState.BillingDetailsCollection>()

            viewModel.handlePrimaryButtonClick(viewModel.currentScreenState.value)

            assertThat(awaitItem())
                .isInstanceOf<USBankAccountFormScreenState.BillingDetailsCollection>()

            viewModel.reset()

            assertThat(awaitItem())
                .isInstanceOf<USBankAccountFormScreenState.BillingDetailsCollection>()
        }
    }

    @Test
    fun `customerRequestedSave equals RequestReuse when showCheckbox and saveForFutureUse`() {
        assertThat(
            customerRequestedSave(
                showCheckbox = true,
                saveForFutureUse = true
            )
        ).isEqualTo(PaymentSelection.CustomerRequestedSave.RequestReuse)
    }

    @Test
    fun `customerRequestedSave equals RequestNoReuse when showCheckbox and not saveForFutureUse`() {
        assertThat(
            customerRequestedSave(
                showCheckbox = true,
                saveForFutureUse = false
            )
        ).isEqualTo(PaymentSelection.CustomerRequestedSave.RequestNoReuse)
    }

    @Test
    fun `customerRequestedSave equals NoRequest when not showCheckbox and not saveForFutureUse`() {
        assertThat(
            customerRequestedSave(
                showCheckbox = false,
                saveForFutureUse = false
            )
        ).isEqualTo(PaymentSelection.CustomerRequestedSave.NoRequest)
    }

    @Test
    fun `Uses CollectBankAccountLauncher for ACH when not in Instant Debits flow`() {
        val viewModel = createViewModel().apply {
            this.collectBankAccountLauncher = mockCollectBankAccountLauncher
        }

        viewModel.nameController.onValueChange("Some Name")
        viewModel.emailController.onValueChange("email@email.com")

        val currentState = viewModel.currentScreenState.value
        viewModel.handlePrimaryButtonClick(currentState)

        verify(mockCollectBankAccountLauncher).presentWithPaymentIntent(
            publishableKey = any(),
            stripeAccountId = anyOrNull(),
            clientSecret = any(),
            configuration = eq(
                CollectBankAccountConfiguration.USBankAccountInternal(
                    name = "Some Name",
                    email = "email@email.com",
                    elementsSessionContext = ElementsSessionContext(
                        initializationMode = ElementsSessionContext.InitializationMode.PaymentIntent("id_12345"),
                        amount = 5099,
                        currency = "usd",
                        linkMode = null,
                    ),
                )
            ),
        )
    }

    @Test
    fun `Uses CollectBankAccountLauncher for Instant Debits when in Instant Debits flow`() {
        val viewModel = createViewModel(
            args = defaultArgs.copy(
                instantDebits = true,
                linkMode = LinkMode.LinkCardBrand,
            ),
        ).apply {
            this.collectBankAccountLauncher = mockCollectBankAccountLauncher
        }

        viewModel.emailController.onValueChange("email@email.com")

        val currentState = viewModel.currentScreenState.value
        viewModel.handlePrimaryButtonClick(currentState)

        verify(mockCollectBankAccountLauncher).presentWithPaymentIntent(
            publishableKey = any(),
            stripeAccountId = anyOrNull(),
            clientSecret = any(),
            configuration = eq(
                CollectBankAccountConfiguration.InstantDebits(
                    email = "email@email.com",
                    elementsSessionContext = ElementsSessionContext(
                        initializationMode = ElementsSessionContext.InitializationMode.PaymentIntent("id_12345"),
                        amount = 5099,
                        currency = "usd",
                        linkMode = LinkMode.LinkCardBrand,
                    ),
                )
            ),
        )
    }

    @Test
    fun `Produces correct mandate text when not using microdeposits verification`() = runTest {
        val viewModel = createViewModel()

        val expectedResult = USBankAccountTextBuilder.buildMandateText(
            merchantName = MERCHANT_NAME,
            isSaveForFutureUseSelected = false,
            isSetupFlow = false,
            isInstantDebits = false,
        )

        viewModel.currentScreenState.test {
            assertThat(awaitItem()).isInstanceOf<USBankAccountFormScreenState.BillingDetailsCollection>()

            val verifiedAccount = mockVerifiedBankAccount()
            viewModel.handleCollectBankAccountResult(verifiedAccount)

            val mandateCollectionViewState = awaitItem()
            assertThat(mandateCollectionViewState.mandateText).isEqualTo(expectedResult)
        }
    }

    @Test
    fun `Produces correct mandate text when using microdeposits verification`() = runTest {
        val viewModel = createViewModel()

        val expectedResult = USBankAccountTextBuilder.buildMandateAndMicrodepositsText(
            merchantName = MERCHANT_NAME,
            isVerifyingMicrodeposits = true,
            isSaveForFutureUseSelected = false,
            isSetupFlow = false,
            isInstantDebits = false,
        )

        viewModel.currentScreenState.test {
            assertThat(awaitItem()).isInstanceOf<USBankAccountFormScreenState.BillingDetailsCollection>()

            val unverifiedAccount = mockUnverifiedBankAccount()
            viewModel.handleCollectBankAccountResult(unverifiedAccount)

            val mandateCollectionViewState = awaitItem()
            assertThat(mandateCollectionViewState.mandateText).isEqualTo(expectedResult)
        }
    }

    @Test
    fun `allowRedisplay returns Unspecified when save behavior is Legacy, not setting up, and no checkbox`() =
        testAllowRedisplay(
            showCheckbox = false,
            shouldSave = true,
            hasIntentForSetup = false,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
            expectedAllowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        )

    @Test
    fun `allowRedisplay returns Unspecified when save behavior is Legacy, not setting up, and should not save`() =
        testAllowRedisplay(
            showCheckbox = true,
            shouldSave = false,
            hasIntentForSetup = false,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
            expectedAllowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        )

    @Test
    fun `allowRedisplay returns Unspecified when save behavior is Legacy, not setting up, and should save`() =
        testAllowRedisplay(
            showCheckbox = true,
            shouldSave = true,
            hasIntentForSetup = false,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
            expectedAllowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        )

    @Test
    fun `allowRedisplay returns Unspecified when save behavior is Legacy, setting up, and no checkbox`() =
        testAllowRedisplay(
            showCheckbox = false,
            shouldSave = true,
            hasIntentForSetup = true,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
            expectedAllowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        )

    @Test
    fun `allowRedisplay returns Unspecified when save behavior is Legacy, setting up, and should not save`() =
        testAllowRedisplay(
            showCheckbox = true,
            shouldSave = false,
            hasIntentForSetup = true,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
            expectedAllowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        )

    @Test
    fun `allowRedisplay returns Unspecified when save behavior is Legacy, setting up, and should save`() =
        testAllowRedisplay(
            showCheckbox = true,
            shouldSave = true,
            hasIntentForSetup = true,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
            expectedAllowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        )

    @Test
    fun `allowRedisplay returns Unspecified when save behavior is Enabled, not setting up, and no checkbox`() =
        testAllowRedisplay(
            showCheckbox = false,
            shouldSave = true,
            hasIntentForSetup = true,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled,
            expectedAllowRedisplay = PaymentMethod.AllowRedisplay.LIMITED,
        )

    @Test
    fun `allowRedisplay returns Unspecified when save behavior is Enabled, not setting up, and should not save`() =
        testAllowRedisplay(
            showCheckbox = true,
            shouldSave = false,
            hasIntentForSetup = false,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled,
            expectedAllowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        )

    @Test
    fun `allowRedisplay returns Always when save behavior is Enabled, not setting up, and should save`() =
        testAllowRedisplay(
            showCheckbox = true,
            shouldSave = true,
            hasIntentForSetup = false,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled,
            expectedAllowRedisplay = PaymentMethod.AllowRedisplay.ALWAYS,
        )

    @Test
    fun `allowRedisplay returns Limited when save behavior is Enabled, setting up, and no checkbox`() =
        testAllowRedisplay(
            showCheckbox = false,
            shouldSave = true,
            hasIntentForSetup = true,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled,
            expectedAllowRedisplay = PaymentMethod.AllowRedisplay.LIMITED,
        )

    @Test
    fun `allowRedisplay returns Limited when save behavior is Enabled, setting up, and should not save`() =
        testAllowRedisplay(
            showCheckbox = true,
            shouldSave = false,
            hasIntentForSetup = true,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled,
            expectedAllowRedisplay = PaymentMethod.AllowRedisplay.LIMITED,
        )

    @Test
    fun `allowRedisplay returns Always when save behavior is Enabled, setting up, and should save`() =
        testAllowRedisplay(
            showCheckbox = true,
            shouldSave = true,
            hasIntentForSetup = true,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled,
            expectedAllowRedisplay = PaymentMethod.AllowRedisplay.ALWAYS,
        )

    @Test
    fun `allowRedisplay returns Unspecified when save behavior is Disabled and not setting up`() =
        testAllowRedisplay(
            showCheckbox = false,
            shouldSave = true,
            hasIntentForSetup = false,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Disabled(
                overrideAllowRedisplay = null,
            ),
            expectedAllowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        )

    @Test
    fun `allowRedisplay returns Limited when save behavior is Disabled and setting up`() =
        testAllowRedisplay(
            showCheckbox = false,
            shouldSave = true,
            hasIntentForSetup = true,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Disabled(
                overrideAllowRedisplay = null,
            ),
            expectedAllowRedisplay = PaymentMethod.AllowRedisplay.LIMITED,
        )

    @Test
    fun `allowRedisplay returns Always when save behavior is Disabled, setting up, and has redisplay override`() =
        testAllowRedisplay(
            showCheckbox = false,
            shouldSave = true,
            hasIntentForSetup = true,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Disabled(
                overrideAllowRedisplay = PaymentMethod.AllowRedisplay.ALWAYS,
            ),
            expectedAllowRedisplay = PaymentMethod.AllowRedisplay.ALWAYS,
        )

    private fun testAllowRedisplay(
        showCheckbox: Boolean,
        shouldSave: Boolean,
        hasIntentForSetup: Boolean,
        paymentMethodSaveConsentBehavior: PaymentMethodSaveConsentBehavior,
        expectedAllowRedisplay: PaymentMethod.AllowRedisplay,
    ) {
        testAllowRedisplay(
            isInstantDebits = true,
            showCheckbox = showCheckbox,
            shouldSave = shouldSave,
            hasIntentForSetup = hasIntentForSetup,
            paymentMethodSaveConsentBehavior = paymentMethodSaveConsentBehavior,
            expectedAllowRedisplay = expectedAllowRedisplay,
        )

        testAllowRedisplay(
            isInstantDebits = false,
            showCheckbox = showCheckbox,
            shouldSave = shouldSave,
            hasIntentForSetup = hasIntentForSetup,
            paymentMethodSaveConsentBehavior = paymentMethodSaveConsentBehavior,
            expectedAllowRedisplay = expectedAllowRedisplay,
        )
    }

    private fun testAllowRedisplay(
        isInstantDebits: Boolean,
        showCheckbox: Boolean,
        shouldSave: Boolean,
        hasIntentForSetup: Boolean,
        paymentMethodSaveConsentBehavior: PaymentMethodSaveConsentBehavior,
        expectedAllowRedisplay: PaymentMethod.AllowRedisplay,
    ) = runTest {
        val viewModel = createViewModel(
            defaultArgs.copy(
                showCheckbox = showCheckbox,
                formArgs = defaultArgs.formArgs.copy(
                    hasIntentToSetup = hasIntentForSetup,
                    paymentMethodSaveConsentBehavior = paymentMethodSaveConsentBehavior,
                )
            )
        )

        viewModel.result.test {
            viewModel.handleCollectBankAccountResult(mockVerifiedBankAccount())

            viewModel.saveForFutureUseElement.controller.onValueChange(shouldSave)

            viewModel.handlePrimaryButtonClick(
                USBankAccountFormScreenState.MandateCollection(
                    intentId = "pm_1",
                    bankName = "Test bank",
                    last4 = "1456",
                    mandateText = resolvableString("Save"),
                    primaryButtonText = resolvableString("Confirm"),
                    resultIdentifier = when (isInstantDebits) {
                        true -> USBankAccountFormScreenState.ResultIdentifier.PaymentMethod(id = "pm_1")
                        false -> USBankAccountFormScreenState.ResultIdentifier.Session(id = "session_1")
                    }
                )
            )

            assertThat(awaitItem()?.paymentMethodCreateParams?.toParamMap()).containsEntry(
                "allow_redisplay",
                when (expectedAllowRedisplay) {
                    PaymentMethod.AllowRedisplay.UNSPECIFIED -> "unspecified"
                    PaymentMethod.AllowRedisplay.LIMITED -> "limited"
                    PaymentMethod.AllowRedisplay.ALWAYS -> "always"
                }
            )
        }
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
            lazyPaymentConfig = { paymentConfiguration },
            savedStateHandle = savedStateHandle,
        )
    }

    private fun mockUnverifiedBankAccount(): CollectBankAccountResultInternal.Completed {
        val paymentIntent = mock<PaymentIntent>()
        val financialConnectionsSession = mock<FinancialConnectionsSession>()
        whenever(paymentIntent.id).thenReturn(defaultArgs.clientSecret)
        whenever(financialConnectionsSession.id).thenReturn("123")
        whenever(financialConnectionsSession.paymentAccount).thenReturn(
            BankAccount(
                id = "123",
                last4 = "4567",
                bankName = "Test",
                routingNumber = "123"
            )
        )

        return CollectBankAccountResultInternal.Completed(
            CollectBankAccountResponseInternal(
                intent = paymentIntent,
                usBankAccountData = CollectBankAccountResponseInternal.USBankAccountData(
                    financialConnectionsSession = financialConnectionsSession
                ),
                instantDebitsData = null
            )
        )
    }

    private fun mockVerifiedBankAccount(): CollectBankAccountResultInternal.Completed {
        val paymentIntent = mock<PaymentIntent>()
        val financialConnectionsSession = mock<FinancialConnectionsSession>()
        whenever(paymentIntent.id).thenReturn(defaultArgs.clientSecret)
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

        return CollectBankAccountResultInternal.Completed(
            CollectBankAccountResponseInternal(
                intent = paymentIntent,
                usBankAccountData = CollectBankAccountResponseInternal.USBankAccountData(
                    financialConnectionsSession = financialConnectionsSession
                ),
                instantDebitsData = null
            )
        )
    }

    private companion object {
        const val MERCHANT_NAME = "merchantName"
        const val CUSTOMER_NAME = "Jenny Rose"
        const val CUSTOMER_EMAIL = "email@email.com"
        const val STRIPE_ACCOUNT_ID = "stripe_account_id"
        const val CUSTOMER_COUNTRY = "US"
        const val CUSTOMER_PHONE_COUNTRY_CODE = "+1"
        const val CUSTOMER_PHONE = "3105551234"
        val CUSTOMER_ADDRESS = PaymentSheet.Address(
            line1 = "123 Main Street",
            line2 = "Apt 456",
            city = "San Francisco",
            state = "CA",
            postalCode = "94111",
            country = "US",
        )
    }
}
