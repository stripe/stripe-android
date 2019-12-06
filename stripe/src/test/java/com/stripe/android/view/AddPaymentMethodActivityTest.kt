package com.stripe.android.view

import android.app.Activity.RESULT_OK
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.ProgressBar
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentSession.Companion.TOKEN_PAYMENT_SESSION
import com.stripe.android.R
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.APIException
import com.stripe.android.exception.AuthenticationException
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.exception.StripeException
import com.stripe.android.model.PaymentMethod
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
    private lateinit var customerSession: CustomerSession

    @Mock
    private lateinit var alertDisplayer: AlertDisplayer

    @Mock
    private lateinit var viewModel: AddPaymentMethodViewModel

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
            createArgs(PaymentMethod.Type.Card, true)
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                val cardMultilineWidget: CardMultilineWidget =
                    activity.findViewById(R.id.card_multiline_widget)
                val widgetControlGroup =
                    CardMultilineWidgetTest.WidgetControlGroup(cardMultilineWidget)
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
                activity.createPaymentMethod(viewModel, null)
                verify(viewModel, never()).createPaymentMethod(any())
            }
        }
    }

    @Test
    fun addCardData_whenDataIsValidAndServerReturnsSuccess_finishesWithIntent() {
        activityScenarioFactory.create<AddPaymentMethodActivity>(
            BASE_CARD_ARGS
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                val progressBar: ProgressBar = activity.findViewById(R.id.progress_bar_as)
                assertEquals(View.GONE, progressBar.visibility)

                `when`(viewModel.createPaymentMethod(PaymentMethodCreateParamsFixtures.DEFAULT_CARD))
                    .thenReturn(createSuccessLiveData(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
                activity.createPaymentMethod(
                    viewModel, PaymentMethodCreateParamsFixtures.DEFAULT_CARD
                )
                verifyFinishesWithResult(activityScenario.result)
            }
        }
    }

    @Test
    fun addFpx_whenServerReturnsSuccessAndUpdatesCustomer_finishesWithIntent() {
        activityScenarioFactory.create<AddPaymentMethodActivity>(
            createArgs(
                PaymentMethod.Type.Fpx,
                initCustomerSessionTokens = true
            )
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                val progressBar: ProgressBar = activity.findViewById(R.id.progress_bar_as)

                assertEquals(View.GONE, progressBar.visibility)

                `when`(viewModel.createPaymentMethod(PaymentMethodCreateParamsFixtures.DEFAULT_FPX))
                    .thenReturn(createSuccessLiveData(PaymentMethodFixtures.FPX_PAYMENT_METHOD))
                activity.createPaymentMethod(viewModel, PaymentMethodCreateParamsFixtures.DEFAULT_FPX)

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
        activityScenarioFactory.create<AddPaymentMethodActivity>(
            createArgs(
                PaymentMethod.Type.Card,
                initCustomerSessionTokens = true
            )
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                val progressBar: ProgressBar = activity.findViewById(R.id.progress_bar_as)
                val cardMultilineWidget: CardMultilineWidget = activity.findViewById(R.id.card_multiline_widget)

                assertEquals(View.GONE, progressBar.visibility)
                assertTrue(cardMultilineWidget.isEnabled)

                `when`(viewModel.createPaymentMethod(PaymentMethodCreateParamsFixtures.DEFAULT_CARD))
                    .thenReturn(createSuccessLiveData(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
                activity.createPaymentMethod(viewModel, PaymentMethodCreateParamsFixtures.DEFAULT_CARD)
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

        activityScenarioFactory.create<AddPaymentMethodActivity>(
            BASE_CARD_ARGS
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                val progressBar: ProgressBar = activity.findViewById(R.id.progress_bar_as)

                activity.alertDisplayer = alertDisplayer

                assertEquals(View.GONE, progressBar.visibility)

                `when`(viewModel.createPaymentMethod(PaymentMethodCreateParamsFixtures.DEFAULT_CARD))
                    .thenReturn(createErrorLiveData(errorMessage))
                activity.createPaymentMethod(
                    viewModel, PaymentMethodCreateParamsFixtures.DEFAULT_CARD
                )

                assertNull(shadowOf(activity).resultIntent)
                assertFalse(activity.isFinishing)
                assertEquals(View.GONE, progressBar.visibility)
                verify(alertDisplayer).show(errorMessage)
            }
        }
    }

    @Test
    fun addCardData_whenPaymentMethodCreateWorksButAddToCustomerFails_showErrorNotFinish() {
        activityScenarioFactory.create<AddPaymentMethodActivity>(
            createArgs(
                PaymentMethod.Type.Card,
                initCustomerSessionTokens = true
            )
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                val progressBar: ProgressBar = activity.findViewById(R.id.progress_bar_as)
                val cardMultilineWidget: CardMultilineWidget = activity.findViewById(R.id.card_multiline_widget)

                activity.alertDisplayer = alertDisplayer

                assertEquals(View.GONE, progressBar.visibility)
                assertTrue(cardMultilineWidget.isEnabled)

                `when`(viewModel.createPaymentMethod(PaymentMethodCreateParamsFixtures.DEFAULT_CARD))
                    .thenReturn(createSuccessLiveData(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
                activity.createPaymentMethod(viewModel, PaymentMethodCreateParamsFixtures.DEFAULT_CARD)

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

    @Test
    fun logProductUsage_whenInitCustomerSessionTokensIsFalse_shouldNotLog() {
        activityScenarioFactory.create<AddPaymentMethodActivity>(
            createArgs(
                PaymentMethod.Type.Fpx
            )
        ).use {
            verify(customerSession, never())
                .addProductUsageTokenIfValid(any())
        }
    }

    private fun verifyFinishesWithResult(activityResult: Instrumentation.ActivityResult) {
        assertEquals(RESULT_OK, activityResult.resultCode)
        val paymentMethod = getPaymentMethodFromIntent(activityResult.resultData)
        assertEquals(EXPECTED_PAYMENT_METHOD, paymentMethod)
    }

    private fun getPaymentMethodFromIntent(intent: Intent): PaymentMethod {
        val result =
            requireNotNull(AddPaymentMethodActivityStarter.Result.fromIntent(intent))
        return result.paymentMethod
    }

    private fun createArgs(
        paymentMethodType: PaymentMethod.Type,
        initCustomerSessionTokens: Boolean = false
    ): AddPaymentMethodActivityStarter.Args {
        return AddPaymentMethodActivityStarter.Args.Builder()
            .setShouldAttachToCustomer(true)
            .setShouldRequirePostalCode(true)
            .setIsPaymentSessionActive(true)
            .setShouldInitCustomerSessionTokens(initCustomerSessionTokens)
            .setPaymentMethodType(paymentMethodType)
            .setPaymentConfiguration(PaymentConfiguration.getInstance(context))
            .build()
    }

    private companion object {
        private val BASE_CARD_ARGS =
            AddPaymentMethodActivityStarter.Args.Builder()
                .setPaymentMethodType(PaymentMethod.Type.Card)
                .build()

        private val EXPECTED_PAYMENT_METHOD =
            requireNotNull(PaymentMethodJsonParser().parse(PaymentMethodTest.PM_CARD_JSON))

        private fun createSuccessLiveData(
            paymentMethod: PaymentMethod
        ): LiveData<AddPaymentMethodViewModel.PaymentMethodResult> {
            return MutableLiveData<AddPaymentMethodViewModel.PaymentMethodResult>().apply {
                value = AddPaymentMethodViewModel.PaymentMethodResult.Success(paymentMethod)
            }
        }

        private fun createErrorLiveData(
            errorMessage: String
        ): LiveData<AddPaymentMethodViewModel.PaymentMethodResult> {
            return MutableLiveData<AddPaymentMethodViewModel.PaymentMethodResult>().apply {
                value = AddPaymentMethodViewModel.PaymentMethodResult.Error(errorMessage)
            }
        }
    }
}
