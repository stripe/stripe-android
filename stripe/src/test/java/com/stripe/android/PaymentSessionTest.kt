package com.stripe.android

import android.app.Activity
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.model.Customer
import com.stripe.android.model.CustomerFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodTest
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import com.stripe.android.testharness.TestEphemeralKeyProvider
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [PaymentSession]
 */
@RunWith(RobolectricTestRunner::class)
class PaymentSessionTest {

    private val ephemeralKeyProvider = TestEphemeralKeyProvider()

    @Mock
    private lateinit var activity: Activity
    @Mock
    private lateinit var threadPoolExecutor: ThreadPoolExecutor
    @Mock
    private lateinit var paymentSessionListener: PaymentSession.PaymentSessionListener
    @Mock
    private lateinit var customerSession: CustomerSession
    @Mock
    private lateinit var paymentMethodsActivityStarter:
        ActivityStarter<PaymentMethodsActivity, PaymentMethodsActivityStarter.Args>
    @Mock
    private lateinit var paymentFlowActivityStarter:
        ActivityStarter<PaymentFlowActivity, PaymentFlowActivityStarter.Args>
    @Mock
    private lateinit var paymentSessionPrefs: PaymentSessionPrefs

    private lateinit var paymentSessionDataArgumentCaptor: KArgumentCaptor<PaymentSessionData>
    private lateinit var intentArgumentCaptor: KArgumentCaptor<Intent>

    @BeforeTest
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val appContext: Context = ApplicationProvider.getApplicationContext()
        PaymentConfiguration.init(appContext, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        paymentSessionDataArgumentCaptor = argumentCaptor()
        intentArgumentCaptor = argumentCaptor()

        `when`(activity.applicationContext).thenReturn(appContext)

        doAnswer { invocation ->
            invocation.getArgument<Runnable>(0).run()
            null
        }.`when`<ThreadPoolExecutor>(threadPoolExecutor).execute(any())
    }

    @Test
    fun init_addsPaymentSessionToken_andFetchesCustomer() {
        val customerSession = createCustomerSession()
        CustomerSession.instance = customerSession

        val paymentSession = PaymentSession(activity)
        paymentSession.init(paymentSessionListener, PaymentSessionConfig.Builder().build())

        assertTrue(customerSession.productUsageTokens
            .contains(PaymentSession.TOKEN_PAYMENT_SESSION))

        verify(paymentSessionListener).onCommunicatingStateChanged(eq(true))
    }

    @Test
    fun init_whenEphemeralKeyProviderContinues_fetchesCustomerAndNotifiesListener() {
        ephemeralKeyProvider
            .setNextRawEphemeralKey(CustomerFixtures.EPHEMERAL_KEY_FIRST.toString())
        CustomerSession.instance = createCustomerSession()

        val paymentSession = PaymentSession(activity)
        paymentSession.init(paymentSessionListener, PaymentSessionConfig.Builder().build())
        verify(paymentSessionListener)
            .onCommunicatingStateChanged(eq(true))
        verify(paymentSessionListener)
            .onPaymentSessionDataChanged(any())
        verify(paymentSessionListener)
            .onCommunicatingStateChanged(eq(false))
    }

    @Test
    fun handlePaymentData_whenPaymentMethodRequest_notifiesListenerAndFetchesCustomer() {
        CustomerSession.instance = createCustomerSession()

        val paymentSession = PaymentSession(activity)
        paymentSession.init(paymentSessionListener, PaymentSessionConfig.Builder().build())

        // We have already tested the functionality up to here.
        reset(paymentSessionListener)

        val result = PaymentMethodsActivityStarter.Result(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        val handled = paymentSession.handlePaymentData(
            PaymentMethodsActivityStarter.REQUEST_CODE, RESULT_OK,
            Intent().putExtras(result.toBundle()))
        assertTrue(handled)

        verify(paymentSessionListener)
            .onPaymentSessionDataChanged(paymentSessionDataArgumentCaptor.capture())
        val data = paymentSessionDataArgumentCaptor.firstValue
        assertEquals(PaymentMethodFixtures.CARD_PAYMENT_METHOD, data.paymentMethod)
    }

    @Test
    fun selectPaymentMethod_launchesPaymentMethodsActivityWithLog() {
        CustomerSession.instance = createCustomerSession()

        val paymentSession = PaymentSession(activity)
        paymentSession.init(paymentSessionListener, PaymentSessionConfig.Builder().build())

        paymentSession.presentPaymentMethodSelection()

        verify(activity).startActivityForResult(intentArgumentCaptor.capture(),
            eq(PaymentMethodsActivityStarter.REQUEST_CODE))

        val intent = intentArgumentCaptor.firstValue
        val component = intent.component
        assertEquals(PaymentMethodsActivity::class.java.name, component?.className)

        val args = PaymentMethodsActivityStarter.Args.create(intent)
        assertFalse(args.shouldRequirePostalCode)
    }

    @Test
    fun presentPaymentMethodSelection_withShouldRequirePostalCode_shouldPassInIntent() {
        CustomerSession.instance = createCustomerSession()

        val paymentSession = PaymentSession(activity)
        paymentSession.init(paymentSessionListener, PaymentSessionConfig.Builder()
            .setBillingAddressFields(BillingAddressFields.PostalCode)
            .build())
        paymentSession.presentPaymentMethodSelection()

        verify(activity).startActivityForResult(intentArgumentCaptor.capture(),
            eq(PaymentMethodsActivityStarter.REQUEST_CODE))
        assertEquals(
            BillingAddressFields.PostalCode,
            PaymentMethodsActivityStarter.Args.create(intentArgumentCaptor.firstValue)
                .billingAddressFields
        )
    }

    @Test
    fun getSelectedPaymentMethodId_whenPrefsNotSet_returnsNull() {
        `when`<Customer>(customerSession.cachedCustomer).thenReturn(FIRST_CUSTOMER)
        CustomerSession.instance = customerSession
        assertNull(createPaymentSession().getSelectedPaymentMethodId(null))
    }

    @Test
    fun getSelectedPaymentMethodId_whenHasPaymentSessionData_returnsExpectedId() {
        val paymentSession = createPaymentSession(
            PaymentSessionFixtures.PAYMENT_SESSION_DATA.copy(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
            )
        )
        assertEquals(
            "pm_123456789",
            paymentSession.getSelectedPaymentMethodId(null)
        )
    }

    @Test
    fun getSelectedPaymentMethodId_whenHasPrefsSet_returnsExpectedId() {
        val customerId = requireNotNull(FIRST_CUSTOMER.id)
        `when`<String>(paymentSessionPrefs.getSelectedPaymentMethodId(customerId))
            .thenReturn("pm_12345")

        `when`<Customer>(customerSession.cachedCustomer).thenReturn(FIRST_CUSTOMER)
        CustomerSession.instance = customerSession

        assertEquals("pm_12345",
            createPaymentSession().getSelectedPaymentMethodId(null))
    }

    @Test
    fun getSelectedPaymentMethodId_whenHasUserSpecifiedPaymentMethod_returnsExpectedId() {
        val paymentSession = createPaymentSession(
            PaymentSessionFixtures.PAYMENT_SESSION_DATA.copy(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
            )
        )
        assertEquals("pm_987",
            paymentSession.getSelectedPaymentMethodId("pm_987"))
    }

    @Test
    fun init_withoutSavedState_clearsLoggingTokensAndStartsWithPaymentSession() {
        val customerSession = createCustomerSession()
        CustomerSession.instance = customerSession
        customerSession
            .addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)
        assertEquals(1, customerSession.productUsageTokens.size)

        val paymentSession = PaymentSession(activity)
        paymentSession.init(paymentSessionListener, PaymentSessionConfig.Builder().build())

        // The init removes PaymentMethodsActivity, but then adds PaymentSession
        val loggingTokens = customerSession.productUsageTokens
        assertEquals(1, loggingTokens.size)
        assertFalse(loggingTokens.contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY))
        assertTrue(loggingTokens.contains(PaymentSession.TOKEN_PAYMENT_SESSION))
    }

    @Test
    fun init_withSavedStateBundle_doesNotClearLoggingTokens() {
        val customerSession = createCustomerSession()
        CustomerSession.instance = customerSession
        customerSession
            .addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)
        assertEquals(1, customerSession.productUsageTokens.size)

        val paymentSession = PaymentSession(activity)
        // If it is given any saved state at all, the tokens are not cleared out.
        paymentSession.init(paymentSessionListener,
            PaymentSessionConfig.Builder().build(), Bundle())

        val loggingTokens = customerSession.productUsageTokens
        assertEquals(2, loggingTokens.size)
        assertTrue(loggingTokens.contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY))
        assertTrue(loggingTokens.contains(PaymentSession.TOKEN_PAYMENT_SESSION))
    }

    @Test
    fun completePayment_withLoggedActions_clearsLoggingTokensAndSetsResult() {
        val customerSession = createCustomerSession()
        CustomerSession.instance = customerSession
        customerSession
            .addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)
        assertEquals(1, customerSession.productUsageTokens.size)

        val paymentSession = PaymentSession(activity)
        // If it is given any saved state at all, the tokens are not cleared out.
        paymentSession.init(paymentSessionListener,
            PaymentSessionConfig.Builder().build(), Bundle())

        val loggingTokens = customerSession.productUsageTokens
        assertEquals(2, loggingTokens.size)

        reset(paymentSessionListener)

        paymentSession.onCompleted()
        assertTrue(customerSession.productUsageTokens.isEmpty())
    }

    @Test
    fun init_withSavedState_setsPaymentSessionData() {
        ephemeralKeyProvider
            .setNextRawEphemeralKey(CustomerFixtures.EPHEMERAL_KEY_FIRST.toString())
        CustomerSession.instance = createCustomerSession()

        val paymentSession = PaymentSession(activity)
        paymentSession.init(paymentSessionListener, PaymentSessionConfig.Builder().build())
        paymentSession.setCartTotal(300L)

        verify(paymentSessionListener)
            .onPaymentSessionDataChanged(paymentSessionDataArgumentCaptor.capture())
        val bundle = Bundle()
        paymentSession.savePaymentSessionInstanceState(bundle)
        val firstPaymentSessionData = paymentSessionDataArgumentCaptor.firstValue

        val secondListener =
            mock(PaymentSession.PaymentSessionListener::class.java)

        paymentSession.init(secondListener, PaymentSessionConfig.Builder().build(), bundle)
        verify(secondListener)
            .onPaymentSessionDataChanged(paymentSessionDataArgumentCaptor.capture())

        val secondPaymentSessionData = paymentSessionDataArgumentCaptor.firstValue
        assertEquals(firstPaymentSessionData.cartTotal,
            secondPaymentSessionData.cartTotal)
        assertEquals(firstPaymentSessionData.paymentMethod,
            secondPaymentSessionData.paymentMethod)
    }

    @Test
    fun handlePaymentData_withInvalidRequestCode_aborts() {
        val paymentSession = createPaymentSession()
        assertFalse(paymentSession.handlePaymentData(-1, RESULT_CANCELED, Intent()))
        verify(customerSession, never()).retrieveCurrentCustomer(any())
    }

    @Test
    fun handlePaymentData_withPaymentMethodsActivityRequestCodeAndCanceledResult_doesNotRetrieveCustomer() {
        val paymentSession = createPaymentSession()
        assertFalse(paymentSession.handlePaymentData(PaymentMethodsActivityStarter.REQUEST_CODE,
            RESULT_CANCELED, Intent()))
        verify(customerSession, never()).retrieveCurrentCustomer(any())
    }

    @Test
    fun handlePaymentData_withPaymentFlowActivityRequestCodeAndCanceledResult_retrievesCustomer() {
        val paymentSession = createPaymentSession()
        assertFalse(paymentSession.handlePaymentData(PaymentFlowActivityStarter.REQUEST_CODE,
            RESULT_CANCELED, Intent()))
        verify(customerSession).retrieveCurrentCustomer(any())
    }

    private fun createPaymentSession(
        paymentSessionData: PaymentSessionData = PaymentSessionFixtures.PAYMENT_SESSION_DATA
    ): PaymentSession {
        return PaymentSession(
            ApplicationProvider.getApplicationContext<Context>(),
            customerSession,
            paymentMethodsActivityStarter,
            paymentFlowActivityStarter,
            paymentSessionPrefs,
            paymentSessionData
        )
    }

    private fun createCustomerSession(): CustomerSession {
        return CustomerSession(
            ApplicationProvider.getApplicationContext<Context>(),
            ephemeralKeyProvider,
            null,
            threadPoolExecutor,
            FakeStripeRepository(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            "acct_abc123",
            true
        )
    }

    private class FakeStripeRepository : AbsFakeStripeRepository() {
        override fun createPaymentMethod(
            paymentMethodCreateParams: PaymentMethodCreateParams,
            options: ApiRequest.Options
        ): PaymentMethod {
            return PaymentMethodJsonParser().parse(PaymentMethodTest.PM_CARD_JSON)
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
    }
}
