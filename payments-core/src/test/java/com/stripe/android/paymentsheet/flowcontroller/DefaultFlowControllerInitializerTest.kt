package com.stripe.android.paymentsheet.flowcontroller

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.FakePaymentMethodsRepository
import com.stripe.android.paymentsheet.FakePrefsRepository
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.repositories.StripeIntentRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class DefaultFlowControllerInitializerTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val prefsRepository = FakePrefsRepository()
    private val initializer = createInitializer()

    private val stripeIntentRepository =
        StripeIntentRepository.Static(PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD)
    private val paymentMethodsRepository = FakePaymentMethodsRepository(PAYMENT_METHODS)

    @AfterTest
    fun after() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `init without configuration should return expect result`() =
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
    fun `init with configuration should return expect result`() = testDispatcher.runBlockingTest {
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
                        PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                        listOf(PaymentMethod.Type.Card),
                        PAYMENT_METHODS,
                        SavedSelection.PaymentMethod("pm_123456789"),
                        isGooglePayReady = true
                    )
                )
            )

            assertThat(prefsRepository.getSavedSelection()).isEqualTo(
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
                    FakePaymentMethodsRepository(emptyList()),
                    PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
                )
            ).isEqualTo(
                FlowControllerInitializer.InitResult.Success(
                    InitData(
                        PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                        PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                        listOf(PaymentMethod.Type.Card),
                        emptyList(),
                        SavedSelection.None,
                        isGooglePayReady = false
                    )
                )
            )

            assertThat(prefsRepository.getSavedSelection()).isEqualTo(SavedSelection.None)
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
                    FakePaymentMethodsRepository(emptyList()),
                    PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
                )
            ).isEqualTo(
                FlowControllerInitializer.InitResult.Success(
                    InitData(
                        PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                        PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                        listOf(PaymentMethod.Type.Card),
                        emptyList(),
                        SavedSelection.GooglePay,
                        isGooglePayReady = true
                    )
                )
            )

            assertThat(prefsRepository.getSavedSelection()).isEqualTo(SavedSelection.GooglePay)
        }

    private fun testDefaultSavedSelection() {
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
            { _, _ -> prefsRepository },
            { isGooglePayReady },
            testDispatcher
        )
    }

    private companion object {
        private val PAYMENT_METHODS =
            listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD) + PaymentMethodFixtures.createCards(5)
    }
}
