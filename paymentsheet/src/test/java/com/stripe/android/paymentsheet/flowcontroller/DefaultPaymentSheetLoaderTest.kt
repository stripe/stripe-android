package com.stripe.android.paymentsheet.flowcontroller

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.model.CountryCode
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.FakePrefsRepository
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.analytics.EventReporter
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

    private val stripeIntentRepository =
        StripeIntentRepository.Static(PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD)
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
    fun `init without configuration should return expected result`() = runTest {
        val expectedIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD

        val loader = createPaymentSheetLoader()

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
                    stripeIntent = expectedIntent,
                    customerPaymentMethods = emptyList(),
                    savedSelection = SavedSelection.Link,
                    isGooglePayReady = false,
                    isLinkEnabled = true,
                )
            )
        )
    }

    @Test
    fun `init with configuration should return expected result`() = runTest {
        prefsRepository.savePaymentSelection(
            PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )

        val expectedIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD

        val loader = createPaymentSheetLoader()

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
                    stripeIntent = expectedIntent,
                    customerPaymentMethods = PAYMENT_METHODS,
                    savedSelection = SavedSelection.PaymentMethod(id = "pm_123456789"),
                    isGooglePayReady = true,
                    isLinkEnabled = true,
                )
            )
        )
    }

    @Test
    fun `init() with no customer and google pay ready should default to Link`() = runTest {
        prefsRepository.savePaymentSelection(null)

        val loader = createPaymentSheetLoader()
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
                    stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                    customerPaymentMethods = emptyList(),
                    savedSelection = SavedSelection.Link,
                    isGooglePayReady = true,
                    isLinkEnabled = true,
                )
            )
        )
    }

    @Test
    fun `init() with no customer and Google Pay not ready should default to Link`() = runTest {
        prefsRepository.savePaymentSelection(null)

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
                    stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                    customerPaymentMethods = emptyList(),
                    savedSelection = SavedSelection.Link,
                    isGooglePayReady = false,
                    isLinkEnabled = true,
                )
            )
        )
    }

    @Test
    fun `init() with customer payment methods, and google pay ready should default saved selection to last payment method`() =
        runTest {
            prefsRepository.savePaymentSelection(null)

            val loader = createPaymentSheetLoader()
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
                        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                        customerPaymentMethods = PAYMENT_METHODS,
                        savedSelection = SavedSelection.PaymentMethod("pm_123456789"),
                        isGooglePayReady = true,
                        isLinkEnabled = true,
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
    fun `init() with customer, no methods, and google pay not ready, should set first payment method as Link`() =
        runTest {
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
                        PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                        emptyList(),
                        SavedSelection.Link,
                        isGooglePayReady = false,
                        isLinkEnabled = true,
                    )
                )
            )

            assertThat(prefsRepository.getSavedSelection(true, true))
                .isEqualTo(SavedSelection.Link)
        }

    @Test
    fun `init() with customer, no methods, and google pay ready, should set first payment method as Link`() =
        runTest {
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
                        PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                        emptyList(),
                        SavedSelection.Link,
                        isGooglePayReady = true,
                        isLinkEnabled = true,
                    )
                )
            )

            assertThat(prefsRepository.getSavedSelection(true, true))
                .isEqualTo(SavedSelection.Link)
        }

    @Test
    fun `init() with customer should fetch only supported payment method types`() =
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
                stripeIntentRepo = StripeIntentRepository.Static(
                    PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                        paymentMethodTypes = paymentMethodTypes
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
                stripeIntentRepo = StripeIntentRepository.Static(
                    PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                        paymentMethodTypes = listOf(
                            PaymentMethod.Type.Card.code,
                            PaymentMethod.Type.SepaDebit.code,
                            PaymentMethod.Type.AuBecsDebit.code
                        )
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
    fun `init() with customer should filter out invalid payment method types`() =
        runTest {
            val result = createPaymentSheetLoader(
                stripeIntentRepo = StripeIntentRepository.Static(
                    PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
                ),
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
    fun `init() when PaymentIntent has invalid status should return null`() =
        runTest {
            val result = createPaymentSheetLoader(
                stripeIntentRepo = StripeIntentRepository.Static(
                    PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                        status = StripeIntent.Status.Succeeded
                    )
                )
            ).load(
                PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            )
            assertThat(result)
                .isInstanceOf(PaymentSheetLoader.Result::class.java)
        }

    @Test
    fun `init() when PaymentIntent has invalid confirmationMethod should return null`() =
        runTest {
            val result = createPaymentSheetLoader(
                stripeIntentRepo = StripeIntentRepository.Static(
                    PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                        confirmationMethod = PaymentIntent.ConfirmationMethod.Manual
                    )
                )
            ).load(
                PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET
            )
            assertThat(result)
                .isInstanceOf(PaymentSheetLoader.Result::class.java)
        }

    @Test
    fun `Defaults to Google Play for guests if Link is not enabled`() = runTest {
        val result = createPaymentSheetLoader(
            stripeIntentRepo = StripeIntentRepository.Static(
                PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK
            )
        ).load(
            clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
            paymentSheetConfiguration = mockConfiguration()
        ) as PaymentSheetLoader.Result.Success

        assertThat(result.state.isLinkEnabled).isFalse()
        assertThat(result.state.savedSelection).isEqualTo(SavedSelection.GooglePay)
    }

    @Test
    fun `Defaults to Link for guests if available`() = runTest {
        val result = createPaymentSheetLoader(
            stripeIntentRepo = StripeIntentRepository.Static(
                PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
            ),
        ).load(
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
            stripeIntentRepo = StripeIntentRepository.Static(
                PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
            ),
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

    private fun createPaymentSheetLoader(
        isGooglePayReady: Boolean = true,
        stripeIntentRepo: StripeIntentRepository = stripeIntentRepository,
        customerRepo: CustomerRepository = customerRepository
    ): PaymentSheetLoader {
        return DefaultPaymentSheetLoader(
            { prefsRepository },
            { if (isGooglePayReady) readyGooglePayRepository else unreadyGooglePayRepository },
            stripeIntentRepo,
            StripeIntentValidator(),
            customerRepo,
            lpmResourceRepository,
            Logger.noop(),
            eventReporter,
            testDispatcher,
        )
    }

    private fun mockConfiguration(
        customer: PaymentSheet.CustomerConfiguration? = null,
        isGooglePayEnabled: Boolean = true
    ): PaymentSheet.Configuration {
        return PaymentSheet.Configuration(
            merchantDisplayName = "Merchant",
            customer = customer,
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
