package com.stripe.android

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.model.Customer
import com.stripe.android.model.CustomerFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.testharness.TestEphemeralKeyProvider
import com.stripe.android.view.ActivityScenarioFactory
import com.stripe.android.view.ActivityStarter
import com.stripe.android.view.BillingAddressFields
import com.stripe.android.view.PaymentFlowActivity
import com.stripe.android.view.PaymentFlowActivityStarter
import com.stripe.android.view.PaymentMethodsActivity
import com.stripe.android.view.PaymentMethodsActivityStarter
import java.util.concurrent.ThreadPoolExecutor
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

/**
 * Test class for [PaymentSession]
 */
@RunWith(RobolectricTestRunner::class)
class PaymentSessionTest {

    private val ephemeralKeyProvider = TestEphemeralKeyProvider()

    private val threadPoolExecutor: ThreadPoolExecutor = mock()
    private val paymentSessionListener: PaymentSession.PaymentSessionListener = mock()
    private val customerSession: CustomerSession = mock()
    private val paymentMethodsActivityStarter:
        ActivityStarter<PaymentMethodsActivity, PaymentMethodsActivityStarter.Args> = mock()
    private val paymentFlowActivityStarter:
        ActivityStarter<PaymentFlowActivity, PaymentFlowActivityStarter.Args> = mock()

    private val paymentSessionDataArgumentCaptor: KArgumentCaptor<PaymentSessionData> = argumentCaptor()
    private val paymentMethodsActivityStarterArgsCaptor: KArgumentCaptor<PaymentMethodsActivityStarter.Args> = argumentCaptor()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val activityScenarioFactory = ActivityScenarioFactory(context)

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        doAnswer { invocation ->
            invocation.getArgument<Runnable>(0).run()
            null
        }.`when`<ThreadPoolExecutor>(threadPoolExecutor).execute(any())

        CustomerSession.instance = createCustomerSession()
    }

    @Test
    fun init_addsPaymentSessionToken_andFetchesCustomer() {
        createActivity {
            val paymentSession = PaymentSession(it, DEFAULT_CONFIG)
            paymentSession.init(paymentSessionListener)

            assertEquals(
                setOf(PaymentSession.TOKEN_PAYMENT_SESSION),
                CustomerSession.getInstance().productUsageTokens
            )

            verify(paymentSessionListener)
                .onCommunicatingStateChanged(eq(true))
        }
    }

    @Test
    fun init_whenEphemeralKeyProviderContinues_fetchesCustomerAndNotifiesListener() {
        ephemeralKeyProvider
            .setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)

        createActivity {
            val paymentSession = PaymentSession(it, DEFAULT_CONFIG)
            paymentSession.init(paymentSessionListener)
            verify(paymentSessionListener)
                .onCommunicatingStateChanged(true)
            verify(paymentSessionListener)
                .onCommunicatingStateChanged(false)
        }
    }

    @Test
    fun handlePaymentData_whenPaymentMethodSelected_notifiesListenerAndFetchesCustomer() {
        createActivity {
            val paymentSession = PaymentSession(it, DEFAULT_CONFIG)
            paymentSession.init(paymentSessionListener)

            // We have already tested the functionality up to here.
            reset(paymentSessionListener)

            val result = PaymentMethodsActivityStarter.Result(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
            )
            val handled = paymentSession.handlePaymentData(
                PaymentMethodsActivityStarter.REQUEST_CODE, RESULT_OK,
                Intent().putExtras(result.toBundle()))
            assertTrue(handled)

            verify(paymentSessionListener)
                .onPaymentSessionDataChanged(paymentSessionDataArgumentCaptor.capture())
            val data = paymentSessionDataArgumentCaptor.firstValue
            assertEquals(PaymentMethodFixtures.CARD_PAYMENT_METHOD, data.paymentMethod)
            assertFalse(data.useGooglePay)
        }
    }

    @Test
    fun handlePaymentData_whenGooglePaySelected_notifiesListenerAndFetchesCustomer() {
        createActivity {
            val paymentSession = PaymentSession(it, DEFAULT_CONFIG)
            paymentSession.init(paymentSessionListener)

            // We have already tested the functionality up to here.
            reset(paymentSessionListener)

            val result = PaymentMethodsActivityStarter.Result(
                useGooglePay = true
            )
            val handled = paymentSession.handlePaymentData(
                PaymentMethodsActivityStarter.REQUEST_CODE, RESULT_OK,
                Intent().putExtras(result.toBundle()))
            assertTrue(handled)

            verify(paymentSessionListener)
                .onPaymentSessionDataChanged(paymentSessionDataArgumentCaptor.capture())
            val data = paymentSessionDataArgumentCaptor.firstValue
            assertNull(data.paymentMethod)
            assertTrue(data.useGooglePay)
        }
    }

    @Test
    fun selectPaymentMethod_launchesPaymentMethodsActivityWithLog() {
        createActivity { activity ->
            val paymentSession = PaymentSession(activity, DEFAULT_CONFIG)
            paymentSession.init(paymentSessionListener)
            paymentSession.presentPaymentMethodSelection()

            val nextStartedActivityForResult =
                Shadows.shadowOf(activity).nextStartedActivityForResult
            val intent = nextStartedActivityForResult.intent

            assertThat(nextStartedActivityForResult.requestCode)
                .isEqualTo(PaymentMethodsActivityStarter.REQUEST_CODE)
            assertThat(intent.component?.className)
                .isEqualTo(PaymentMethodsActivity::class.java.name)

            val args =
                PaymentMethodsActivityStarter.Args.create(intent)
            assertEquals(BillingAddressFields.Full, args.billingAddressFields)
        }
    }

    @Test
    fun presentPaymentMethodSelection_withShouldRequirePostalCode_shouldPassInIntent() {
        createActivity { activity ->
            val paymentSession = PaymentSession(
                activity,
                PaymentSessionConfig.Builder()
                    .setShippingMethodsRequired(false)
                    .setBillingAddressFields(BillingAddressFields.PostalCode)
                    .build()
            )
            paymentSession.init(paymentSessionListener)
            paymentSession.presentPaymentMethodSelection()

            val nextStartedActivityForResult =
                Shadows.shadowOf(activity).nextStartedActivityForResult
            val intent = nextStartedActivityForResult.intent

            assertThat(nextStartedActivityForResult.requestCode)
                .isEqualTo(PaymentMethodsActivityStarter.REQUEST_CODE)
            assertThat(intent.component?.className)
                .isEqualTo(PaymentMethodsActivity::class.java.name)

            val args =
                PaymentMethodsActivityStarter.Args.create(nextStartedActivityForResult.intent)
            assertThat(args.billingAddressFields)
                .isEqualTo(BillingAddressFields.PostalCode)
        }
    }

    @Test
    fun getSelectedPaymentMethodId_whenHasPaymentSessionData_returnsExpectedId() {
        createActivity {
            val paymentSession = createPaymentSession(
                it,
                DEFAULT_CONFIG,
                PaymentSessionFixtures.PAYMENT_SESSION_DATA.copy(
                    paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
                )
            )
            paymentSession.presentPaymentMethodSelection()
            verify(paymentMethodsActivityStarter).startForResult(
                paymentMethodsActivityStarterArgsCaptor.capture()
            )
            assertEquals(
                "pm_123456789",
                paymentMethodsActivityStarterArgsCaptor.firstValue.initialPaymentMethodId
            )
        }
    }

    @Test
    fun getSelectedPaymentMethodId_whenHasUserSpecifiedPaymentMethod_returnsExpectedId() {
        createActivity {
            val paymentSession = createPaymentSession(
                it,
                DEFAULT_CONFIG,
                PaymentSessionFixtures.PAYMENT_SESSION_DATA.copy(
                    paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
                )
            )
            paymentSession.presentPaymentMethodSelection("pm_987")
            verify(paymentMethodsActivityStarter).startForResult(
                paymentMethodsActivityStarterArgsCaptor.capture()
            )
            assertEquals(
                "pm_987",
                paymentMethodsActivityStarterArgsCaptor.firstValue.initialPaymentMethodId
            )
        }
    }

    @Test
    fun init_withoutSavedState_clearsLoggingTokensAndStartsWithPaymentSession() {
        val customerSession = CustomerSession.getInstance()
        customerSession
            .addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)
        assertEquals(
            setOf(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY),
            customerSession.productUsageTokens
        )

        createActivity {
            val paymentSession = PaymentSession(it, DEFAULT_CONFIG)
            paymentSession.init(paymentSessionListener)

            // The init removes PaymentMethodsActivity, but then adds PaymentSession
            assertEquals(
                setOf(PaymentSession.TOKEN_PAYMENT_SESSION),
                customerSession.productUsageTokens
            )
        }
    }

    @Test
    fun completePayment_withLoggedActions_clearsLoggingTokensAndSetsResult() {
        val customerSession = CustomerSession.getInstance()
        customerSession
            .addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)
        assertEquals(
            setOf(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY),
            customerSession.productUsageTokens
        )

        createActivity {
            val paymentSession = PaymentSession(it, DEFAULT_CONFIG)
            // If it is given any saved state at all, the tokens are not cleared out.
            paymentSession.init(paymentSessionListener)

            assertEquals(
                setOf(PaymentSession.TOKEN_PAYMENT_SESSION),
                customerSession.productUsageTokens
            )

            reset(paymentSessionListener)

            paymentSession.onCompleted()
            assertTrue(customerSession.productUsageTokens.isEmpty())
        }
    }

    @Test
    fun init_withSavedState_setsPaymentSessionData() {
        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)

        createActivity { activity ->
            val paymentSession = PaymentSession(activity, DEFAULT_CONFIG).also {
                it.setCartTotal(300L)
                it.init(paymentSessionListener)
            }

            paymentSession.init(paymentSessionListener)

            verify(paymentSessionListener, times(2))
                .onPaymentSessionDataChanged(paymentSessionDataArgumentCaptor.capture())

            assertEquals(
                paymentSessionDataArgumentCaptor.allValues[0],
                paymentSessionDataArgumentCaptor.allValues[1]
            )
        }
    }

    @Test
    fun handlePaymentData_withInvalidRequestCode_aborts() {
        createActivity {
            val paymentSession = createPaymentSession(it)
            assertFalse(paymentSession.handlePaymentData(-1, RESULT_CANCELED, Intent()))
            verify(customerSession, never()).retrieveCurrentCustomer(any())
        }
    }

    @Test
    fun handlePaymentData_withPaymentMethodsActivityRequestCodeAndCanceledResult_doesNotRetrieveCustomer() {
        createActivity {
            val paymentSession = createPaymentSession(it)
            assertFalse(
                paymentSession.handlePaymentData(
                    PaymentMethodsActivityStarter.REQUEST_CODE,
                    RESULT_CANCELED,
                    Intent()
                )
            )
            verify(customerSession, never()).retrieveCurrentCustomer(any())
        }
    }

    @Test
    fun handlePaymentData_withPaymentFlowActivityRequestCodeAndCanceledResult_retrievesCustomer() {
        createActivity {
            val paymentSession = createPaymentSession(it)
            assertFalse(paymentSession.handlePaymentData(PaymentFlowActivityStarter.REQUEST_CODE,
                RESULT_CANCELED, Intent()))
            verify(customerSession).retrieveCurrentCustomer(any())
        }
    }

    @Test
    fun onDestroy_shouldReleaseListener() {
        createActivityScenario { activityScenario ->
            activityScenario.onActivity { activity ->
                val paymentSession = createPaymentSession(activity)
                paymentSession.init(paymentSessionListener)
                assertNotNull(paymentSession.listener)
                activityScenario.moveToState(Lifecycle.State.DESTROYED)
                assertNull(paymentSession.listener)
            }
        }
    }

    private fun createPaymentSession(
        activity: ComponentActivity,
        config: PaymentSessionConfig = DEFAULT_CONFIG,
        paymentSessionData: PaymentSessionData = PaymentSessionData(config)
    ): PaymentSession {
        return PaymentSession(
            activity,
            activity.application,
            activity,
            activity,
            activity,
            config,
            customerSession,
            paymentMethodsActivityStarter,
            paymentFlowActivityStarter,
            paymentSessionData
        )
    }

    private fun createCustomerSession(): CustomerSession {
        return CustomerSession(
            context,
            FakeStripeRepository(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            "acct_abc123",
            threadPoolExecutor = threadPoolExecutor,
            ephemeralKeyManagerFactory = EphemeralKeyManager.Factory.Default(
                keyProvider = ephemeralKeyProvider,
                shouldPrefetchEphemeralKey = true
            )
        )
    }

    private fun createActivity(callback: (ComponentActivity) -> Unit) {
        // start an arbitrary ComponentActivity
        createActivityScenario { it.onActivity(callback) }
    }

    private fun createActivityScenario(
        callback: (ActivityScenario<out ComponentActivity>) -> Unit
    ) {
        activityScenarioFactory.create<PaymentMethodsActivity>(
            PaymentMethodsActivityStarter.Args.DEFAULT
        ).use(callback)
    }

    private class FakeStripeRepository : AbsFakeStripeRepository() {
        override fun createPaymentMethod(
            paymentMethodCreateParams: PaymentMethodCreateParams,
            options: ApiRequest.Options
        ): PaymentMethod {
            return PaymentMethodFixtures.CARD_PAYMENT_METHOD
        }

        override fun setDefaultCustomerSource(
            customerId: String,
            publishableKey: String,
            productUsageTokens: Set<String>,
            sourceId: String,
            sourceType: String,
            requestOptions: ApiRequest.Options
        ): Customer {
            return SECOND_CUSTOMER
        }

        override fun retrieveCustomer(
            customerId: String,
            requestOptions: ApiRequest.Options
        ): Customer {
            return FIRST_CUSTOMER
        }
    }

    private companion object {
        private val FIRST_CUSTOMER = CustomerFixtures.CUSTOMER
        private val SECOND_CUSTOMER = CustomerFixtures.OTHER_CUSTOMER

        private val DEFAULT_CONFIG = PaymentSessionFixtures.CONFIG
    }
}
