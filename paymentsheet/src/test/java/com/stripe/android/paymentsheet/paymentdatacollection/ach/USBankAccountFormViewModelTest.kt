package com.stripe.android.paymentsheet.paymentdatacollection.ach

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.financialconnections.ElementsSessionContext
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.IS_PAYMENT_METHOD_SET_AS_DEFAULT_ENABLED_DEFAULT_VALUE
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResponseInternal
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode
import com.stripe.android.paymentsheet.addresselement.TestAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSelection.CustomerRequestedSave
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.SetAsDefaultPaymentMethodElement
import com.stripe.android.uicore.elements.AutocompleteAddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.utils.BankFormScreenStateFactory
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class USBankAccountFormViewModelTest {

    private val defaultArgs = USBankAccountFormViewModel.Args(
        instantDebits = false,
        incentive = null,
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
        setAsDefaultPaymentMethodEnabled = false,
        setAsDefaultMatchesSaveForFutureUse = false,
        financialConnectionsAvailability = FinancialConnectionsAvailability.Full,
        termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        sellerBusinessName = null,
        forceSetupFutureUseBehavior = false,
        clientAttributionMetadata = null,
    )

    private val mockCollectBankAccountLauncher = mock<CollectBankAccountLauncher>()
    private val savedStateHandle = SavedStateHandle()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

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
            viewModel.handlePrimaryButtonClick()
            verify(mockCollectBankAccountLauncher).presentWithPaymentIntent(any(), any(), any(), any())
        }

    @Test
    fun `Transitions to correct screen state when collecting an unverified bank account in complete flow`() = runTest {
        val viewModel = createViewModel()

        viewModel.linkedAccount.test {
            skipItems(1)
            viewModel.handleCollectBankAccountResult(mockManuallyEnteredBankAccount(usesMicrodeposits = true))

            val screenState = awaitItem()?.screenState
            assertThat(screenState?.linkedBankAccount?.isVerifyingWithMicrodeposits).isTrue()
        }
    }

    @Test
    fun `Transitions to correct screen state when collecting a verified bank account in complete flow`() = runTest {
        val viewModel = createViewModel()

        viewModel.linkedAccount.test {
            skipItems(1)
            viewModel.handleCollectBankAccountResult(mockVerifiedBankAccount())
            assertThat(awaitItem()?.screenState?.linkedBankAccount).isNotNull()
        }
    }

    @Test
    fun `Transitions to correct screen state when collecting an unverified bank account in custom flow`() = runTest {
        val viewModel = createViewModel(defaultArgs.copy(isCompleteFlow = false))
        val bankAccount = mockManuallyEnteredBankAccount(usesMicrodeposits = true)

        viewModel.linkedAccount.test {
            skipItems(1)
            viewModel.handleCollectBankAccountResult(bankAccount)

            val screenState = awaitItem()?.screenState
            assertThat(screenState?.linkedBankAccount?.isVerifyingWithMicrodeposits).isTrue()
        }
    }

    @Test
    fun `Transitions to correct screen state when collecting a verified bank account in custom flow`() = runTest {
        val viewModel = createViewModel(defaultArgs.copy(isCompleteFlow = false))
        val bankAccount = mockVerifiedBankAccount()

        viewModel.linkedAccount.test {
            skipItems(1)
            viewModel.handleCollectBankAccountResult(bankAccount)

            val screenState = awaitItem()?.screenState
            assertThat(screenState?.linkedBankAccount?.isVerifyingWithMicrodeposits).isFalse()
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

        viewModel.linkedAccount.test {
            skipItems(1)
            viewModel.handleCollectBankAccountResult(bankAccount)

            assertThat(awaitItem()?.paymentMethodOptionsParams).isEqualTo(
                PaymentMethodOptionsParams.USBankAccount(
                    setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.Blank
                )
            )
        }
    }

    @Test
    fun `when hasIntentToSetup, paymentMethodOptionsParams does not send SFU`() = runTest {
        val viewModel = createViewModel(
            defaultArgs.copy(
                formArgs = defaultArgs.formArgs.copy(
                    hasIntentToSetup = true
                ),
                showCheckbox = true,
            )
        )
        val bankAccount = mockVerifiedBankAccount()

        viewModel.linkedAccount.test {
            skipItems(1)
            viewModel.handleCollectBankAccountResult(bankAccount)

            assertThat(awaitItem()?.paymentMethodOptionsParams).isEqualTo(
                PaymentMethodOptionsParams.USBankAccount(
                    setupFutureUsage = null
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

            viewModel.handlePrimaryButtonClick()

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
    fun `Correctly creates selection when default address`() = runTest {
        val customerAddress = CUSTOMER_ADDRESS.asAddressModel()
        val viewModel = createViewModel(
            defaultArgs.copy(
                formArgs = defaultArgs.formArgs.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        attachDefaultsToPaymentMethod = true,
                        name = CollectionMode.Always,
                        email = CollectionMode.Always,
                        phone = CollectionMode.Always,
                        address = AddressCollectionMode.Full,
                    ),
                    billingDetails = PaymentSheet.BillingDetails(
                        name = CUSTOMER_NAME,
                        email = CUSTOMER_EMAIL,
                        phone = CUSTOMER_PHONE,
                        address = CUSTOMER_ADDRESS,
                    )
                ),
                savedPaymentMethod = null,
            )
        )

        viewModel.handleCollectBankAccountResult(mockManuallyEnteredBankAccount(usesMicrodeposits = true))

        viewModel.linkedAccount.test {
            val paymentSelection = awaitItem()

            assertThat(paymentSelection?.input).isNotNull()

            val input = requireNotNull(paymentSelection?.input)

            assertThat(input.name).isEqualTo(CUSTOMER_NAME)
            assertThat(input.email).isEqualTo(CUSTOMER_EMAIL)
            assertThat(input.phone).isEqualTo(CUSTOMER_PHONE)
            assertThat(input.address).isEqualTo(customerAddress)

            assertThat(paymentSelection?.paymentMethodCreateParams).isNotNull()

            val paymentMethodCreateParams = requireNotNull(paymentSelection?.paymentMethodCreateParams)

            assertThat(paymentMethodCreateParams.billingDetails).isNotNull()

            val billingDetails = requireNotNull(paymentMethodCreateParams.billingDetails)

            assertThat(billingDetails.name).isEqualTo(CUSTOMER_NAME)
            assertThat(billingDetails.email).isEqualTo(CUSTOMER_EMAIL)
            assertThat(billingDetails.phone).isEqualTo(CUSTOMER_PHONE)
            assertThat(billingDetails.address).isEqualTo(customerAddress)
        }
    }

    @Test
    fun `Correctly creates selection when no default address`() = runTest {
        val viewModel = createViewModel(
            defaultArgs.copy(
                formArgs = defaultArgs.formArgs.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        attachDefaultsToPaymentMethod = true,
                        name = CollectionMode.Always,
                        email = CollectionMode.Always,
                        phone = CollectionMode.Always,
                        address = AddressCollectionMode.Full,
                    ),
                    billingDetails = null
                ),
                savedPaymentMethod = null,
            )
        )

        val customerAddress = CUSTOMER_ADDRESS.asAddressModel()

        viewModel.nameController.onValueChange(CUSTOMER_NAME)
        viewModel.emailController.onValueChange(CUSTOMER_EMAIL)
        viewModel.phoneController.onValueChange(CUSTOMER_PHONE.removePrefix("+1"))
        viewModel.addressElement.addressController.test {
            val controller = awaitItem()

            controller.fieldsFlowable.test {
                val formFieldValues = customerAddress.asFormFieldValues()

                awaitItem().forEach { field ->
                    field.setRawValue(formFieldValues)
                }

                cancelAndIgnoreRemainingEvents()
            }
        }

        viewModel.handleCollectBankAccountResult(mockManuallyEnteredBankAccount(usesMicrodeposits = true))

        viewModel.linkedAccount.test {
            val paymentSelection = awaitItem()

            assertThat(paymentSelection?.input).isNotNull()

            val input = requireNotNull(paymentSelection?.input)

            assertThat(input.name).isEqualTo(CUSTOMER_NAME)
            assertThat(input.email).isEqualTo(CUSTOMER_EMAIL)
            assertThat(input.phone).isEqualTo(CUSTOMER_PHONE)
            assertThat(input.address).isEqualTo(customerAddress)

            assertThat(paymentSelection?.paymentMethodCreateParams).isNotNull()

            val paymentMethodCreateParams = requireNotNull(paymentSelection?.paymentMethodCreateParams)

            assertThat(paymentMethodCreateParams.billingDetails).isNotNull()

            val billingDetails = requireNotNull(paymentMethodCreateParams.billingDetails)

            assertThat(billingDetails.name).isEqualTo(CUSTOMER_NAME)
            assertThat(billingDetails.email).isEqualTo(CUSTOMER_EMAIL)
            assertThat(billingDetails.phone).isEqualTo(CUSTOMER_PHONE)
            assertThat(billingDetails.address).isEqualTo(customerAddress)
        }
    }

    @Test
    fun `Correctly restores input when re-opening screen`() = runTest {
        val input = PaymentSelection.New.USBankAccount.Input(
            name = "Some One",
            email = "someone@email.com",
            phone = "+11112223456",
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
                    label = "Test",
                    iconResource = 0,
                    paymentMethodCreateParams = mock(),
                    customerRequestedSave = mock(),
                    input = input,
                    instantDebits = null,
                    screenState = BankFormScreenStateFactory.createWithSession("session_1234"),
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
            phone = "+11112223456",
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
                    label = "Test",
                    iconResource = 0,
                    paymentMethodCreateParams = mock(),
                    customerRequestedSave = mock(),
                    input = input,
                    instantDebits = null,
                    screenState = BankFormScreenStateFactory.createWithSession("session_1234"),
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
                        phone = CUSTOMER_PHONE,
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
            sellerBusinessName = null,
            forceSetupFutureUseBehavior = false,
        )

        val continueWithMicrodepositsMandate = USBankAccountTextBuilder.buildMandateAndMicrodepositsText(
            merchantName = MERCHANT_NAME,
            sellerBusinessName = null,
            forceSetupFutureUseBehavior = false,
            isVerifyingMicrodeposits = true,
            isSaveForFutureUseSelected = true,
            isInstantDebits = false,
            isSetupFlow = false,
        )

        val screenStates = listOf(
            BankFormScreenState(isPaymentFlow = true),
            BankFormScreenStateFactory.createWithSession(
                sessionId = "session_1234",
                isVerifyingWithMicrodeposits = false,
                mandateText = continueMandate,
            ),
            BankFormScreenStateFactory.createWithSession(
                sessionId = "session_1234",
                isVerifyingWithMicrodeposits = true,
                mandateText = continueWithMicrodepositsMandate,
            ),
        )

        for (screenState in screenStates) {
            val viewModel = createViewModel(
                defaultArgs.copy(
                    savedPaymentMethod = PaymentSelection.New.USBankAccount(
                        label = "Test",
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
    fun `Test defaults are used when not collecting fields if they are attached`() =
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
        assertThat(viewModel.saveForFutureUseCheckedFlow.value).isFalse()
    }

    @Test
    fun `Doesn't set setAsDefaultPaymentMethodElement by default`() = runTest {
        val viewModel = createViewModel(
            args = defaultArgs.copy(
                formArgs = defaultArgs.formArgs,
                showCheckbox = true,
                setAsDefaultPaymentMethodEnabled = IS_PAYMENT_METHOD_SET_AS_DEFAULT_ENABLED_DEFAULT_VALUE,
            ),
        )
        assertThat(viewModel.setAsDefaultPaymentMethodElement).isNull()
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

        viewModel.handlePrimaryButtonClick()

        verify(mockCollectBankAccountLauncher).presentWithDeferredPayment(
            publishableKey = any(),
            stripeAccountId = any(),
            configuration = eq(
                CollectBankAccountConfiguration.USBankAccountInternal(
                    name = "Jenny Rose",
                    email = "email@email.com",
                    elementsSessionContext = ElementsSessionContext(
                        amount = 5099,
                        currency = "usd",
                        linkMode = LinkMode.LinkPaymentMethod,
                        allowRedisplay = ElementsSessionContext.AllowRedisplay.Unspecified,
                        billingDetails = ElementsSessionContext.BillingDetails(
                            name = "Jenny Rose",
                            email = "email@email.com",
                        ),
                        prefillDetails = ElementsSessionContext.PrefillDetails(
                            email = "email@email.com",
                            phone = null,
                            phoneCountryCode = "US",
                        ),
                        incentiveEligibilitySession = null,
                        clientAttributionMetadata = null,
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

        viewModel.handlePrimaryButtonClick()

        verify(mockCollectBankAccountLauncher).presentWithDeferredSetup(
            publishableKey = any(),
            stripeAccountId = any(),
            configuration = eq(
                CollectBankAccountConfiguration.USBankAccountInternal(
                    name = "Jenny Rose",
                    email = "email@email.com",
                    elementsSessionContext = ElementsSessionContext(
                        amount = null,
                        currency = null,
                        linkMode = LinkMode.LinkPaymentMethod,
                        allowRedisplay = ElementsSessionContext.AllowRedisplay.Unspecified,
                        billingDetails = ElementsSessionContext.BillingDetails(
                            name = "Jenny Rose",
                            email = "email@email.com",
                        ),
                        prefillDetails = ElementsSessionContext.PrefillDetails(
                            email = "email@email.com",
                            phone = null,
                            phoneCountryCode = "US",
                        ),
                        incentiveEligibilitySession = null,
                        clientAttributionMetadata = null,
                    ),
                )
            ),
            elementsSessionId = any(),
            customerId = anyOrNull(),
            onBehalfOf = any(),
        )
    }

//    @Test
//    fun `When form destroyed, collect bank account result is null and screen is not reset`() = runTest {
//        val viewModel = createViewModel(
//            defaultArgs.copy(
//                clientSecret = null,
//                isPaymentFlow = false
//            )
//        )
//
//        viewModel.result.test {
//            viewModel.handleCollectBankAccountResult(result = mockVerifiedBankAccount())
//            assertThat(awaitItem()).isNotNull()
//
//            viewModel.onDestroy()
//            assertThat(awaitItem()).isNull()
//
//            val currentScreenState =
//                viewModel.currentScreenState.value
//
//            assertThat(currentScreenState)
//                .isInstanceOf<USBankAccountFormScreenState.MandateCollection>()
//        }
//    }

    @Test
    fun `When the primary button is pressed, the primary button state moves to processing`() = runTest {
        val viewModel = createViewModel()

        viewModel.currentScreenState.test {
            assertThat(awaitItem().isProcessing)
                .isFalse()

            viewModel.handlePrimaryButtonClick()

            assertThat(awaitItem().isProcessing)
                .isTrue()
        }
    }

    @Test
    fun `When collect bank account is returned from FC SDK, the result is emitted`() = runTest {
        val viewModel = createViewModel()

        viewModel.linkedAccount.test {
            assertThat(awaitItem()).isNull()

            viewModel.handleCollectBankAccountResult(mockVerifiedBankAccount())
            assertThat(awaitItem()).isNotNull()

            // Simulate a removal
            viewModel.reset()
            assertThat(awaitItem()).isNull()

            viewModel.handleCollectBankAccountResult(
                result = CollectBankAccountResultInternal.Cancelled
            )
            expectNoEvents()

            viewModel.handleCollectBankAccountResult(
                CollectBankAccountResultInternal.Failed(IllegalArgumentException("Failed"))
            )
            expectNoEvents()
        }
    }

    @Test
    fun `When the view model is reset, collect bank account result should be null`() = runTest {
        val viewModel = createViewModel()

        viewModel.linkedAccount.test {
            assertThat(awaitItem()).isNull()

            viewModel.handleCollectBankAccountResult(mockVerifiedBankAccount())
            assertThat(awaitItem()).isNotNull()

            viewModel.reset()
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `Should be reset after confirming bank account and attempting to reset`() = runTest {
        val viewModel = createViewModel()

        viewModel.currentScreenState.test {
            assertThat(awaitItem().linkedBankAccount).isNull()

            val verifiedAccount = mockVerifiedBankAccount()
            viewModel.handleCollectBankAccountResult(
                result = verifiedAccount
            )

            assertThat(awaitItem().linkedBankAccount).isNotNull()

            viewModel.handlePrimaryButtonClick()
            viewModel.reset()

            assertThat(expectMostRecentItem().linkedBankAccount).isNull()
        }
    }

    @Test
    fun `customerRequestedSave equals RequestReuse when showCheckbox and saveForFutureUse`() {
        assertThat(
            customerRequestedSave(
                showCheckbox = true,
                saveForFutureUse = true
            )
        ).isEqualTo(CustomerRequestedSave.RequestReuse)
    }

    @Test
    fun `customerRequestedSave equals RequestNoReuse when showCheckbox and not saveForFutureUse`() {
        assertThat(
            customerRequestedSave(
                showCheckbox = true,
                saveForFutureUse = false
            )
        ).isEqualTo(CustomerRequestedSave.RequestNoReuse)
    }

    @Test
    fun `customerRequestedSave equals NoRequest when not showCheckbox and not saveForFutureUse`() {
        assertThat(
            customerRequestedSave(
                showCheckbox = false,
                saveForFutureUse = false
            )
        ).isEqualTo(CustomerRequestedSave.NoRequest)
    }

    @Test
    fun `Uses CollectBankAccountLauncher for ACH when not in Instant Debits flow`() {
        val viewModel = createViewModel().apply {
            this.collectBankAccountLauncher = mockCollectBankAccountLauncher
        }

        viewModel.nameController.onValueChange("Some Name")
        viewModel.emailController.onValueChange("email@email.com")

        viewModel.handlePrimaryButtonClick()

        verify(mockCollectBankAccountLauncher).presentWithPaymentIntent(
            publishableKey = any(),
            stripeAccountId = anyOrNull(),
            clientSecret = any(),
            configuration = eq(
                CollectBankAccountConfiguration.USBankAccountInternal(
                    name = "Some Name",
                    email = "email@email.com",
                    elementsSessionContext = ElementsSessionContext(
                        amount = 5099,
                        currency = "usd",
                        linkMode = null,
                        allowRedisplay = ElementsSessionContext.AllowRedisplay.Unspecified,
                        billingDetails = ElementsSessionContext.BillingDetails(
                            name = "Some Name",
                            email = "email@email.com",
                        ),
                        prefillDetails = ElementsSessionContext.PrefillDetails(
                            email = "email@email.com",
                            phone = null,
                            phoneCountryCode = "US",
                        ),
                        incentiveEligibilitySession = null,
                        clientAttributionMetadata = null,
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

        viewModel.handlePrimaryButtonClick()

        verify(mockCollectBankAccountLauncher).presentWithPaymentIntent(
            publishableKey = any(),
            stripeAccountId = anyOrNull(),
            clientSecret = any(),
            configuration = eq(
                CollectBankAccountConfiguration.InstantDebits(
                    email = "email@email.com",
                    elementsSessionContext = ElementsSessionContext(
                        amount = 5099,
                        currency = "usd",
                        linkMode = LinkMode.LinkCardBrand,
                        allowRedisplay = ElementsSessionContext.AllowRedisplay.Unspecified,
                        billingDetails = ElementsSessionContext.BillingDetails(
                            email = "email@email.com",
                        ),
                        prefillDetails = ElementsSessionContext.PrefillDetails(
                            email = "email@email.com",
                            phone = null,
                            phoneCountryCode = "US",
                        ),
                        incentiveEligibilitySession = null,
                        clientAttributionMetadata = null,
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
            sellerBusinessName = null,
            forceSetupFutureUseBehavior = false,
        )

        viewModel.currentScreenState.test {
            assertThat(awaitItem().linkedBankAccount).isNull()

            val verifiedAccount = mockVerifiedBankAccount()
            viewModel.handleCollectBankAccountResult(verifiedAccount)

            val mandateCollectionViewState = awaitItem()
            assertThat(mandateCollectionViewState.linkedBankAccount?.mandateText).isEqualTo(expectedResult)
        }
    }

    @Test
    fun `Produces correct mandate text when using microdeposits verification`() = runTest {
        val viewModel = createViewModel()

        val expectedResult = USBankAccountTextBuilder.buildMandateAndMicrodepositsText(
            merchantName = MERCHANT_NAME,
            sellerBusinessName = null,
            forceSetupFutureUseBehavior = false,
            isVerifyingMicrodeposits = true,
            isSaveForFutureUseSelected = false,
            isSetupFlow = false,
            isInstantDebits = false,
        )

        viewModel.currentScreenState.test {
            assertThat(awaitItem().linkedBankAccount).isNull()

            val unverifiedAccount = mockManuallyEnteredBankAccount(usesMicrodeposits = true)
            viewModel.handleCollectBankAccountResult(unverifiedAccount)

            val mandateCollectionViewState = awaitItem()
            assertThat(mandateCollectionViewState.linkedBankAccount?.mandateText).isEqualTo(expectedResult)
        }
    }

    @Test
    fun `Produces correct mandate text when skipping verification for manually entered account`() = runTest {
        val viewModel = createViewModel()

        val expectedResult = USBankAccountTextBuilder.buildMandateAndMicrodepositsText(
            merchantName = MERCHANT_NAME,
            sellerBusinessName = null,
            forceSetupFutureUseBehavior = false,
            isVerifyingMicrodeposits = false,
            isSaveForFutureUseSelected = false,
            isSetupFlow = false,
            isInstantDebits = false,
        )

        viewModel.currentScreenState.test {
            assertThat(awaitItem().linkedBankAccount).isNull()

            val unverifiedAccount = mockManuallyEnteredBankAccount(usesMicrodeposits = false)
            viewModel.handleCollectBankAccountResult(unverifiedAccount)

            val mandateCollectionViewState = awaitItem()
            assertThat(mandateCollectionViewState.linkedBankAccount?.mandateText).isEqualTo(expectedResult)
        }
    }

    @Test
    fun `Produces null mandate text when termsDisplay=NEVER`() = runTest {
        val viewModel = createViewModel(args = defaultArgs.copy(termsDisplay = PaymentSheet.TermsDisplay.NEVER))

        viewModel.currentScreenState.test {
            assertThat(awaitItem().linkedBankAccount).isNull()

            val unverifiedAccount = mockManuallyEnteredBankAccount(usesMicrodeposits = false)
            viewModel.handleCollectBankAccountResult(unverifiedAccount)

            val mandateCollectionViewState = awaitItem()
            assertThat(mandateCollectionViewState.linkedBankAccount?.mandateText).isNull()
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

    @Test
    fun `Creates correct ElementsSessionContext if attaching defaults to PaymentMethod`() = runTest {
        val args = createArgsForBillingDetailsCollectionInInstantDebits(
            collectName = false,
            collectEmail = true,
            collectPhone = false,
            collectAddress = false,
            attachDefaultsToPaymentMethod = true,
        )

        val elementsSessionContext = testElementsSessionContextGeneration(viewModelArgs = args)

        assertThat(elementsSessionContext?.billingDetails).isEqualTo(
            ElementsSessionContext.BillingDetails(
                name = "Jenny Rose",
                email = "email@email.com",
                phone = "+13105551234",
                address = ElementsSessionContext.BillingDetails.Address(
                    line1 = "123 Main Street",
                    line2 = "Apt 456",
                    city = "San Francisco",
                    state = "CA",
                    postalCode = "94111",
                    country = "US",
                ),
            )
        )
    }

    @Test
    fun `Creates correct ElementsSessionContext if not attaching defaults to PaymentMethod`() = runTest {
        val args = createArgsForBillingDetailsCollectionInInstantDebits(
            collectName = false,
            collectEmail = true,
            collectPhone = false,
            collectAddress = false,
            attachDefaultsToPaymentMethod = false,
        )

        val elementsSessionContext = testElementsSessionContextGeneration(viewModelArgs = args)

        assertThat(elementsSessionContext?.billingDetails).isEqualTo(
            ElementsSessionContext.BillingDetails(
                email = "email@email.com",
            )
        )
    }

    @Test
    fun `Creates correct ElementsSessionContext if not attaching defaults to PaymentMethod with specific collection`() =
        runTest {
            val args = createArgsForBillingDetailsCollectionInInstantDebits(
                collectName = false,
                collectEmail = true,
                collectPhone = true,
                collectAddress = false,
                attachDefaultsToPaymentMethod = false,
            )

            val elementsSessionContext = testElementsSessionContextGeneration(viewModelArgs = args)

            assertThat(elementsSessionContext?.billingDetails).isEqualTo(
                ElementsSessionContext.BillingDetails(
                    email = "email@email.com",
                    phone = "+13105551234",
                )
            )
        }

    @Test
    fun `Updates result when 'save for future use' changes after linking account`() = runTest {
        val viewModel = createViewModel(
            args = defaultArgs.copy(showCheckbox = true)
        )

        viewModel.linkedAccount.test {
            assertThat(awaitItem()).isNull()

            viewModel.nameController.onValueChange("Some Name")
            viewModel.emailController.onValueChange("email@email.com")
            viewModel.handleCollectBankAccountResult(mockVerifiedBankAccount())
            assertThat(awaitItem()?.customerRequestedSave).isEqualTo(CustomerRequestedSave.RequestNoReuse)

            viewModel.saveForFutureUseElement.controller.onValueChange(true)
            assertThat(awaitItem()?.customerRequestedSave).isEqualTo(CustomerRequestedSave.RequestReuse)

            viewModel.saveForFutureUseElement.controller.onValueChange(false)
            assertThat(awaitItem()?.customerRequestedSave).isEqualTo(CustomerRequestedSave.RequestNoReuse)
        }
    }

    @Test
    fun `PaymentMethodOptionsParams does not contain SFU for RequestNoReuse if hasIntentToSetup`() = runTest {
        val viewModel = createViewModel(
            args = defaultArgs.copy(
                showCheckbox = true,
                formArgs = defaultArgs.formArgs.copy(
                    hasIntentToSetup = true
                )
            )
        )

        viewModel.linkedAccount.test {
            assertThat(awaitItem()).isNull()

            viewModel.nameController.onValueChange("Some Name")
            viewModel.emailController.onValueChange("email@email.com")
            viewModel.handleCollectBankAccountResult(mockVerifiedBankAccount())

            val initialAccount = awaitItem()
            assertThat(initialAccount?.customerRequestedSave).isEqualTo(CustomerRequestedSave.RequestNoReuse)
            assertThat(initialAccount?.paymentMethodOptionsParams).isEqualTo(
                PaymentMethodOptionsParams.USBankAccount(
                    setupFutureUsage = null
                )
            )

            viewModel.saveForFutureUseElement.controller.onValueChange(true)
            val updatedAccount = awaitItem()
            assertThat(updatedAccount?.customerRequestedSave).isEqualTo(CustomerRequestedSave.RequestReuse)
            assertThat(updatedAccount?.paymentMethodOptionsParams).isEqualTo(
                PaymentMethodOptionsParams.USBankAccount(
                    setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                )
            )
        }
    }

    @Test
    fun `'setAsDefaultPaymentMethod' shown correctly when saveForFutureUse checked`() = runTest {
        testSetAsDefaultPaymentMethod { saveForFutureUseElement, setAsDefaultPaymentMethodElement, testContext ->
            var nextItem = testContext.awaitItem()
            assertThat(nextItem?.input?.saveForFutureUse).isFalse()

            saveForFutureUseElement.controller.onValueChange(true)

            nextItem = testContext.awaitItem()
            assertThat(nextItem?.input?.saveForFutureUse).isTrue()
            assertThat(setAsDefaultPaymentMethodElement.shouldShowElementFlow.value).isTrue()
        }
    }

    @Test
    fun `'setAsDefaultPaymentMethod' hidden correctly when saveForFutureUse unchecked`() = runTest {
        testSetAsDefaultPaymentMethod { saveForFutureUseElement, setAsDefaultPaymentMethodElement, testContext ->
            var nextItem = testContext.awaitItem()
            assertThat(nextItem?.input?.saveForFutureUse).isFalse()

            saveForFutureUseElement.controller.onValueChange(true)

            nextItem = testContext.awaitItem()
            assertThat(nextItem?.input?.saveForFutureUse).isTrue()
            assertThat(setAsDefaultPaymentMethodElement.shouldShowElementFlow.value).isTrue()

            saveForFutureUseElement.controller.onValueChange(false)

            nextItem = testContext.awaitItem()
            assertThat(nextItem?.input?.saveForFutureUse).isFalse()
            assertThat(setAsDefaultPaymentMethodElement.shouldShowElementFlow.value).isFalse()
        }
    }

    @Test
    fun `'setAsDefaultPaymentMethod' hidden when saveForFutureUse checked & setAsDefaultMatchesSaveForFutureUse`() =
        runTest {
            testSetAsDefaultPaymentMethod(
                setAsDefaultMatchesSaveForFutureUse = true,
            ) { saveForFutureUseElement, setAsDefaultPaymentMethodElement, testContext ->
                var nextItem = testContext.awaitItem()
                assertThat(nextItem?.input?.saveForFutureUse).isFalse()

                saveForFutureUseElement.controller.onValueChange(true)

                nextItem = testContext.awaitItem()
                assertThat(nextItem?.input?.saveForFutureUse).isTrue()
                assertThat(setAsDefaultPaymentMethodElement.shouldShowElementFlow.value).isFalse()
            }
        }

    @Test
    fun `'setAsDefaultPaymentMethod' fieldVal true, saveForFutureUse checked & setAsDefaultMatchesSaveForFutureUse`() =
        runTest {
            testSetAsDefaultPaymentMethod(
                setAsDefaultMatchesSaveForFutureUse = true,
            ) { saveForFutureUseElement, setAsDefaultPaymentMethodElement, testContext ->
                var nextItem = testContext.awaitItem()
                assertThat(nextItem?.input?.saveForFutureUse).isFalse()

                saveForFutureUseElement.controller.onValueChange(true)

                nextItem = testContext.awaitItem()
                assertThat(nextItem?.input?.saveForFutureUse).isTrue()
                assertThat(setAsDefaultPaymentMethodElement.controller.fieldValue.value.toBoolean()).isTrue()
            }
        }

    @Test
    fun `'setAsDefaultPaymentMethod' hidden when saveForFutureUse !checked & setAsDefaultMatchesSaveForFutureUse`() =
        runTest {
            testSetAsDefaultPaymentMethod(
                setAsDefaultMatchesSaveForFutureUse = true,
            ) { saveForFutureUseElement, setAsDefaultPaymentMethodElement, testContext ->
                val nextItem = testContext.awaitItem()
                assertThat(nextItem?.input?.saveForFutureUse).isFalse()

                saveForFutureUseElement.controller.onValueChange(false)

                assertThat(setAsDefaultPaymentMethodElement.shouldShowElementFlow.value).isFalse()
            }
        }

    @Test
    fun `setAsDefaultPaymentMethod fieldVal false, saveForFutureUse !checked & setAsDefaultMatchesSaveForFutureUse`() =
        runTest {
            testSetAsDefaultPaymentMethod(
                setAsDefaultMatchesSaveForFutureUse = true,
            ) { saveForFutureUseElement, setAsDefaultPaymentMethodElement, testContext ->
                val nextItem = testContext.awaitItem()
                assertThat(nextItem?.input?.saveForFutureUse).isFalse()

                saveForFutureUseElement.controller.onValueChange(false)

                assertThat(setAsDefaultPaymentMethodElement.controller.fieldValue.value.toBoolean()).isFalse()
            }
        }

    @Test
    fun `Updates result when 'setAsDefaultPaymentMethod' changes after linking account`() = runTest {
        val viewModel = createViewModel(
            args = defaultArgs.copy(
                showCheckbox = true,
                setAsDefaultPaymentMethodEnabled = true
            )
        )

        viewModel.linkedAccount.test {
            assertThat(awaitItem()).isNull()

            viewModel.nameController.onValueChange("Some Name")
            viewModel.emailController.onValueChange("email@email.com")
            viewModel.handleCollectBankAccountResult(mockVerifiedBankAccount())
            viewModel.saveForFutureUseElement.controller.onValueChange(true)

            assertThat((awaitItem()?.paymentMethodExtraParams as PaymentMethodExtraParams.USBankAccount).setAsDefault)
                .isFalse()
            assertThat(viewModel.setAsDefaultPaymentMethodElement!!.shouldShowElementFlow.value)
                .isTrue()

            assertThat(awaitItem()?.input?.saveForFutureUse).isTrue()

            viewModel.setAsDefaultPaymentMethodElement.controller.onValueChange(true)
            assertThat((awaitItem()?.paymentMethodExtraParams as PaymentMethodExtraParams.USBankAccount).setAsDefault)
                .isTrue()

            viewModel.setAsDefaultPaymentMethodElement.controller.onValueChange(false)
            assertThat((awaitItem()?.paymentMethodExtraParams as PaymentMethodExtraParams.USBankAccount).setAsDefault)
                .isFalse()
        }
    }

    @Test
    fun `Updates result when billing address changes after linking account`() = runTest {
        val viewModel = createViewModel(
            args = defaultArgs.copy(showCheckbox = true)
        )

        viewModel.linkedAccount.test {
            assertThat(awaitItem()).isNull()

            viewModel.nameController.onValueChange("Some Name")
            viewModel.emailController.onValueChange("email@email.com")
            viewModel.handleCollectBankAccountResult(mockVerifiedBankAccount())
            assertThat(awaitItem()?.paymentMethodCreateParams?.billingDetails?.email).isEqualTo("email@email.com")

            viewModel.emailController.onValueChange("email@email.ca")
            assertThat(awaitItem()?.paymentMethodCreateParams?.billingDetails?.email).isEqualTo("email@email.ca")

            viewModel.emailController.onValueChange("email@email.com")
            assertThat(awaitItem()?.paymentMethodCreateParams?.billingDetails?.email).isEqualTo("email@email.com")
        }
    }

    @Test
    fun `ElementsSessionContext contains correct allowRedisplay when using Legacy save behavior`() = runTest {
        val elementsSessionContext = testElementsSessionContextGeneration(
            viewModelArgs = defaultArgs.copy(
                instantDebits = true,
                showCheckbox = false,
                formArgs = defaultArgs.formArgs.copy(
                    hasIntentToSetup = false,
                    paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
                )
            )
        )

        assertThat(elementsSessionContext).isNotNull()
        assertThat(elementsSessionContext?.allowRedisplay)
            .isEqualTo(ElementsSessionContext.AllowRedisplay.Unspecified)
    }

    @Test
    fun `ElementsSessionContext contains correct allowRedisplay when using Enabled save behavior and checked`() =
        runTest {
            val elementsSessionContext = testElementsSessionContextGeneration(
                viewModelArgs = defaultArgs.copy(
                    instantDebits = true,
                    showCheckbox = true,
                    formArgs = defaultArgs.formArgs.copy(
                        hasIntentToSetup = false,
                        paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled,
                    )
                ),
                checkboxValue = true,
            )

            assertThat(elementsSessionContext).isNotNull()
            assertThat(elementsSessionContext?.allowRedisplay)
                .isEqualTo(ElementsSessionContext.AllowRedisplay.Always)
        }

    @Test
    fun `ElementsSessionContext contains correct allowRedisplay when using Enabled save behavior and not checked`() =
        runTest {
            val elementsSessionContext = testElementsSessionContextGeneration(
                viewModelArgs = defaultArgs.copy(
                    instantDebits = true,
                    showCheckbox = true,
                    formArgs = defaultArgs.formArgs.copy(
                        hasIntentToSetup = false,
                        paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled,
                    )
                ),
                checkboxValue = false,
            )

            assertThat(elementsSessionContext).isNotNull()
            assertThat(elementsSessionContext?.allowRedisplay)
                .isEqualTo(ElementsSessionContext.AllowRedisplay.Unspecified)
        }

    @Test
    fun `ElementsSessionContext has correct allowRedisplay when Enabled behavior with setup intent & not checked`() =
        runTest {
            val elementsSessionContext = testElementsSessionContextGeneration(
                viewModelArgs = defaultArgs.copy(
                    instantDebits = true,
                    showCheckbox = true,
                    formArgs = defaultArgs.formArgs.copy(
                        hasIntentToSetup = true,
                        paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled,
                    )
                ),
                checkboxValue = false,
            )

            assertThat(elementsSessionContext).isNotNull()
            assertThat(elementsSessionContext?.allowRedisplay)
                .isEqualTo(ElementsSessionContext.AllowRedisplay.Limited)
        }

    @Test
    fun `ElementsSessionContext contains correct allowRedisplay with Disabled behavior without override`() = runTest {
        val elementsSessionContext = testElementsSessionContextGeneration(
            viewModelArgs = defaultArgs.copy(
                instantDebits = true,
                showCheckbox = false,
                formArgs = defaultArgs.formArgs.copy(
                    hasIntentToSetup = true,
                    paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Disabled(
                        overrideAllowRedisplay = null
                    ),
                )
            ),
        )

        assertThat(elementsSessionContext).isNotNull()
        assertThat(elementsSessionContext?.allowRedisplay)
            .isEqualTo(ElementsSessionContext.AllowRedisplay.Limited)
    }

    @Test
    fun `ElementsSessionContext contains correct allowRedisplay with Disabled behavior with Always override`() =
        runTest {
            val elementsSessionContext = testElementsSessionContextGeneration(
                viewModelArgs = defaultArgs.copy(
                    instantDebits = true,
                    showCheckbox = false,
                    formArgs = defaultArgs.formArgs.copy(
                        hasIntentToSetup = true,
                        paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Disabled(
                            overrideAllowRedisplay = PaymentMethod.AllowRedisplay.ALWAYS
                        ),
                    )
                ),
            )

            assertThat(elementsSessionContext).isNotNull()
            assertThat(elementsSessionContext?.allowRedisplay)
                .isEqualTo(ElementsSessionContext.AllowRedisplay.Always)
        }

    @Test
    fun `Contains all supported billing countries when allowed countries is empty`() {
        val viewModel = createViewModel(
            args = defaultArgs.run {
                copy(
                    formArgs = formArgs.copy(
                        billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                            name = CollectionMode.Automatic,
                            phone = CollectionMode.Automatic,
                            email = CollectionMode.Automatic,
                            address = AddressCollectionMode.Full,
                            allowedCountries = emptySet()
                        )
                    )
                )
            }
        )

        assertThat(viewModel.addressElement.countryElement.controller.displayItems)
            .hasSize(CountryUtils.supportedBillingCountries.size)
    }

    @Test
    fun `Contains only countries provided through billing configuration`() {
        val viewModel = createViewModel(
            args = defaultArgs.run {
                copy(
                    formArgs = formArgs.copy(
                        billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                            name = CollectionMode.Automatic,
                            phone = CollectionMode.Automatic,
                            email = CollectionMode.Automatic,
                            address = AddressCollectionMode.Full,
                            allowedCountries = setOf("US", "CA")
                        )
                    )
                )
            }
        )

        assertThat(viewModel.addressElement.countryElement.controller.displayItems).containsExactly(
            "\uD83C\uDDFA\uD83C\uDDF8 United States",
            "\uD83C\uDDE8\uD83C\uDDE6 Canada"
        )
    }

    @Test
    fun `If autocomplete address interactor factory provided, should use autocomplete element`() = runTest {
        val viewModel = createViewModel(
            args = defaultArgs.copy(showCheckbox = true),
            autocompleteAddressInteractorFactory = {
                TestAutocompleteAddressInteractor.noOp(
                    autocompleteConfig = AutocompleteAddressInteractor.Config(
                        googlePlacesApiKey = "gi_123",
                        autocompleteCountries = setOf("US")
                    )
                )
            }
        )

        assertThat(viewModel.addressElement).isInstanceOf<AutocompleteAddressElement>()
    }

    @Test
    fun `If autocomplete & no billing, last text field identifier should be the autocomplete text field`() = runTest {
        val viewModel = createViewModel(
            args = defaultArgs.copy(
                formArgs = defaultArgs.formArgs.copy(
                    billingDetails = null,
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        name = CollectionMode.Always,
                        phone = CollectionMode.Always,
                        email = CollectionMode.Always,
                        address = AddressCollectionMode.Full,
                    ),
                )
            ),
            autocompleteAddressInteractorFactory = {
                TestAutocompleteAddressInteractor.noOp(
                    autocompleteConfig = AutocompleteAddressInteractor.Config(
                        googlePlacesApiKey = "gi_123",
                        autocompleteCountries = setOf("US"),
                        isPlacesAvailable = true,
                    )
                )
            }
        )

        viewModel.lastTextFieldIdentifier.test {
            assertThat(expectMostRecentItem()).isEqualTo(IdentifierSpec.OneLineAddress)
        }
    }

    @Test
    fun `If missing required fields, 'validate' should validate fields & reset validation on 'reset'`() = runTest {
        val viewModel = createViewModel(
            args = defaultArgs.run {
                copy(
                    formArgs = formArgs.copy(
                        billingDetails = null,
                        billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                            name = CollectionMode.Always,
                            phone = CollectionMode.Always,
                            email = CollectionMode.Always,
                            address = AddressCollectionMode.Full,
                            allowedCountries = emptySet()
                        )
                    )
                )
            }
        )

        turbineScope {
            val nameErrorTurbine = viewModel.nameController.error.testIn(this)
            val emailErrorTurbine = viewModel.emailController.error.testIn(this)
            val phoneErrorTurbine = viewModel.phoneController.error.testIn(this)
            val addressErrorTurbine =
                viewModel.addressElement.sectionFieldErrorController().error.testIn(this)

            assertThat(nameErrorTurbine.awaitItem()).isNull()
            assertThat(emailErrorTurbine.awaitItem()).isNull()
            assertThat(phoneErrorTurbine.awaitItem()).isNull()
            assertThat(addressErrorTurbine.awaitItem()).isNull()

            viewModel.validate()

            assertThat(nameErrorTurbine.awaitItem()).isNotNull()
            assertThat(emailErrorTurbine.awaitItem()).isNotNull()
            assertThat(phoneErrorTurbine.awaitItem()).isNotNull()
            assertThat(addressErrorTurbine.awaitItem()).isNotNull()

            viewModel.onDestroy()

            assertThat(nameErrorTurbine.awaitItem()).isNull()
            assertThat(emailErrorTurbine.awaitItem()).isNull()
            assertThat(phoneErrorTurbine.awaitItem()).isNull()
            assertThat(addressErrorTurbine.expectMostRecentItem()).isNull()

            nameErrorTurbine.cancelAndIgnoreRemainingEvents()
            emailErrorTurbine.cancelAndIgnoreRemainingEvents()
            phoneErrorTurbine.cancelAndIgnoreRemainingEvents()
            addressErrorTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    private fun testElementsSessionContextGeneration(
        viewModelArgs: USBankAccountFormViewModel.Args,
        checkboxValue: Boolean? = null
    ): ElementsSessionContext? {
        val viewModel = createViewModel(viewModelArgs)
        viewModel.collectBankAccountLauncher = mockCollectBankAccountLauncher

        checkboxValue?.let {
            viewModel.saveForFutureUseElement.controller.onValueChange(it)
        }

        viewModel.handlePrimaryButtonClick()

        val argumentCaptor = argumentCaptor<CollectBankAccountConfiguration>()

        verify(mockCollectBankAccountLauncher).presentWithPaymentIntent(
            publishableKey = any(),
            stripeAccountId = anyOrNull(),
            clientSecret = any(),
            configuration = argumentCaptor.capture(),
        )

        val instantDebitsConfiguration = argumentCaptor.firstValue as CollectBankAccountConfiguration.InstantDebits
        return instantDebitsConfiguration.elementsSessionContext
    }

    private fun createArgsForBillingDetailsCollectionInInstantDebits(
        collectEmail: Boolean,
        collectName: Boolean,
        collectPhone: Boolean,
        collectAddress: Boolean,
        attachDefaultsToPaymentMethod: Boolean,
    ): USBankAccountFormViewModel.Args {
        val billingDetails = PaymentSheet.BillingDetails(
            name = CUSTOMER_NAME,
            email = CUSTOMER_EMAIL,
            phone = CUSTOMER_PHONE,
            address = CUSTOMER_ADDRESS,
        )

        val billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            name = if (collectName) CollectionMode.Always else CollectionMode.Never,
            email = if (collectEmail) CollectionMode.Always else CollectionMode.Never,
            phone = if (collectPhone) CollectionMode.Always else CollectionMode.Never,
            address = if (collectAddress) AddressCollectionMode.Full else AddressCollectionMode.Never,
            attachDefaultsToPaymentMethod = attachDefaultsToPaymentMethod,
        )

        return defaultArgs.copy(
            instantDebits = true,
            formArgs = defaultArgs.formArgs.copy(
                billingDetails = billingDetails,
                billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
            )
        )
    }

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

    private suspend fun testSetAsDefaultPaymentMethod(
        setAsDefaultMatchesSaveForFutureUse: Boolean = false,
        block:
        suspend (
            SaveForFutureUseElement,
            SetAsDefaultPaymentMethodElement,
            TurbineTestContext<PaymentSelection.New.USBankAccount?>,
        ) -> Unit
    ) {
        val viewModel = createViewModel(
            args = defaultArgs.copy(
                showCheckbox = true,
                setAsDefaultPaymentMethodEnabled = true,
                setAsDefaultMatchesSaveForFutureUse = setAsDefaultMatchesSaveForFutureUse,
            )
        )

        viewModel.linkedAccount.test {
            assertThat(awaitItem()).isNull()

            viewModel.nameController.onValueChange("Some Name")
            viewModel.emailController.onValueChange("email@email.com")
            viewModel.handleCollectBankAccountResult(mockVerifiedBankAccount())

            block(viewModel.saveForFutureUseElement, viewModel.setAsDefaultPaymentMethodElement!!, this)
        }
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
                instantDebits = isInstantDebits,
                showCheckbox = showCheckbox,
                formArgs = defaultArgs.formArgs.copy(
                    hasIntentToSetup = hasIntentForSetup,
                    paymentMethodSaveConsentBehavior = paymentMethodSaveConsentBehavior,
                )
            )
        )

        viewModel.linkedAccount.test {
            skipItems(1)

            viewModel.handleCollectBankAccountResult(mockVerifiedBankAccount())
            var result = awaitItem()

            if (shouldSave) {
                viewModel.saveForFutureUseElement.controller.onValueChange(shouldSave)
                result = awaitItem()
            }

            viewModel.handlePrimaryButtonClick()

            assertThat(result?.paymentMethodCreateParams?.toParamMap()).containsEntry(
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
        args: USBankAccountFormViewModel.Args = defaultArgs,
        autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory? = null,
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
            autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
        )
    }

    private fun mockManuallyEnteredBankAccount(usesMicrodeposits: Boolean): CollectBankAccountResultInternal.Completed {
        val paymentIntent = mock<PaymentIntent>()
        val financialConnectionsSession = mock<FinancialConnectionsSession>()
        whenever(paymentIntent.id).thenReturn(defaultArgs.clientSecret)
        whenever(financialConnectionsSession.id).thenReturn("123")
        whenever(financialConnectionsSession.paymentAccount).thenReturn(
            BankAccount(
                id = "123",
                last4 = "4567",
                bankName = "Test",
                routingNumber = "123",
                usesMicrodeposits = usesMicrodeposits,
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
        const val CUSTOMER_PHONE = "+13105551234"
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
