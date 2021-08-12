package com.stripe.android.paymentsheet.flowcontroller

import com.google.common.truth.Truth.assertThat
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
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.StripeIntentRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class DefaultFlowControllerInitializerTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val prefsRepository = FakePrefsRepository()
    private val initializer = createInitializer()

    private val stripeIntentRepository =
        StripeIntentRepository.Static(PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD)
    private val paymentMethodsRepository = FakeCustomerRepository(PAYMENT_METHODS)

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

    @AfterTest
    fun after() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `init without configuration should return expected result`() =
        testDispatcher.runBlockingTest {
            assertThat(
                initializer.init(
                    PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                    stripeIntentRepository,
                    paymentMethodsRepository
                )
            ).isEqualTo(
                FlowControllerInitializer.InitResult.Success(
                    InitData(
                        null,
                        PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                        PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                        listOf(PaymentMethod.Type.Card),
                        emptyList(),
                        SavedSelection.None,
                        isGooglePayReady = false
                    )
                )
            )
        }

    @Test
    fun `init with configuration should return expected result`() = testDispatcher.runBlockingTest {
        prefsRepository.savePaymentSelection(
            PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )

        assertThat(
            initializer.init(
                PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                stripeIntentRepository,
                paymentMethodsRepository,
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            )
        ).isEqualTo(
            FlowControllerInitializer.InitResult.Success(
                InitData(
                    PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                    PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                    PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                    listOf(PaymentMethod.Type.Card),
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
        testDispatcher.runBlockingTest {
            prefsRepository.savePaymentSelection(null)

            val initializer = createInitializer()
            assertThat(
                initializer.init(
                    PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                    stripeIntentRepository,
                    paymentMethodsRepository,
                    PaymentSheetFixtures.CONFIG_GOOGLEPAY
                )
            ).isEqualTo(
                FlowControllerInitializer.InitResult.Success(
                    InitData(
                        PaymentSheetFixtures.CONFIG_GOOGLEPAY,
                        PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                        PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                        listOf(PaymentMethod.Type.Card),
                        emptyList(),
                        SavedSelection.GooglePay,
                        isGooglePayReady = true
                    )
                )
            )
        }

    @Test
    fun `init() with no customer, and google pay not ready should default saved selection to none`() =
        testDispatcher.runBlockingTest {
            prefsRepository.savePaymentSelection(null)

            val initializer = createInitializer(isGooglePayReady = false)
            assertThat(
                initializer.init(
                    PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                    stripeIntentRepository,
                    paymentMethodsRepository,
                    PaymentSheetFixtures.CONFIG_GOOGLEPAY
                )
            ).isEqualTo(
                FlowControllerInitializer.InitResult.Success(
                    InitData(
                        PaymentSheetFixtures.CONFIG_GOOGLEPAY,
                        PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                        PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                        listOf(PaymentMethod.Type.Card),
                        emptyList(),
                        SavedSelection.None,
                        isGooglePayReady = false
                    )
                )
            )
        }

    @Test
    fun `init() with customer payment methods, and google pay ready should default saved selection to last payment method`() =
        testDispatcher.runBlockingTest {
            prefsRepository.savePaymentSelection(null)

            val initializer = createInitializer()
            assertThat(
                initializer.init(
                    PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                    stripeIntentRepository,
                    paymentMethodsRepository,
                    PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
                )
            ).isEqualTo(
                FlowControllerInitializer.InitResult.Success(
                    InitData(
                        PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                        PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                        PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                        listOf(PaymentMethod.Type.Card),
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
        testDispatcher.runBlockingTest {
            prefsRepository.savePaymentSelection(null)

            val initializer = createInitializer(
                isGooglePayReady = false
            )
            assertThat(
                initializer.init(
                    PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                    stripeIntentRepository,
                    FakeCustomerRepository(emptyList()),
                    PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
                )
            ).isEqualTo(
                FlowControllerInitializer.InitResult.Success(
                    InitData(
                        PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                        PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                        PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                        listOf(PaymentMethod.Type.Card),
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
        testDispatcher.runBlockingTest {
            prefsRepository.savePaymentSelection(null)

            val initializer = createInitializer()
            assertThat(
                initializer.init(
                    PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                    stripeIntentRepository,
                    FakeCustomerRepository(emptyList()),
                    PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
                )
            ).isEqualTo(
                FlowControllerInitializer.InitResult.Success(
                    InitData(
                        PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                        PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                        PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                        listOf(PaymentMethod.Type.Card),
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
        testDispatcher.runBlockingTest {
            val paymentMethodsRepository = mock<CustomerRepository> {
                whenever(it.getPaymentMethods(any(), any())).thenReturn(emptyList())
            }

            val initializer = createInitializer()
            val paymentMethodTypes = listOf(
                "card", // valid and supported
                "fpx", // valid but not supported
                "invalid_type" // unknown type
            )

            initializer.init(
                PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                StripeIntentRepository.Static(
                    PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                        paymentMethodTypes = paymentMethodTypes
                    )
                ),
                paymentMethodsRepository,
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            )

            verify(paymentMethodsRepository).getPaymentMethods(any(), capture(paymentMethodTypeCaptor))
            assertThat(paymentMethodTypeCaptor.allValues.flatten())
                .containsExactly(PaymentMethod.Type.Card)
        }

    @Test
    fun `init() with customer should filter out invalid payment method types`() =
        testDispatcher.runBlockingTest {
            val initResult = createInitializer().init(
                PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                StripeIntentRepository.Static(PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD),
                FakeCustomerRepository(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(card = null), // invalid
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                ),
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            ) as FlowControllerInitializer.InitResult.Success

            assertThat(initResult.initData.paymentMethods)
                .containsExactly(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        }

    @Test
    fun `init() when PaymentIntent has invalid status should return null`() =
        testDispatcher.runBlockingTest {
            val result = createInitializer().init(
                PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                StripeIntentRepository.Static(
                    PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                        status = StripeIntent.Status.Succeeded
                    )
                ),
                paymentMethodsRepository,
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            )
            assertThat(result)
                .isInstanceOf(FlowControllerInitializer.InitResult::class.java)
        }

    @Test
    fun `init() when PaymentIntent has invalid confirmationMethod should return null`() =
        testDispatcher.runBlockingTest {
            val result = createInitializer().init(
                PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                StripeIntentRepository.Static(
                    PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                        confirmationMethod = PaymentIntent.ConfirmationMethod.Manual
                    )
                ),
                paymentMethodsRepository
            )
            assertThat(result)
                .isInstanceOf(FlowControllerInitializer.InitResult::class.java)
        }

    private fun createInitializer(
        isGooglePayReady: Boolean = true
    ): FlowControllerInitializer {
        return DefaultFlowControllerInitializer(
            { prefsRepository },
            { if (isGooglePayReady) readyGooglePayRepository else unreadyGooglePayRepository },
            testDispatcher
        )
    }

    private companion object {
        private val PAYMENT_METHODS =
            listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD) + PaymentMethodFixtures.createCards(5)
    }
}
