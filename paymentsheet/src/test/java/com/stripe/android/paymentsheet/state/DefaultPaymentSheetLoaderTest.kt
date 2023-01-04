package com.stripe.android.paymentsheet.state

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.model.CountryCode
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.FakePrefsRepository
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.StripeIntentValidator
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.StripeIntentRepository
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.StaticLpmResourceRepository
import com.stripe.android.utils.FakeCustomerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class DefaultPaymentSheetLoaderTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val eventReporter = mock<EventReporter>()

    private val customerRepository = FakeCustomerRepository(PAYMENT_METHODS)
    private val lpmResourceRepository = StaticLpmResourceRepository(
        LpmRepository(
            LpmRepository.LpmRepositoryArguments(ApplicationProvider.getApplicationContext<Application>().resources)
        ).apply {
            this.forceUpdate(listOf(PaymentMethod.Type.Card.code, PaymentMethod.Type.USBankAccount.code), null)
        }
    )

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
    fun `load without configuration should return expected result`() = runTest {
        val stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val loader = createPaymentSheetLoader(stripeIntent = stripeIntent)

        assertThat(
            loader.load(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                paymentSheetConfiguration = null,
            )
        ).isEqualTo(
            PaymentSheetLoader.Result.Success(
                PaymentSheetState.Full(
                    config = null,
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                    stripeIntent = stripeIntent,
                    supportedPaymentMethodTypes = listOf(PaymentMethod.Type.Card.code),
                    customerPaymentMethods = emptyList(),
                    savedSelection = SavedSelection.Link,
                    isGooglePayReady = false,
                    newPaymentSelection = null,
                    linkState = LinkState(
                        configuration = LinkPaymentLauncher.Configuration(
                            stripeIntent = stripeIntent,
                            merchantName = "App Name",
                            customerName = null,
                            customerEmail = null,
                            customerPhone = null,
                            customerBillingCountryCode = null,
                            shippingValues = null,
                        ),
                        loginState = LinkState.LoginState.LoggedIn,
                    ),
                )
            )
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
                PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            )
        ).isEqualTo(
            PaymentSheetLoader.Result.Success(
                PaymentSheetState.Full(
                    config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                    stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
                    supportedPaymentMethodTypes = listOf(PaymentMethod.Type.Card.code),
                    customerPaymentMethods = PAYMENT_METHODS,
                    savedSelection = SavedSelection.PaymentMethod(id = "pm_123456789"),
                    isGooglePayReady = true,
                    newPaymentSelection = null,
                    linkState = null,
                )
            )
        )
    }

    @Test
    fun `load() with no customer and google pay ready should default to Link`() = runTest {
        prefsRepository.savePaymentSelection(null)

        val stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val loader = createPaymentSheetLoader(stripeIntent = stripeIntent)

        assertThat(
            loader.load(
                PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                PaymentSheetFixtures.CONFIG_GOOGLEPAY
            )
        ).isEqualTo(
            PaymentSheetLoader.Result.Success(
                PaymentSheetState.Full(
                    config = PaymentSheetFixtures.CONFIG_GOOGLEPAY,
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                    stripeIntent = stripeIntent,
                    supportedPaymentMethodTypes = listOf(PaymentMethod.Type.Card.code),
                    customerPaymentMethods = emptyList(),
                    savedSelection = SavedSelection.Link,
                    isGooglePayReady = true,
                    newPaymentSelection = null,
                    linkState = LinkState(
                        configuration = LinkPaymentLauncher.Configuration(
                            stripeIntent = stripeIntent,
                            merchantName = "Merchant, Inc.",
                            customerName = null,
                            customerEmail = null,
                            customerPhone = null,
                            customerBillingCountryCode = null,
                            shippingValues = null,
                        ),
                        loginState = LinkState.LoginState.LoggedIn,
                    ),
                )
            )
        )
    }

    @Test
    fun `load() with no customer and Google Pay not ready should default to Link`() = runTest {
        prefsRepository.savePaymentSelection(null)

        val expectedIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val expectedLinkState = LinkState(
            configuration = LinkPaymentLauncher.Configuration(
                stripeIntent = expectedIntent,
                merchantName = "Merchant, Inc.",
                customerName = null,
                customerEmail = null,
                customerPhone = null,
                customerBillingCountryCode = null,
                shippingValues = null,
            ),
            loginState = LinkState.LoginState.LoggedIn,
        )

        val loader = createPaymentSheetLoader(isGooglePayReady = false)

        assertThat(
            loader.load(
                PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                PaymentSheetFixtures.CONFIG_GOOGLEPAY
            )
        ).isEqualTo(
            PaymentSheetLoader.Result.Success(
                PaymentSheetState.Full(
                    config = PaymentSheetFixtures.CONFIG_GOOGLEPAY,
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                    stripeIntent = expectedIntent,
                    supportedPaymentMethodTypes = listOf(PaymentMethod.Type.Card.code),
                    customerPaymentMethods = emptyList(),
                    savedSelection = SavedSelection.Link,
                    isGooglePayReady = false,
                    newPaymentSelection = null,
                    linkState = expectedLinkState,
                )
            )
        )
    }

    @Test
    fun `load() with payment methods, and google pay ready should default to last payment method`() =
        runTest {
            prefsRepository.savePaymentSelection(null)

            val loader = createPaymentSheetLoader(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
            )

            assertThat(
                loader.load(
                    PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                    PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
                )
            ).isEqualTo(
                PaymentSheetLoader.Result.Success(
                    PaymentSheetState.Full(
                        config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                        clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
                        supportedPaymentMethodTypes = listOf(PaymentMethod.Type.Card.code),
                        customerPaymentMethods = PAYMENT_METHODS,
                        savedSelection = SavedSelection.PaymentMethod("pm_123456789"),
                        isGooglePayReady = true,
                        newPaymentSelection = null,
                        linkState = null,
                    )
                )
            )

            assertThat(prefsRepository.getSavedSelection(true, true))
                .isEqualTo(
                    SavedSelection.PaymentMethod(
                        id = "pm_123456789"
                    )
                )
        }

    @Test
    fun `load() with customer, no methods, and google pay not ready, should set first payment method as Link`() =
        runTest {
            val expectedIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
            val expectedLinkState = LinkState(
                configuration = LinkPaymentLauncher.Configuration(
                    stripeIntent = expectedIntent,
                    merchantName = "Merchant, Inc.",
                    customerName = null,
                    customerEmail = null,
                    customerPhone = null,
                    customerBillingCountryCode = null,
                    shippingValues = null,
                ),
                loginState = LinkState.LoginState.LoggedIn,
            )

            prefsRepository.savePaymentSelection(null)

            val loader = createPaymentSheetLoader(
                isGooglePayReady = false,
                customerRepo = FakeCustomerRepository(emptyList())
            )
            assertThat(
                loader.load(
                    PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                    PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
                )
            ).isEqualTo(
                PaymentSheetLoader.Result.Success(
                    PaymentSheetState.Full(
                        PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                        PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                        expectedIntent,
                        supportedPaymentMethodTypes = listOf(PaymentMethod.Type.Card.code),
                        newPaymentSelection = null,
                        customerPaymentMethods = emptyList(),
                        savedSelection = SavedSelection.Link,
                        isGooglePayReady = false,
                        linkState = expectedLinkState,
                    )
                )
            )

            assertThat(prefsRepository.getSavedSelection(true, true))
                .isEqualTo(SavedSelection.Link)
        }

    @Test
    fun `load() with customer, no methods, and google pay ready, should set first payment method as Link`() =
        runTest {
            val expectedIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
            val expectedLinkState = LinkState(
                configuration = LinkPaymentLauncher.Configuration(
                    stripeIntent = expectedIntent,
                    merchantName = "Merchant, Inc.",
                    customerName = null,
                    customerEmail = null,
                    customerPhone = null,
                    customerBillingCountryCode = null,
                    shippingValues = null,
                ),
                loginState = LinkState.LoginState.LoggedIn,
            )

            prefsRepository.savePaymentSelection(null)

            val loader = createPaymentSheetLoader(
                customerRepo = FakeCustomerRepository(emptyList())
            )
            assertThat(
                loader.load(
                    PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                    PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
                )
            ).isEqualTo(
                PaymentSheetLoader.Result.Success(
                    PaymentSheetState.Full(
                        PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                        PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                        expectedIntent,
                        supportedPaymentMethodTypes = listOf(PaymentMethod.Type.Card.code),
                        newPaymentSelection = null,
                        customerPaymentMethods = emptyList(),
                        savedSelection = SavedSelection.Link,
                        isGooglePayReady = true,
                        linkState = expectedLinkState,
                    )
                )
            )

            assertThat(prefsRepository.getSavedSelection(true, true))
                .isEqualTo(SavedSelection.Link)
        }

    @Test
    fun `load() with customer should fetch only supported payment method types`() =
        runTest {
            val customerRepository = mock<CustomerRepository> {
                whenever(it.getPaymentMethods(any(), any())).thenReturn(emptyList())
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
                PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            )

            verify(customerRepository).getPaymentMethods(
                any(),
                capture(paymentMethodTypeCaptor)
            )
            assertThat(paymentMethodTypeCaptor.allValues.flatten())
                .containsExactly(PaymentMethod.Type.Card)
        }

    @Test
    fun `when allowsDelayedPaymentMethods is false then delayed payment methods are filtered out`() =
        runTest {
            val customerRepository = mock<CustomerRepository> {
                whenever(it.getPaymentMethods(any(), any())).thenReturn(emptyList())
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
                PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            )

            verify(customerRepository).getPaymentMethods(
                any(),
                capture(paymentMethodTypeCaptor)
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
                PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            ) as PaymentSheetLoader.Result.Success

            assertThat(result.state.customerPaymentMethods)
                .containsExactly(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        }

    @Test
    fun `load() when PaymentIntent has invalid status should return null`() =
        runTest {
            val result = createPaymentSheetLoader(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    status = StripeIntent.Status.Succeeded
                ),
            ).load(
                PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            )
            assertThat(result)
                .isInstanceOf(PaymentSheetLoader.Result::class.java)
        }

    @Test
    fun `load() when PaymentIntent has invalid confirmationMethod should return null`() =
        runTest {
            val result = createPaymentSheetLoader(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    confirmationMethod = PaymentIntent.ConfirmationMethod.Manual
                ),
            ).load(
                PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET
            )
            assertThat(result)
                .isInstanceOf(PaymentSheetLoader.Result::class.java)
        }

    @Test
    fun `Defaults to Google Play for guests if Link is not enabled`() = runTest {
        val result = createPaymentSheetLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
        ).load(
            clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
            paymentSheetConfiguration = mockConfiguration()
        ) as PaymentSheetLoader.Result.Success

        assertThat(result.state.linkState).isNull()
        assertThat(result.state.savedSelection).isEqualTo(SavedSelection.GooglePay)
    }

    @Test
    fun `Defaults to Link for guests if available`() = runTest {
        val result = createPaymentSheetLoader().load(
            clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
            // The mock configuration is necessary to enable Google Pay. We want to do that,
            // so that we can check that Link is preferred.
            paymentSheetConfiguration = mockConfiguration()
        ) as PaymentSheetLoader.Result.Success

        assertThat(result.state.savedSelection).isEqualTo(SavedSelection.Link)
    }

    @Test
    fun `Defaults to first existing payment method for known customer`() = runTest {
        val result = createPaymentSheetLoader(
            customerRepo = FakeCustomerRepository(paymentMethods = PAYMENT_METHODS)
        ).load(
            clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
            paymentSheetConfiguration = mockConfiguration(
                customer = PaymentSheet.CustomerConfiguration(
                    id = "some_id",
                    ephemeralKeySecret = "some_key"
                )
            )
        ) as PaymentSheetLoader.Result.Success

        val expectedPaymentMethodId = requireNotNull(PAYMENT_METHODS.first().id)
        assertThat(result.state.savedSelection).isEqualTo(SavedSelection.PaymentMethod(id = expectedPaymentMethodId))
    }

    @Test
    fun `Considers Link logged in if the account is verified`() = runTest {
        val loader = createPaymentSheetLoader(linkAccountState = AccountStatus.Verified)

        val result = loader.load(
            clientSecret = PaymentIntentClientSecret("secret"),
            paymentSheetConfiguration = mockConfiguration(),
        ) as PaymentSheetLoader.Result.Success

        assertThat(result.state.linkState?.loginState).isEqualTo(LinkState.LoginState.LoggedIn)
    }

    @Test
    fun `Considers Link as needing verification if the account needs verification`() = runTest {
        val loader = createPaymentSheetLoader(linkAccountState = AccountStatus.NeedsVerification)

        val result = loader.load(
            clientSecret = PaymentIntentClientSecret("secret"),
            paymentSheetConfiguration = mockConfiguration(),
        ) as PaymentSheetLoader.Result.Success

        assertThat(result.state.linkState?.loginState).isEqualTo(LinkState.LoginState.NeedsVerification)
    }

    @Test
    fun `Considers Link as needing verification if the account is being verified`() = runTest {
        val loader = createPaymentSheetLoader(linkAccountState = AccountStatus.VerificationStarted)

        val result = loader.load(
            clientSecret = PaymentIntentClientSecret("secret"),
            paymentSheetConfiguration = mockConfiguration(),
        ) as PaymentSheetLoader.Result.Success

        assertThat(result.state.linkState?.loginState).isEqualTo(LinkState.LoginState.NeedsVerification)
    }

    @Test
    fun `Considers Link as logged out correctly`() = runTest {
        val loader = createPaymentSheetLoader(linkAccountState = AccountStatus.SignedOut)

        val result = loader.load(
            clientSecret = PaymentIntentClientSecret("secret"),
            paymentSheetConfiguration = mockConfiguration(),
        ) as PaymentSheetLoader.Result.Success

        assertThat(result.state.linkState?.loginState).isEqualTo(LinkState.LoginState.LoggedOut)
    }

    @Test
    fun `Populates Link configuration correctly from billing details`() = runTest {
        val loader = createPaymentSheetLoader()

        val billingDetails = PaymentSheet.BillingDetails(
            address = PaymentSheet.Address(country = "CA"),
            name = "Till",
        )

        val result = loader.load(
            clientSecret = PaymentIntentClientSecret("secret"),
            paymentSheetConfiguration = mockConfiguration(
                defaultBillingDetails = billingDetails,
            ),
        ) as PaymentSheetLoader.Result.Success

        val expectedLinkConfig = LinkPaymentLauncher.Configuration(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            merchantName = "Merchant",
            customerName = "Till",
            customerEmail = null,
            customerPhone = null,
            customerBillingCountryCode = "CA",
            shippingValues = null,
        )

        assertThat(result.state.linkState?.configuration).isEqualTo(expectedLinkConfig)
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
            clientSecret = PaymentIntentClientSecret("secret"),
            paymentSheetConfiguration = mockConfiguration(
                shippingDetails = shippingDetails,
            ),
        ) as PaymentSheetLoader.Result.Success

        assertThat(result.state.linkState?.configuration?.shippingValues).isNotNull()
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
            clientSecret = PaymentIntentClientSecret("secret"),
            paymentSheetConfiguration = mockConfiguration(
                shippingDetails = shippingDetails,
                defaultBillingDetails = billingDetails,
            ),
        ) as PaymentSheetLoader.Result.Success

        assertThat(result.state.linkState?.configuration?.customerPhone)
            .isEqualTo(shippingDetails.phoneNumber)
    }

    @Test
    fun `Falls back to customer email if billing address email not provided`() = runTest {
        val loader = createPaymentSheetLoader(
            customerRepo = FakeCustomerRepository(
                customer = mock {
                    on { email } doReturn "email@stripe.com"
                }
            ),
        )

        val result = loader.load(
            clientSecret = PaymentIntentClientSecret("secret"),
            paymentSheetConfiguration = mockConfiguration(
                customer = PaymentSheet.CustomerConfiguration(id = "id", ephemeralKeySecret = "key"),
            ),
        ) as PaymentSheetLoader.Result.Success

        assertThat(result.state.linkState?.configuration?.customerEmail)
            .isEqualTo("email@stripe.com")
    }

    @Test
    fun `Verify supported payment methods exclude afterpay if no shipping and no allow flag`() = runTest {
        val loader = createPaymentSheetLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("afterpay_clearpay", "card"),
                shipping = null,
            ),
        )

        val result = loader.load(
            clientSecret = PaymentIntentClientSecret("secret"),
            paymentSheetConfiguration = mockConfiguration(
                allowsPaymentMethodsRequiringShippingAddress = false
            )
        )

        assertThat(result)
            .isInstanceOf(PaymentSheetLoader.Result.Success::class.java)
        assertThat((result as PaymentSheetLoader.Result.Success).state.supportedPaymentMethodTypes)
            .doesNotContain("afterpay_clearpay")
    }

    @Test
    fun `Verify supported payment methods include afterpay if allow flag but no shipping`() = runTest {
        val loader = createPaymentSheetLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("afterpay_clearpay"),
                shipping = null,
            ),
        )

        val result = loader.load(
            clientSecret = PaymentIntentClientSecret("secret"),
            paymentSheetConfiguration = mockConfiguration(
                allowsPaymentMethodsRequiringShippingAddress = true
            )
        )

        assertThat(result)
            .isInstanceOf(PaymentSheetLoader.Result.Success::class.java)
        assertThat((result as PaymentSheetLoader.Result.Success).state.supportedPaymentMethodTypes)
            .containsExactly("afterpay_clearpay")
    }

    @Test
    fun `Verify supported payment methods include afterpay if shipping but no allow flag`() = runTest {
        val loader = createPaymentSheetLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("afterpay_clearpay"),
                shipping = PaymentIntent.Shipping(
                    address = Address("city")
                )
            ),
        )

        val result = loader.load(
            clientSecret = PaymentIntentClientSecret("secret"),
            paymentSheetConfiguration = mockConfiguration(
                allowsPaymentMethodsRequiringShippingAddress = true
            )
        )

        assertThat(result)
            .isInstanceOf(PaymentSheetLoader.Result.Success::class.java)
        assertThat((result as PaymentSheetLoader.Result.Success).state.supportedPaymentMethodTypes)
            .containsExactly("afterpay_clearpay")
    }

    @Test
    fun `getSupportedPaymentMethods() filters payment methods with delayed settlement`() = runTest {
        val loader = createPaymentSheetLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf(
                    PaymentMethod.Type.Card.code,
                    PaymentMethod.Type.Ideal.code,
                    PaymentMethod.Type.SepaDebit.code,
                    PaymentMethod.Type.Sofort.code,
                )
            ),
        )

        val result = loader.load(
            clientSecret = PaymentIntentClientSecret("secret"),
            paymentSheetConfiguration = mockConfiguration(
                allowsPaymentMethodsRequiringShippingAddress = true
            )
        )

        assertThat(result)
            .isInstanceOf(PaymentSheetLoader.Result.Success::class.java)
        assertThat((result as PaymentSheetLoader.Result.Success).state.supportedPaymentMethodTypes)
            .containsExactly("card", "ideal")
    }

    @Test
    fun `getSupportedPaymentMethods() does not filter payment methods when supportsDelayedSettlement = true`() = runTest {
        val loader = createPaymentSheetLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf(
                    PaymentMethod.Type.Card.code,
                    PaymentMethod.Type.Ideal.code,
                    PaymentMethod.Type.SepaDebit.code,
                    PaymentMethod.Type.Sofort.code,
                )
            ),
        )

        val result = loader.load(
            clientSecret = PaymentIntentClientSecret("secret"),
            paymentSheetConfiguration = mockConfiguration(
                allowsPaymentMethodsRequiringShippingAddress = true,
                allowsDelayedPaymentMethods = true
            )
        )

        assertThat(result)
            .isInstanceOf(PaymentSheetLoader.Result.Success::class.java)
        assertThat((result as PaymentSheetLoader.Result.Success).state.supportedPaymentMethodTypes)
            .containsExactly("card", "ideal", "sepa_debit", "sofort")
    }

    private fun createPaymentSheetLoader(
        isGooglePayReady: Boolean = true,
        stripeIntent: StripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        customerRepo: CustomerRepository = customerRepository,
        linkAccountState: AccountStatus = AccountStatus.Verified,
    ): PaymentSheetLoader {
        return DefaultPaymentSheetLoader(
            appName = "App Name",
            prefsRepositoryFactory = { prefsRepository },
            googlePayRepositoryFactory = {
                if (isGooglePayReady) readyGooglePayRepository else unreadyGooglePayRepository
            },
            stripeIntentRepository = StripeIntentRepository.Static(stripeIntent),
            stripeIntentValidator = StripeIntentValidator(),
            customerRepository = customerRepo,
            lpmResourceRepository = lpmResourceRepository,
            logger = Logger.noop(),
            eventReporter = eventReporter,
            workContext = testDispatcher,
            accountStatusProvider = { linkAccountState },
        )
    }

    private fun mockConfiguration(
        customer: PaymentSheet.CustomerConfiguration? = null,
        isGooglePayEnabled: Boolean = true,
        shippingDetails: AddressDetails? = null,
        defaultBillingDetails: PaymentSheet.BillingDetails? = null,
        allowsPaymentMethodsRequiringShippingAddress: Boolean = false,
        allowsDelayedPaymentMethods: Boolean = false
    ): PaymentSheet.Configuration {
        return PaymentSheet.Configuration(
            merchantDisplayName = "Merchant",
            customer = customer,
            shippingDetails = shippingDetails,
            defaultBillingDetails = defaultBillingDetails,
            googlePay = PaymentSheet.GooglePayConfiguration(
                environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                countryCode = CountryCode.US.value
            ).takeIf { isGooglePayEnabled },
            allowsPaymentMethodsRequiringShippingAddress = allowsPaymentMethodsRequiringShippingAddress,
            allowsDelayedPaymentMethods = allowsDelayedPaymentMethods
        )
    }

    private companion object {
        private val PAYMENT_METHODS =
            listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD) + PaymentMethodFixtures.createCards(5)
    }
}
