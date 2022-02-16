package com.stripe.android.paymentsheet.flowcontroller

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.FakeCustomerRepository
import com.stripe.android.paymentsheet.FakePrefsRepository
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.StripeIntentValidator
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.StripeIntentRepository
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
internal class DefaultFlowControllerInitializerTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private val stripeIntentRepository =
        StripeIntentRepository.Static(PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD)
    private val customerRepository = FakeCustomerRepository(PAYMENT_METHODS)

    private val prefsRepository = FakePrefsRepository()
    private val initializer = createInitializer()

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
    fun `init without configuration should return expected result`() =
        runTest {
            assertThat(
                initializer.init(
                    PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET
                )
            ).isEqualTo(
                FlowControllerInitializer.InitResult.Success(
                    InitData(
                        null,
                        PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                        PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                        emptyList(),
                        SavedSelection.None,
                        isGooglePayReady = false
                    )
                )
            )
        }

    @Test
    fun `init with configuration should return expected result`() = runTest {
        prefsRepository.savePaymentSelection(
            PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )

        assertThat(
            initializer.init(
                PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            )
        ).isEqualTo(
            FlowControllerInitializer.InitResult.Success(
                InitData(
                    PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                    PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                    PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                    PAYMENT_METHODS,
                    SavedSelection.PaymentMethod(
                        id = "pm_123456789"
                    ),
                    isGooglePayReady = true
                )
            )
        )
    }

    @Test
    fun `init() with no customer, and google pay ready should default saved selection to google pay`() =
        runTest {
            prefsRepository.savePaymentSelection(null)

            val initializer = createInitializer()
            assertThat(
                initializer.init(
                    PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                    PaymentSheetFixtures.CONFIG_GOOGLEPAY
                )
            ).isEqualTo(
                FlowControllerInitializer.InitResult.Success(
                    InitData(
                        PaymentSheetFixtures.CONFIG_GOOGLEPAY,
                        PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                        PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                        emptyList(),
                        SavedSelection.GooglePay,
                        isGooglePayReady = true
                    )
                )
            )
        }

    @Test
    fun `init() with no customer, and google pay not ready should default saved selection to none`() =
        runTest {
            prefsRepository.savePaymentSelection(null)

            val initializer = createInitializer(isGooglePayReady = false)
            assertThat(
                initializer.init(
                    PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                    PaymentSheetFixtures.CONFIG_GOOGLEPAY
                )
            ).isEqualTo(
                FlowControllerInitializer.InitResult.Success(
                    InitData(
                        PaymentSheetFixtures.CONFIG_GOOGLEPAY,
                        PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                        PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                        emptyList(),
                        SavedSelection.None,
                        isGooglePayReady = false
                    )
                )
            )
        }

    @Test
    fun `init() with customer payment methods, and google pay ready should default saved selection to last payment method`() =
        runTest {
            prefsRepository.savePaymentSelection(null)

            val initializer = createInitializer()
            assertThat(
                initializer.init(
                    PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                    PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
                )
            ).isEqualTo(
                FlowControllerInitializer.InitResult.Success(
                    InitData(
                        PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                        PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                        PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                        PAYMENT_METHODS,
                        SavedSelection.PaymentMethod("pm_123456789"),
                        isGooglePayReady = true
                    )
                )
            )

            assertThat(prefsRepository.getSavedSelection(true))
                .isEqualTo(
                    SavedSelection.PaymentMethod(
                        id = "pm_123456789"
                    )
                )
        }

    @Test
    fun `init() with customer, no methods, and google pay not ready, should set first payment method as google pay`() =
        runTest {
            prefsRepository.savePaymentSelection(null)

            val initializer = createInitializer(
                isGooglePayReady = false,
                customerRepo = FakeCustomerRepository(emptyList())
            )
            assertThat(
                initializer.init(
                    PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                    PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
                )
            ).isEqualTo(
                FlowControllerInitializer.InitResult.Success(
                    InitData(
                        PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                        PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                        PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                        emptyList(),
                        SavedSelection.None,
                        isGooglePayReady = false
                    )
                )
            )

            assertThat(prefsRepository.getSavedSelection(true))
                .isEqualTo(SavedSelection.None)
        }

    @Test
    fun `init() with customer, no methods, and google pay ready, should set first payment method as none`() =
        runTest {
            prefsRepository.savePaymentSelection(null)

            val initializer = createInitializer(
                customerRepo = FakeCustomerRepository(emptyList())
            )
            assertThat(
                initializer.init(
                    PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                    PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
                )
            ).isEqualTo(
                FlowControllerInitializer.InitResult.Success(
                    InitData(
                        PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                        PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                        PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                        emptyList(),
                        SavedSelection.GooglePay,
                        isGooglePayReady = true
                    )
                )
            )

            assertThat(prefsRepository.getSavedSelection(true))
                .isEqualTo(SavedSelection.GooglePay)
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
            val initializer = createInitializer(
                stripeIntentRepo = StripeIntentRepository.Static(
                    PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                        paymentMethodTypes = paymentMethodTypes
                    )
                ),
                customerRepo = customerRepository
            )

            initializer.init(
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

            val initializer = createInitializer(
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

            initializer.init(
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
            val initResult = createInitializer(
                stripeIntentRepo = StripeIntentRepository.Static(
                    PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
                ),
                customerRepo = FakeCustomerRepository(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(card = null), // invalid
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                )
            ).init(
                PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            ) as FlowControllerInitializer.InitResult.Success

            assertThat(initResult.initData.paymentMethods)
                .containsExactly(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        }

    @Test
    fun `init() when PaymentIntent has invalid status should return null`() =
        runTest {
            val result = createInitializer(
                stripeIntentRepo = StripeIntentRepository.Static(
                    PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                        status = StripeIntent.Status.Succeeded
                    )
                )
            ).init(
                PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            )
            assertThat(result)
                .isInstanceOf(FlowControllerInitializer.InitResult::class.java)
        }

    @Test
    fun `init() when PaymentIntent has invalid confirmationMethod should return null`() =
        runTest {
            val result = createInitializer(
                stripeIntentRepo = StripeIntentRepository.Static(
                    PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                        confirmationMethod = PaymentIntent.ConfirmationMethod.Manual
                    )
                )
            ).init(
                PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET
            )
            assertThat(result)
                .isInstanceOf(FlowControllerInitializer.InitResult::class.java)
        }

    private fun createInitializer(
        isGooglePayReady: Boolean = true,
        stripeIntentRepo: StripeIntentRepository = stripeIntentRepository,
        customerRepo: CustomerRepository = customerRepository
    ): FlowControllerInitializer {
        return DefaultFlowControllerInitializer(
            { prefsRepository },
            { if (isGooglePayReady) readyGooglePayRepository else unreadyGooglePayRepository },
            stripeIntentRepo,
            StripeIntentValidator(),
            customerRepo,
            Logger.noop(),
            testDispatcher
        )
    }

    private companion object {
        private val PAYMENT_METHODS =
            listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD) + PaymentMethodFixtures.createCards(5)
    }
}
