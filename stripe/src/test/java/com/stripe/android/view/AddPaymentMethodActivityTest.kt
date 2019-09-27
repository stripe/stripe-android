package com.stripe.android.view

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.stripe.android.AbsFakeStripeRepository
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.ApiRequest
import com.stripe.android.CustomerSession
import com.stripe.android.CustomerSession.ACTION_API_EXCEPTION
import com.stripe.android.CustomerSession.EXTRA_EXCEPTION
import com.stripe.android.CustomerSessionTestHelper
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentController
import com.stripe.android.PaymentSession.TOKEN_PAYMENT_SESSION
import com.stripe.android.R
import com.stripe.android.Stripe
import com.stripe.android.StripeNetworkUtils
import com.stripe.android.StripeRepository
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.APIException
import com.stripe.android.exception.AuthenticationException
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.exception.StripeException
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodTest
import com.stripe.android.view.AddPaymentMethodActivity.Companion.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY
import java.util.Calendar
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowActivity

/**
 * Test class for [AddPaymentMethodActivity].
 */
@RunWith(RobolectricTestRunner::class)
class AddPaymentMethodActivityTest :
    BaseViewTest<AddPaymentMethodActivity>(AddPaymentMethodActivity::class.java) {

    private lateinit var context: Context
    private lateinit var stripe: Stripe

    private lateinit var cardMultilineWidget: CardMultilineWidget
    private lateinit var widgetControlGroup: CardMultilineWidgetTest.WidgetControlGroup
    private lateinit var progressBar: ProgressBar
    private lateinit var activity: AddPaymentMethodActivity
    private lateinit var shadowActivity: ShadowActivity

    private lateinit var paymentMethodIdCaptor: KArgumentCaptor<String>
    private lateinit var listenerArgumentCaptor:
        KArgumentCaptor<CustomerSession.PaymentMethodRetrievalListener>

    @Mock
    private lateinit var mockStripeRepository: StripeRepository

    @Mock
    private lateinit var customerSession: CustomerSession

    @BeforeTest
    fun setup() {
        // The input in this test class will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)

        MockitoAnnotations.initMocks(this)

        paymentMethodIdCaptor = argumentCaptor()
        listenerArgumentCaptor = argumentCaptor()

        context = ApplicationProvider.getApplicationContext()

        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        CustomerSessionTestHelper.setInstance(customerSession)
    }

    @AfterTest
    override fun tearDown() {
        super.tearDown()
    }

    private fun setUpForLocalTest() {
        activity = createActivity(AddPaymentMethodActivityStarter.Args.Builder()
            .setPaymentMethodType(PaymentMethod.Type.Card)
            .build())
        cardMultilineWidget = activity.findViewById(R.id.add_source_card_entry_widget)
        progressBar = activity.findViewById(R.id.progress_bar_as)
        widgetControlGroup = CardMultilineWidgetTest.WidgetControlGroup(cardMultilineWidget)

        shadowActivity = shadowOf(activity)
    }

    private fun setUpForProxySessionTest(paymentMethodType: PaymentMethod.Type) {
        activity = createActivity(AddPaymentMethodActivityStarter.Args.Builder()
            .setShouldAttachToCustomer(true)
            .setShouldRequirePostalCode(true)
            .setIsPaymentSessionActive(true)
            .setShouldInitCustomerSessionTokens(false)
            .setPaymentMethodType(paymentMethodType)
            .setPaymentConfiguration(PaymentConfiguration.getInstance(context))
            .build())

        progressBar = activity.findViewById(R.id.progress_bar_as)

        if (PaymentMethod.Type.Card == paymentMethodType) {
            cardMultilineWidget = activity.findViewById(R.id.add_source_card_entry_widget)
            widgetControlGroup = CardMultilineWidgetTest.WidgetControlGroup(cardMultilineWidget)
        }

        shadowActivity = shadowOf(activity)
        activity.initCustomerSessionTokens()
    }

    @Test
    fun testConstructionForLocal() {
        stripe = createStripe(createFakeRepository(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
        setUpForLocalTest()
        assertEquals(View.GONE, widgetControlGroup.postalCodeInputLayout.visibility)
    }

    @Test
    fun testConstructionForCustomerSession() {
        stripe = createStripe(createFakeRepository(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
        setUpForProxySessionTest(PaymentMethod.Type.Card)
        assertEquals(View.VISIBLE, widgetControlGroup.postalCodeInputLayout.visibility)
    }

    @Test
    @Throws(APIException::class, AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class)
    fun softEnterKey_whenDataIsNotValid_doesNotHideKeyboardAndDoesNotFinish() {
        stripe = createStripe(mockStripeRepository)
        setUpForLocalTest()
        assertEquals(View.GONE, progressBar.visibility)
        activity.createPaymentMethod(stripe, null)
        verify<StripeRepository>(mockStripeRepository, never()).createPaymentMethod(
            any(), any()
        )
    }

    @Test
    fun addCardData_whenDataIsValidAndServerReturnsSuccess_finishesWithIntent() {
        stripe = createStripe(createFakeRepository(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
        setUpForLocalTest()
        assertEquals(View.GONE, progressBar.visibility)
        activity.createPaymentMethod(stripe, PaymentMethodCreateParamsFixtures.DEFAULT_CARD)
        verifyFinishesWithIntent()
    }

    @Test
    fun addFpx_whenServerReturnsSuccessAndUpdatesCustomer_finishesWithIntent() {
        stripe = createStripe(createFakeRepository(PaymentMethodFixtures.FPX_PAYMENT_METHOD))
        setUpForProxySessionTest(PaymentMethod.Type.Fpx)

        assertEquals(View.GONE, progressBar.visibility)
        activity.createPaymentMethod(stripe, PaymentMethodCreateParamsFixtures.DEFAULT_FPX)

        val expectedPaymentMethod = PaymentMethodFixtures.FPX_PAYMENT_METHOD

        verify<CustomerSession>(customerSession)
            .addProductUsageTokenIfValid(TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        verify<CustomerSession>(customerSession)
            .addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION)
        verify<CustomerSession>(customerSession, never()).attachPaymentMethod(
            any(), any()
        )

        assertEquals(RESULT_OK, shadowActivity.resultCode)
        val intent = shadowActivity.resultIntent

        assertTrue(activity.isFinishing)

        val paymentMethod = getPaymentMethodFromIntent(intent)
        assertEquals(expectedPaymentMethod, paymentMethod)
    }

    @Test
    fun addCardData_whenServerReturnsSuccessAndUpdatesCustomer_finishesWithIntent() {
        stripe = createStripe(createFakeRepository(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
        setUpForProxySessionTest(PaymentMethod.Type.Card)

        assertEquals(View.GONE, progressBar.visibility)
        assertTrue(cardMultilineWidget.isEnabled)

        activity.createPaymentMethod(stripe, PaymentMethodCreateParamsFixtures.DEFAULT_CARD)
        assertEquals(View.VISIBLE, progressBar.visibility)
        assertFalse(cardMultilineWidget.isEnabled)

        val expectedPaymentMethod =
            PaymentMethod.fromString(PaymentMethodTest.PM_CARD_JSON)!!

        verify<CustomerSession>(customerSession)
            .addProductUsageTokenIfValid(TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        verify<CustomerSession>(customerSession)
            .addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION)
        verify<CustomerSession>(customerSession).attachPaymentMethod(
            paymentMethodIdCaptor.capture(),
            listenerArgumentCaptor.capture()
        )

        assertEquals(expectedPaymentMethod.id, paymentMethodIdCaptor.firstValue)
        listenerArgumentCaptor.firstValue.onPaymentMethodRetrieved(expectedPaymentMethod)

        assertEquals(RESULT_OK, shadowActivity.resultCode)
        val intent = shadowActivity.resultIntent

        assertTrue(activity.isFinishing)

        val paymentMethod = getPaymentMethodFromIntent(intent)
        assertEquals(expectedPaymentMethod, paymentMethod)
    }

    @Test
    fun addCardData_whenDataIsValidButServerReturnsError_doesNotFinish() {
        val errorMessage = "Oh no! An Error!"
        stripe = createStripe(object : AbsFakeStripeRepository() {
            override fun createPaymentMethod(
                paymentMethodCreateParams: PaymentMethodCreateParams,
                options: ApiRequest.Options
            ): PaymentMethod? {
                throw APIException(errorMessage, null, 0, null, null)
            }
        })
        setUpForLocalTest()

        val alertMessageListener: StripeActivity.AlertMessageListener = mock()
        activity.setAlertMessageListener(alertMessageListener)

        assertEquals(View.GONE, progressBar.visibility)
        activity.createPaymentMethod(stripe, PaymentMethodCreateParamsFixtures.DEFAULT_CARD)

        assertNull(shadowActivity.resultIntent)
        assertFalse(activity.isFinishing)
        assertEquals(View.GONE, progressBar.visibility)
        verify(alertMessageListener).onAlertMessageDisplayed(errorMessage)
    }

    @Test
    fun addCardData_whenPaymentMethodCreateWorksButAddToCustomerFails_showErrorNotFinish() {
        stripe = createStripe(createFakeRepository(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
        setUpForProxySessionTest(PaymentMethod.Type.Card)
        val alertMessageListener: StripeActivity.AlertMessageListener = mock()
        activity.setAlertMessageListener(alertMessageListener)

        assertEquals(View.GONE, progressBar.visibility)
        assertTrue(cardMultilineWidget.isEnabled)

        activity.createPaymentMethod(stripe, PaymentMethodCreateParamsFixtures.DEFAULT_CARD)

        assertEquals(View.VISIBLE, progressBar.visibility)
        assertFalse(cardMultilineWidget.isEnabled)

        val expectedPaymentMethod =
            PaymentMethod.fromString(PaymentMethodTest.PM_CARD_JSON)!!

        verify<CustomerSession>(customerSession)
            .addProductUsageTokenIfValid(TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        verify<CustomerSession>(customerSession)
            .addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION)
        verify<CustomerSession>(customerSession).attachPaymentMethod(
            paymentMethodIdCaptor.capture(),
            listenerArgumentCaptor.capture()
        )

        assertEquals(expectedPaymentMethod.id, paymentMethodIdCaptor.firstValue)

        val error: StripeException = mock()
        val errorMessage = "Oh no! An Error!"
        `when`(error.localizedMessage).thenReturn(errorMessage)
        listenerArgumentCaptor.firstValue.onError(400, errorMessage, null)

        // We're mocking the CustomerSession, so we have to replicate its broadcast behavior.
        val bundle = Bundle()
        bundle.putSerializable(EXTRA_EXCEPTION, error)
        LocalBroadcastManager.getInstance(activity)
            .sendBroadcast(Intent(ACTION_API_EXCEPTION)
                .putExtras(bundle))

        val intent = shadowActivity.resultIntent
        assertNull(intent)
        assertFalse(activity.isFinishing)
        assertEquals(View.GONE, progressBar.visibility)
        verify<StripeActivity.AlertMessageListener>(alertMessageListener)
            .onAlertMessageDisplayed(errorMessage)
    }

    private fun verifyFinishesWithIntent() {
        val expectedPaymentMethod =
            PaymentMethod.fromString(PaymentMethodTest.PM_CARD_JSON)!!
        assertEquals(RESULT_OK, shadowActivity.resultCode)
        val intent = shadowActivity.resultIntent

        assertTrue(activity.isFinishing)

        val paymentMethod = getPaymentMethodFromIntent(intent)
        assertEquals(expectedPaymentMethod, paymentMethod)
    }

    private fun getPaymentMethodFromIntent(intent: Intent): PaymentMethod {
        val result =
            AddPaymentMethodActivityStarter.Result.fromIntent(intent)
        return result!!.paymentMethod
    }

    private fun createStripe(stripeRepository: StripeRepository): Stripe {
        return Stripe(
            stripeRepository,
            StripeNetworkUtils(context),
            PaymentController.create(context, stripeRepository),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            null
        )
    }

    companion object {
        private fun createFakeRepository(paymentMethod: PaymentMethod): StripeRepository {
            return object : AbsFakeStripeRepository() {
                override fun createPaymentMethod(
                    paymentMethodCreateParams: PaymentMethodCreateParams,
                    options: ApiRequest.Options
                ): PaymentMethod? {
                    return paymentMethod
                }
            }
        }
    }
}
