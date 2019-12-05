package com.stripe.android.view

import android.app.Activity.RESULT_OK
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.ProgressBar
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.AbsFakeStripeRepository
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.ApiRequest
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentSession.Companion.TOKEN_PAYMENT_SESSION
import com.stripe.android.R
import com.stripe.android.Stripe
import com.stripe.android.StripeNetworkUtils
import com.stripe.android.StripePaymentController
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
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import com.stripe.android.view.AddPaymentMethodActivity.Companion.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY
import java.util.Calendar
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.MainScope
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Test class for [AddPaymentMethodActivity].
 */
@RunWith(RobolectricTestRunner::class)
class AddPaymentMethodActivityTest {

    @Mock
    private lateinit var stripeRepository: StripeRepository

    @Mock
    private lateinit var customerSession: CustomerSession

    @Mock
    private lateinit var alertDisplayer: AlertDisplayer

    private val paymentMethodIdCaptor: KArgumentCaptor<String> by lazy {
        argumentCaptor<String>()
    }
    private val listenerArgumentCaptor: KArgumentCaptor<CustomerSession.PaymentMethodRetrievalListener> by lazy {
        argumentCaptor<CustomerSession.PaymentMethodRetrievalListener>()
    }

    private val context: Context by lazy {
        ApplicationProvider.getApplicationContext<Context>()
    }
    private val activityScenarioFactory: ActivityScenarioFactory by lazy {
        ActivityScenarioFactory(ApplicationProvider.getApplicationContext())
    }

    @BeforeTest
    fun setup() {
        // The input in this test class will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)

        MockitoAnnotations.initMocks(this)

        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        CustomerSession.instance = customerSession
    }

    @Test
    fun testConstructionForLocal() {
        activityScenarioFactory.create<AddPaymentMethodActivity>(
            BASE_CARD_ARGS
        ).use { activityScenario ->
            activityScenario.onActivity {
                val cardMultilineWidget: CardMultilineWidget =
                    it.findViewById(R.id.card_multiline_widget)
                val widgetControlGroup =
                    CardMultilineWidgetTest.WidgetControlGroup(cardMultilineWidget)
                assertEquals(View.GONE, widgetControlGroup.postalCodeInputLayout.visibility)
            }
        }
    }

    @Test
    fun testConstructionForCustomerSession() {
        activityScenarioFactory.create<AddPaymentMethodActivity>(
            createArgs(PaymentMethod.Type.Card)
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                val cardMultilineWidget: CardMultilineWidget = activity.findViewById(R.id.card_multiline_widget)
                val widgetControlGroup = CardMultilineWidgetTest.WidgetControlGroup(cardMultilineWidget)
                activity.initCustomerSessionTokens(customerSession)
                assertEquals(View.VISIBLE, widgetControlGroup.postalCodeInputLayout.visibility)
            }
        }
    }

    @Test
    @Throws(APIException::class, AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class)
    fun softEnterKey_whenDataIsNotValid_doesNotHideKeyboardAndDoesNotFinish() {
        activityScenarioFactory.create<AddPaymentMethodActivity>(
            BASE_CARD_ARGS
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                val progressBar: ProgressBar = activity.findViewById(R.id.progress_bar_as)
                assertEquals(View.GONE, progressBar.visibility)
                activity.createPaymentMethod(createStripe(stripeRepository), null)
                verify(stripeRepository, never()).createPaymentMethod(
                    any(), any()
                )
            }
        }
    }

    @Test
    fun addCardData_whenDataIsValidAndServerReturnsSuccess_finishesWithIntent() {
        val stripe = createStripe(createFakeRepository(PaymentMethodFixtures.CARD_PAYMENT_METHOD))

        activityScenarioFactory.create<AddPaymentMethodActivity>(
            BASE_CARD_ARGS
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                val progressBar: ProgressBar = activity.findViewById(R.id.progress_bar_as)
                assertEquals(View.GONE, progressBar.visibility)
                activity.createPaymentMethod(stripe, PaymentMethodCreateParamsFixtures.DEFAULT_CARD)
                verifyFinishesWithResult(activityScenario.result)
            }
        }
    }

    @Test
    fun addFpx_whenServerReturnsSuccessAndUpdatesCustomer_finishesWithIntent() {
        val stripe = createStripe(
            createFakeRepository(PaymentMethodFixtures.FPX_PAYMENT_METHOD)
        )

        activityScenarioFactory.create<AddPaymentMethodActivity>(
            createArgs(PaymentMethod.Type.Fpx)
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                val progressBar: ProgressBar = activity.findViewById(R.id.progress_bar_as)
                activity.initCustomerSessionTokens(customerSession)

                assertEquals(View.GONE, progressBar.visibility)
                activity.createPaymentMethod(stripe, PaymentMethodCreateParamsFixtures.DEFAULT_FPX)

                val expectedPaymentMethod = PaymentMethodFixtures.FPX_PAYMENT_METHOD

                verify(customerSession)
                    .addProductUsageTokenIfValid(TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
                verify(customerSession)
                    .addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION)
                verify(customerSession, never()).attachPaymentMethod(
                    any(), any()
                )

                assertEquals(RESULT_OK, activityScenario.result.resultCode)
                val paymentMethod = getPaymentMethodFromIntent(
                    activityScenario.result.resultData
                )
                assertEquals(expectedPaymentMethod, paymentMethod)
            }
        }
    }

    @Test
    fun addCardData_whenServerReturnsSuccessAndUpdatesCustomer_finishesWithIntent() {
        val stripe = createStripe(createFakeRepository(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
        activityScenarioFactory.create<AddPaymentMethodActivity>(
            createArgs(PaymentMethod.Type.Card)
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                val progressBar: ProgressBar = activity.findViewById(R.id.progress_bar_as)
                val cardMultilineWidget: CardMultilineWidget = activity.findViewById(R.id.card_multiline_widget)

                activity.initCustomerSessionTokens(customerSession)

                assertEquals(View.GONE, progressBar.visibility)
                assertTrue(cardMultilineWidget.isEnabled)

                activity.createPaymentMethod(stripe, PaymentMethodCreateParamsFixtures.DEFAULT_CARD)
                assertEquals(View.VISIBLE, progressBar.visibility)
                assertFalse(cardMultilineWidget.isEnabled)

                verify(customerSession)
                    .addProductUsageTokenIfValid(TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
                verify(customerSession)
                    .addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION)
                verify(customerSession).attachPaymentMethod(
                    paymentMethodIdCaptor.capture(),
                    listenerArgumentCaptor.capture()
                )

                assertEquals(EXPECTED_PAYMENT_METHOD.id, paymentMethodIdCaptor.firstValue)
                listenerArgumentCaptor.firstValue.onPaymentMethodRetrieved(EXPECTED_PAYMENT_METHOD)

                verifyFinishesWithResult(activityScenario.result)
            }
        }
    }

    @Test
    fun addCardData_whenDataIsValidButServerReturnsError_doesNotFinish() {
        val errorMessage = "Oh no! An Error!"
        val stripe = createStripe(object : AbsFakeStripeRepository() {
            override fun createPaymentMethod(
                paymentMethodCreateParams: PaymentMethodCreateParams,
                options: ApiRequest.Options
            ): PaymentMethod? {
                throw APIException(errorMessage, null, 0, null, null)
            }
        })

        activityScenarioFactory.create<AddPaymentMethodActivity>(
            BASE_CARD_ARGS
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                val progressBar: ProgressBar = activity.findViewById(R.id.progress_bar_as)

                activity.alertDisplayer = alertDisplayer

                assertEquals(View.GONE, progressBar.visibility)
                activity.createPaymentMethod(stripe, PaymentMethodCreateParamsFixtures.DEFAULT_CARD)

                assertNull(shadowOf(activity).resultIntent)
                assertFalse(activity.isFinishing)
                assertEquals(View.GONE, progressBar.visibility)
                verify(alertDisplayer).show(errorMessage)
            }
        }
    }

    @Test
    fun addCardData_whenPaymentMethodCreateWorksButAddToCustomerFails_showErrorNotFinish() {
        val stripe = createStripe(createFakeRepository(PaymentMethodFixtures.CARD_PAYMENT_METHOD))

        activityScenarioFactory.create<AddPaymentMethodActivity>(
            createArgs(PaymentMethod.Type.Card)
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                val progressBar: ProgressBar = activity.findViewById(R.id.progress_bar_as)
                val cardMultilineWidget: CardMultilineWidget = activity.findViewById(R.id.card_multiline_widget)
                activity.initCustomerSessionTokens(customerSession)

                activity.alertDisplayer = alertDisplayer

                assertEquals(View.GONE, progressBar.visibility)
                assertTrue(cardMultilineWidget.isEnabled)

                activity.createPaymentMethod(stripe, PaymentMethodCreateParamsFixtures.DEFAULT_CARD)

                assertEquals(View.VISIBLE, progressBar.visibility)
                assertFalse(cardMultilineWidget.isEnabled)

                verify(customerSession)
                    .addProductUsageTokenIfValid(TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
                verify(customerSession)
                    .addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION)
                verify(customerSession).attachPaymentMethod(
                    paymentMethodIdCaptor.capture(),
                    listenerArgumentCaptor.capture()
                )

                assertEquals(EXPECTED_PAYMENT_METHOD.id, paymentMethodIdCaptor.firstValue)

                val error: StripeException = mock()
                val errorMessage = "Oh no! An Error!"
                `when`(error.localizedMessage).thenReturn(errorMessage)
                listenerArgumentCaptor.firstValue.onError(400, errorMessage, null)

                val intent = shadowOf(activity).resultIntent
                assertNull(intent)
                assertFalse(activity.isFinishing)
                assertEquals(View.GONE, progressBar.visibility)
                verify(alertDisplayer).show(errorMessage)
            }
        }
    }

    private fun verifyFinishesWithResult(activityResult: Instrumentation.ActivityResult) {
        assertEquals(RESULT_OK, activityResult.resultCode)
        val paymentMethod = getPaymentMethodFromIntent(activityResult.resultData)
        assertEquals(EXPECTED_PAYMENT_METHOD, paymentMethod)
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
            StripePaymentController.create(context, stripeRepository),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            workScope = MainScope()
        )
    }

    private fun createArgs(
        paymentMethodType: PaymentMethod.Type
    ): AddPaymentMethodActivityStarter.Args {
        return AddPaymentMethodActivityStarter.Args.Builder()
            .setShouldAttachToCustomer(true)
            .setShouldRequirePostalCode(true)
            .setIsPaymentSessionActive(true)
            .setShouldInitCustomerSessionTokens(false)
            .setPaymentMethodType(paymentMethodType)
            .setPaymentConfiguration(PaymentConfiguration.getInstance(context))
            .build()
    }

    private companion object {
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

        private val BASE_CARD_ARGS =
            AddPaymentMethodActivityStarter.Args.Builder()
                .setPaymentMethodType(PaymentMethod.Type.Card)
                .build()

        private val EXPECTED_PAYMENT_METHOD =
            requireNotNull(PaymentMethodJsonParser().parse(PaymentMethodTest.PM_CARD_JSON))
    }
}
