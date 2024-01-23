package com.stripe.android.paymentsheet.state

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.model.CountryCode
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.LinkSignupMode.AlongsideSaveForFutureUse
import com.stripe.android.link.ui.inline.LinkSignupMode.InsteadOfSaveForFutureUse
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntent.ConfirmationMethod.Manual
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeIntent.Status.Canceled
import com.stripe.android.model.StripeIntent.Status.Succeeded
import com.stripe.android.model.wallets.Wallet
import com.stripe.android.paymentsheet.FakePrefsRepository
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.PaymentSheetLoadingException.PaymentIntentInTerminalState
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.FakeElementsSessionRepository
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.capture
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class DefaultPaymentSheetLoaderTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val eventReporter = mock<EventReporter>()

    private val customerRepository = FakeCustomerRepository(PAYMENT_METHODS)
    private val lpmRepository = LpmRepository(
        arguments = LpmRepository.LpmRepositoryArguments(
            resources = ApplicationProvider.getApplicationContext<Application>().resources,
        ),
        lpmInitialFormData = LpmRepository.LpmInitialFormData(),
    ).apply {
        this.update(
            PaymentIntentFactory.create(
                paymentMethodTypes = listOf(
                    PaymentMethod.Type.Card.code,
                    PaymentMethod.Type.USBankAccount.code,
                    PaymentMethod.Type.CashAppPay.code,
                ),
            ),
            null
        )
    }

    private val prefsRepository = FakePrefsRepository()

    @Captor
    private lateinit var paymentMethodTypeCaptor: ArgumentCaptor<List<PaymentMethod.Type>>

    private val readyGooglePayRepository = mock<GooglePayRepository>()
    private val unreadyGooglePayRepository = mock<GooglePayRepository>()

    @BeforeTest
    fun setup() {
        MockitoAnnotations.openMocks(this)

        whenever(readyGooglePayRepository.isReady()).thenReturn(
            flow {
                emit(true)
            }
        )

        whenever(unreadyGooglePayRepository.isReady()).thenReturn(
            flow {
                emit(false)
            }
        )
    }

    @Test
    fun `load with configuration should return expected result`() = runTest {
        prefsRepository.savePaymentSelection(
            PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )

        val loader = createPaymentSheetLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
        )

        assertThat(
            loader.load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            ).getOrThrow()
        ).isEqualTo(
            PaymentSheetState.Full(
                config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
                customerPaymentMethods = PAYMENT_METHODS,
                isGooglePayReady = true,
                paymentSelection = PaymentSelection.Saved(
                    paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                ),
                linkState = null,
                isEligibleForCardBrandChoice = false,
            )
        )
    }

    @Test
    fun `load with google pay kill switch enabled should return expected result`() = runTest {
        prefsRepository.savePaymentSelection(
            PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )

        val loader = createPaymentSheetLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
            isGooglePayReady = true,
            isGooglePayEnabledFromBackend = false,
        )

        assertThat(
            loader.load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            ).getOrThrow()
        ).isEqualTo(
            PaymentSheetState.Full(
                config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
                customerPaymentMethods = PAYMENT_METHODS,
                isGooglePayReady = false,
                paymentSelection = PaymentSelection.Saved(
                    paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                ),
                linkState = null,
                isEligibleForCardBrandChoice = false,
            )
        )
    }

    @Test
    fun `Should default to first payment method if customer has payment methods`() = runTest {
        prefsRepository.savePaymentSelection(null)

        val loader = createPaymentSheetLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
            isGooglePayReady = true,
            customerRepo = FakeCustomerRepository(paymentMethods = PAYMENT_METHODS),
        )

        val state = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ).getOrThrow()

        assertThat(state.paymentSelection).isEqualTo(
            PaymentSelection.Saved(paymentMethod = PAYMENT_METHODS.first())
        )
    }

    @Test
    fun `Should default to last used payment method if available even if customer has payment methods`() = runTest {
        prefsRepository.savePaymentSelection(PaymentSelection.GooglePay)

        val loader = createPaymentSheetLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
            isGooglePayReady = true,
            customerRepo = FakeCustomerRepository(paymentMethods = PAYMENT_METHODS),
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ).getOrThrow()

        assertThat(result.paymentSelection).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `Should default to no payment method if customer has no payment methods and no last used payment method`() = runTest {
        prefsRepository.savePaymentSelection(null)

        val loader = createPaymentSheetLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
            isGooglePayReady = true,
            customerRepo = FakeCustomerRepository(paymentMethods = emptyList()),
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ).getOrThrow()

        assertThat(result.paymentSelection).isNull()
    }

    @Test
    fun `load() with customer should fetch only supported payment method types`() =
        runTest {
            val customerRepository = mock<CustomerRepository> {
                whenever(it.getPaymentMethods(any(), any(), any())).thenReturn(Result.success(emptyList()))
            }

            val paymentMethodTypes = listOf(
                "card", // valid and supported
                "fpx", // valid but not supported
                "invalid_type" // unknown type
            )

            val loader = createPaymentSheetLoader(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = paymentMethodTypes
                ),
                customerRepo = customerRepository
            )

            loader.load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            )

            verify(customerRepository).getPaymentMethods(
                any(),
                capture(paymentMethodTypeCaptor),
                any(),
            )
            assertThat(paymentMethodTypeCaptor.allValues.flatten())
                .containsExactly(PaymentMethod.Type.Card)
        }

    @Test
    fun `when allowsDelayedPaymentMethods is false then delayed payment methods are filtered out`() =
        runTest {
            val customerRepository = mock<CustomerRepository> {
                whenever(it.getPaymentMethods(any(), any(), any())).thenReturn(Result.success(emptyList()))
            }

            val loader = createPaymentSheetLoader(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf(
                        PaymentMethod.Type.Card.code,
                        PaymentMethod.Type.SepaDebit.code,
                        PaymentMethod.Type.AuBecsDebit.code
                    )
                ),
                customerRepo = customerRepository
            )

            loader.load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            )

            verify(customerRepository).getPaymentMethods(
                any(),
                capture(paymentMethodTypeCaptor),
                any(),
            )
            assertThat(paymentMethodTypeCaptor.value)
                .containsExactly(PaymentMethod.Type.Card)
        }

    @Test
    fun `load() with customer should filter out invalid payment method types`() =
        runTest {
            val result = createPaymentSheetLoader(
                customerRepo = FakeCustomerRepository(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(card = null), // invalid
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                )
            ).load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            ).getOrThrow()

            assertThat(result.customerPaymentMethods)
                .containsExactly(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        }

    @Test
    fun `load() with customer should not filter out cards attached to a wallet`() =
        runTest {
            val cardWithAmexWallet = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
                card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.card?.copy(
                    wallet = Wallet.AmexExpressCheckoutWallet("3000")
                )
            )
            val result = createPaymentSheetLoader(
                customerRepo = FakeCustomerRepository(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                        cardWithAmexWallet
                    )
                )
            ).load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            ).getOrThrow()

            assertThat(result.customerPaymentMethods)
                .containsExactly(PaymentMethodFixtures.CARD_PAYMENT_METHOD, cardWithAmexWallet)
        }

    @Test
    fun `load() with customer should allow sepa`() = runTest {
        var requestPaymentMethodTypes: List<PaymentMethod.Type>? = null
        val result = createPaymentSheetLoader(
            customerRepo = object : FakeCustomerRepository() {
                override suspend fun getPaymentMethods(
                    customerConfig: PaymentSheet.CustomerConfiguration,
                    types: List<PaymentMethod.Type>,
                    silentlyFail: Boolean
                ): Result<List<PaymentMethod>> {
                    requestPaymentMethodTypes = types
                    return Result.success(
                        listOf(
                            PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                            PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD,
                        )
                    )
                }
            },
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "sepa_debit")
            )
        ).load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.CLIENT_SECRET,
            ),
            paymentSheetConfiguration = PaymentSheetFixtures.CONFIG_CUSTOMER.copy(
                allowsDelayedPaymentMethods = true,
            ),
        ).getOrThrow()

        assertThat(result.customerPaymentMethods)
            .containsExactly(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD,
            )
        assertThat(requestPaymentMethodTypes)
            .containsExactly(PaymentMethod.Type.Card, PaymentMethod.Type.SepaDebit)
    }

    @Test
    fun `load() when PaymentIntent has invalid status should return null`() = runTest {
        val paymentIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
            paymentMethod = PaymentMethodFactory.card(),
        )
        val paymentMethod = paymentIntent.paymentMethod!!

        val result = createPaymentSheetLoader(
            stripeIntent = paymentIntent,
        ).load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ).exceptionOrNull()

        assertThat(result).isEqualTo(PaymentIntentInTerminalState(paymentMethod, Succeeded))
    }

    @Test
    fun `load() when PaymentIntent has invalid confirmationMethod should return null`() = runTest {
        val result = createPaymentSheetLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                confirmationMethod = Manual,
            ),
        ).load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
        ).exceptionOrNull()

        assertThat(result).isEqualTo(PaymentSheetLoadingException.InvalidConfirmationMethod(Manual))
    }

    @Test
    fun `Defaults to first existing payment method for known customer`() = runTest {
        val result = createPaymentSheetLoader(
            customerRepo = FakeCustomerRepository(paymentMethods = PAYMENT_METHODS)
        ).load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = mockConfiguration(
                customer = PaymentSheet.CustomerConfiguration(
                    id = "some_id",
                    ephemeralKeySecret = "some_key"
                )
            )
        ).getOrThrow()

        val expectedPaymentMethod = requireNotNull(PAYMENT_METHODS.first())
        assertThat(result.paymentSelection).isEqualTo(PaymentSelection.Saved(expectedPaymentMethod))
    }

    @Test
    fun `Considers Link logged in if the account is verified`() = runTest {
        val loader = createPaymentSheetLoader(linkAccountState = AccountStatus.Verified)

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
        ).getOrThrow()

        assertThat(result.linkState?.loginState).isEqualTo(LinkState.LoginState.LoggedIn)
    }

    @Test
    fun `Considers Link as needing verification if the account needs verification`() = runTest {
        val loader = createPaymentSheetLoader(linkAccountState = AccountStatus.NeedsVerification)

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
        ).getOrThrow()

        assertThat(result.linkState?.loginState).isEqualTo(LinkState.LoginState.NeedsVerification)
    }

    @Test
    fun `Considers Link as needing verification if the account is being verified`() = runTest {
        val loader = createPaymentSheetLoader(linkAccountState = AccountStatus.VerificationStarted)

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
        ).getOrThrow()

        assertThat(result.linkState?.loginState).isEqualTo(LinkState.LoginState.NeedsVerification)
    }

    @Test
    fun `Considers Link as logged out correctly`() = runTest {
        val loader = createPaymentSheetLoader(linkAccountState = AccountStatus.SignedOut)

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
        ).getOrThrow()

        assertThat(result.linkState?.loginState).isEqualTo(LinkState.LoginState.LoggedOut)
    }

    @Test
    fun `Populates Link configuration correctly from billing details`() = runTest {
        val loader = createPaymentSheetLoader()

        val billingDetails = PaymentSheet.BillingDetails(
            address = PaymentSheet.Address(country = "CA"),
            name = "Till",
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(
                defaultBillingDetails = billingDetails,
            ),
        ).getOrThrow()

        val expectedLinkConfig = LinkConfiguration(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            signupMode = InsteadOfSaveForFutureUse,
            merchantName = "Merchant",
            merchantCountryCode = null,
            customerInfo = LinkConfiguration.CustomerInfo(
                name = "Till",
                email = null,
                phone = null,
                billingCountryCode = "CA",
                shouldPrefill = true,
            ),
            shippingValues = null,
            passthroughModeEnabled = false,
        )

        assertThat(result.linkState?.configuration).isEqualTo(expectedLinkConfig)
    }

    @Test
    fun `Populates Link configuration with shipping details if checkbox is selected`() = runTest {
        val loader = createPaymentSheetLoader()

        val shippingDetails = AddressDetails(
            name = "Not Till",
            address = PaymentSheet.Address(country = "US"),
            isCheckboxSelected = true,
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(
                shippingDetails = shippingDetails,
            ),
        ).getOrThrow()

        assertThat(result.linkState?.configuration?.shippingValues).isNotNull()
    }

    @Test
    fun `Populates Link configuration with passthrough mode`() = runTest {
        val loader = createPaymentSheetLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
            linkSettings = ElementsSession.LinkSettings(
                linkFundingSources = emptyList(),
                linkPassthroughModeEnabled = true,
            )
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
        ).getOrThrow()

        assertThat(result.linkState?.configuration?.passthroughModeEnabled).isTrue()
    }

    @Test
    fun `Uses shipping address phone number if checkbox is selected`() = runTest {
        val loader = createPaymentSheetLoader()

        val billingDetails = PaymentSheet.BillingDetails(phone = "123-456-7890")

        val shippingDetails = AddressDetails(
            address = PaymentSheet.Address(country = "US"),
            phoneNumber = "098-765-4321",
            isCheckboxSelected = true,
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(
                shippingDetails = shippingDetails,
                defaultBillingDetails = billingDetails,
            ),
        ).getOrThrow()

        assertThat(result.linkState?.configuration?.customerInfo?.phone).isEqualTo(shippingDetails.phoneNumber)
    }

    @Test
    fun `Falls back to customer email if billing address email not provided`() = runTest {
        val loader = createPaymentSheetLoader(
            customerRepo = FakeCustomerRepository(
                customer = mock {
                    on { email } doReturn "email@stripe.com"
                }
            ),
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                setupFutureUsage = StripeIntent.Usage.OffSession,
            )
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(
                customer = PaymentSheet.CustomerConfiguration(
                    id = "id",
                    ephemeralKeySecret = "key"
                ),
            ),
        ).getOrThrow()

        assertThat(result.linkState?.configuration?.customerInfo?.email).isEqualTo("email@stripe.com")
    }

    @Test
    fun `Moves last-used customer payment method to the front of the list`() = runTest {
        val paymentMethods = PaymentMethodFixtures.createCards(10)
        val lastUsed = paymentMethods[6]
        prefsRepository.savePaymentSelection(PaymentSelection.Saved(lastUsed))

        val loader = createPaymentSheetLoader(
            customerRepo = FakeCustomerRepository(paymentMethods = paymentMethods),
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(
                customer = PaymentSheet.CustomerConfiguration(
                    id = "id",
                    ephemeralKeySecret = "key",
                ),
            ),
        ).getOrThrow()

        val observedElements = result.customerPaymentMethods
        val expectedElements = listOf(lastUsed) + (paymentMethods - lastUsed)

        assertThat(observedElements).containsExactlyElementsIn(expectedElements).inOrder()
    }

    @Test
    fun `Returns failure if StripeIntent does not contain any supported payment method type`() = runTest {
        val loader = createPaymentSheetLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("gold", "silver", "bronze"),
            ),
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
        ).exceptionOrNull()

        assertThat(result).isEqualTo(
            PaymentSheetLoadingException.NoPaymentMethodTypesAvailable(
                requested = "gold, silver, bronze",
                supported = "card, cashapp",
            )
        )
    }

    @Test
    fun `Emits correct events when loading succeeds for non-deferred intent`() = runTest {
        val loader = createPaymentSheetLoader()

        loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
        )

        verify(eventReporter).onLoadStarted()
        verify(eventReporter).onLoadSucceeded(linkEnabled = true, currency = "usd")
    }

    @Test
    fun `Emits correct events when loading succeeds for deferred intent`() = runTest {
        val loader = createPaymentSheetLoader()

        loader.load(
            initializationMode = PaymentSheet.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1234,
                        currency = "cad",
                    ),
                ),
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
        )

        verify(eventReporter).onLoadStarted()
        verify(eventReporter).onLoadSucceeded(linkEnabled = true, currency = "usd")
    }

    @Test
    fun `Emits correct events when loading fails for non-deferred intent`() = runTest {
        val error = PaymentSheetLoadingException.MissingAmountOrCurrency
        val loader = createPaymentSheetLoader(error = error)

        loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
        )

        verify(eventReporter).onLoadStarted()
        verify(eventReporter).onLoadFailed(error)
    }

    @Test
    fun `Emits correct events when loading fails for deferred intent`() = runTest {
        val error = PaymentIntentInTerminalState(usedPaymentMethod = null, status = Canceled)
        val loader = createPaymentSheetLoader(error = error)

        loader.load(
            initializationMode = PaymentSheet.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1234,
                        currency = "cad",
                    ),
                ),
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
        )

        verify(eventReporter).onLoadStarted()
        verify(eventReporter).onLoadFailed(error)
    }

    @Test
    fun `Emits correct events when loading fails with invalid confirmation method`() = runTest {
        val loader = createPaymentSheetLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                confirmationMethod = Manual,
            )
        )

        loader.load(
            initializationMode = PaymentSheet.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1234,
                        currency = "cad",
                    ),
                ),
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
        )

        verify(eventReporter).onLoadStarted()
        verify(eventReporter).onLoadFailed(PaymentSheetLoadingException.InvalidConfirmationMethod(Manual))
    }

    @Test
    fun `Emits correct events when we fallback to the core Stripe API`() = runTest {
        val intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val error = APIConnectionException()

        val loader = createPaymentSheetLoader(fallbackError = error)

        loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = intent.clientSecret!!,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
        )

        verify(eventReporter).onLoadStarted()
        verify(eventReporter).onElementsSessionLoadFailed(error)
    }

    @Test
    fun `Includes card brand choice state if feature is enabled`() = runTest {
        val loader = createPaymentSheetLoader(isCbcEligible = true)

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
        ).getOrThrow()

        assertThat(result.isEligibleForCardBrandChoice).isTrue()
    }

    @Test
    fun `Provides Link signup prefill if showing instead of save-for-future-use`() = runTest {
        val loader = createPaymentSheetLoader(
            linkAccountState = AccountStatus.SignedOut,
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration(
                merchantDisplayName = "Some Name",
            ),
        ).getOrThrow()

        assertThat(result.linkState?.configuration?.customerInfo?.shouldPrefill).isTrue()
    }

    @Test
    fun `Provides no Link signup prefill if showing alongside save-for-future-use`() = runTest {
        val loader = createPaymentSheetLoader(
            linkAccountState = AccountStatus.NeedsVerification,
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration(
                merchantDisplayName = "Some Name",
                customer = PaymentSheet.CustomerConfiguration(
                    id = "cus_123",
                    ephemeralKeySecret = "some_secret",
                ),
            ),
        ).getOrThrow()

        assertThat(result.linkState?.configuration?.customerInfo?.shouldPrefill).isFalse()
    }

    @Test
    fun `Returns correct Link signup mode if not saving for future use`() = runTest {
        val loader = createPaymentSheetLoader(
            linkAccountState = AccountStatus.Error,
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
        ).getOrThrow()

        assertThat(result.linkState?.configuration?.signupMode).isEqualTo(InsteadOfSaveForFutureUse)
    }

    @Test
    fun `Returns correct Link signup mode if saving for future use`() = runTest {
        val loader = createPaymentSheetLoader(
            linkAccountState = AccountStatus.NeedsVerification,
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration(
                merchantDisplayName = "Some Name",
                customer = PaymentSheet.CustomerConfiguration(
                    id = "cus_123",
                    ephemeralKeySecret = "some_secret",
                ),
            ),
        ).getOrThrow()

        assertThat(result.linkState?.configuration?.signupMode).isEqualTo(AlongsideSaveForFutureUse)
    }

    @Test
    fun `Disables Link if BDCC collects anything`() = runTest {
        val loader = createPaymentSheetLoader()

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration(
                merchantDisplayName = "Some Name",
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                ),
            ),
        ).getOrThrow()

        assertThat(result.linkState).isNull()
    }

    private fun createPaymentSheetLoader(
        isGooglePayReady: Boolean = true,
        stripeIntent: StripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        customerRepo: CustomerRepository = customerRepository,
        linkAccountState: AccountStatus = AccountStatus.Verified,
        error: Throwable? = null,
        linkSettings: ElementsSession.LinkSettings? = null,
        isGooglePayEnabledFromBackend: Boolean = true,
        fallbackError: Throwable? = null,
        isCbcEligible: Boolean = false,
    ): PaymentSheetLoader {
        return DefaultPaymentSheetLoader(
            prefsRepositoryFactory = { prefsRepository },
            googlePayRepositoryFactory = {
                if (isGooglePayReady) readyGooglePayRepository else unreadyGooglePayRepository
            },
            elementsSessionRepository = FakeElementsSessionRepository(
                stripeIntent = stripeIntent,
                error = error,
                sessionsError = fallbackError,
                linkSettings = linkSettings,
                isGooglePayEnabled = isGooglePayEnabledFromBackend,
                isCbcEligible = isCbcEligible,
            ),
            customerRepository = customerRepo,
            lpmRepository = lpmRepository,
            logger = Logger.noop(),
            eventReporter = eventReporter,
            workContext = testDispatcher,
            accountStatusProvider = { linkAccountState },
            linkStore = mock(),
        )
    }

    private fun mockConfiguration(
        customer: PaymentSheet.CustomerConfiguration? = null,
        isGooglePayEnabled: Boolean = true,
        shippingDetails: AddressDetails? = null,
        defaultBillingDetails: PaymentSheet.BillingDetails? = null,
    ): PaymentSheet.Configuration {
        return PaymentSheet.Configuration(
            merchantDisplayName = "Merchant",
            customer = customer,
            shippingDetails = shippingDetails,
            defaultBillingDetails = defaultBillingDetails,
            googlePay = PaymentSheet.GooglePayConfiguration(
                environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                countryCode = CountryCode.US.value
            ).takeIf { isGooglePayEnabled }
        )
    }

    private companion object {
        private val PAYMENT_METHODS =
            listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD) + PaymentMethodFixtures.createCards(5)
    }
}
