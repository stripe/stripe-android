package com.stripe.android.view

import android.app.Activity.RESULT_OK
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
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
import com.stripe.android.PaymentSession
import com.stripe.android.R
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.APIException
import com.stripe.android.exception.AuthenticationException
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.exception.StripeException
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import java.util.Calendar
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlertDialog

/**
 * Test class for [AddPaymentMethodActivity].
 */
@RunWith(RobolectricTestRunner::class)
class AddPaymentMethodActivityTest {

    private val customerSession: CustomerSession = mock()
    private val viewModel: AddPaymentMethodViewModel = mock()

    private val paymentMethodIdCaptor: KArgumentCaptor<String> = argumentCaptor()
    private val listenerArgumentCaptor: KArgumentCaptor<CustomerSession.PaymentMethodRetrievalListener> = argumentCaptor()
    private val productUsageArgumentCaptor: KArgumentCaptor<Set<String>> = argumentCaptor()

    private val context: Context = ApplicationProvider.getApplicationContext<Context>()
    private val activityScenarioFactory: ActivityScenarioFactory by lazy {
        ActivityScenarioFactory(ApplicationProvider.getApplicationContext())
    }

    @BeforeTest
    fun setup() {
        // The input in this test class will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)
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
                val progressBar: ProgressBar = activity.findViewById(R.id.progress_bar)
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
                val progressBar: ProgressBar = activity.findViewById(R.id.progress_bar)
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
                val progressBar: ProgressBar = activity.findViewById(R.id.progress_bar)

                assertEquals(View.GONE, progressBar.visibility)

                `when`(viewModel.createPaymentMethod(PaymentMethodCreateParamsFixtures.DEFAULT_FPX))
                    .thenReturn(createSuccessLiveData(PaymentMethodFixtures.FPX_PAYMENT_METHOD))
                activity.createPaymentMethod(viewModel, PaymentMethodCreateParamsFixtures.DEFAULT_FPX)

                val expectedPaymentMethod = PaymentMethodFixtures.FPX_PAYMENT_METHOD

                verify(customerSession, never()).attachPaymentMethod(
                    any(),
                    any(),
                    any()
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
    fun addCardData_withFullBillingFieldsRequirement_shouldShowBillingAddressWidget() {
        activityScenarioFactory.create<AddPaymentMethodActivity>(
            createArgs(
                PaymentMethod.Type.Card,
                initCustomerSessionTokens = true,
                billingAddressFields = BillingAddressFields.Full
            )
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                assertEquals(
                    View.VISIBLE,
                    activity.findViewById<View>(R.id.billing_address_widget).visibility
                )

                val cardMultilineWidget: CardMultilineWidget =
                    activity.findViewById(R.id.card_multiline_widget)
                assertEquals(
                    View.GONE,
                    cardMultilineWidget.findViewById<View>(R.id.tl_postal_code).visibility
                )
            }
        }
    }

    @Test
    fun addCardData_withPostalCodeBillingFieldsRequirement_shouldHideBillingAddressWidget() {
        activityScenarioFactory.create<AddPaymentMethodActivity>(
            createArgs(
                PaymentMethod.Type.Card,
                initCustomerSessionTokens = true,
                billingAddressFields = BillingAddressFields.PostalCode
            )
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                assertEquals(
                    View.GONE,
                    activity.findViewById<View>(R.id.billing_address_widget).visibility
                )

                val cardMultilineWidget: CardMultilineWidget =
                    activity.findViewById(R.id.card_multiline_widget)
                assertEquals(
                    View.VISIBLE,
                    cardMultilineWidget.findViewById<View>(R.id.tl_postal_code).visibility
                )
            }
        }
    }

    @Test
    fun addCardData_withNoBillingFieldsRequirement_shouldHideBillingAddressWidgetAndPostalCode() {
        activityScenarioFactory.create<AddPaymentMethodActivity>(
            createArgs(
                PaymentMethod.Type.Card,
                initCustomerSessionTokens = true,
                billingAddressFields = BillingAddressFields.None
            )
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                assertEquals(
                    View.GONE,
                    activity.findViewById<View>(R.id.billing_address_widget).visibility
                )

                val cardMultilineWidget: CardMultilineWidget =
                    activity.findViewById(R.id.card_multiline_widget)
                assertEquals(
                    View.GONE,
                    cardMultilineWidget.findViewById<View>(R.id.tl_postal_code).visibility
                )
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
                val progressBar: ProgressBar = activity.findViewById(R.id.progress_bar)
                val cardMultilineWidget: CardMultilineWidget = activity.findViewById(R.id.card_multiline_widget)

                assertEquals(View.GONE, progressBar.visibility)
                assertTrue(cardMultilineWidget.isEnabled)

                `when`(viewModel.createPaymentMethod(PaymentMethodCreateParamsFixtures.DEFAULT_CARD))
                    .thenReturn(createSuccessLiveData(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
                activity.createPaymentMethod(viewModel, PaymentMethodCreateParamsFixtures.DEFAULT_CARD)
                assertEquals(View.VISIBLE, progressBar.visibility)
                assertFalse(cardMultilineWidget.isEnabled)

                verify(customerSession).attachPaymentMethod(
                    paymentMethodIdCaptor.capture(),
                    productUsageArgumentCaptor.capture(),
                    listenerArgumentCaptor.capture()
                )

                assertEquals(
                    setOf(
                        AddPaymentMethodActivity.PRODUCT_TOKEN,
                        PaymentSession.PRODUCT_TOKEN
                    ),
                    productUsageArgumentCaptor.firstValue
                )

                assertEquals(EXPECTED_PAYMENT_METHOD.id, paymentMethodIdCaptor.firstValue)
                listenerArgumentCaptor.firstValue.onPaymentMethodRetrieved(EXPECTED_PAYMENT_METHOD)

                verifyFinishesWithResult(activityScenario.result)
            }
        }
    }

    @Test
    fun addCardData_whenDataIsValidButServerReturnsError_doesNotFinish() {
        activityScenarioFactory.create<AddPaymentMethodActivity>(
            BASE_CARD_ARGS
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                val progressBar: ProgressBar = activity.findViewById(R.id.progress_bar)
                assertEquals(View.GONE, progressBar.visibility)

                `when`(viewModel.createPaymentMethod(PaymentMethodCreateParamsFixtures.DEFAULT_CARD))
                    .thenReturn(createErrorLiveData())
                activity.createPaymentMethod(
                    viewModel, PaymentMethodCreateParamsFixtures.DEFAULT_CARD
                )

                assertNull(shadowOf(activity).resultIntent)
                assertFalse(activity.isFinishing)
                assertEquals(View.GONE, progressBar.visibility)

                verifyDialogWithMessage(ERROR_MESSAGE)
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
                val progressBar: ProgressBar = activity.findViewById(R.id.progress_bar)
                val cardMultilineWidget: CardMultilineWidget = activity.findViewById(R.id.card_multiline_widget)

                assertEquals(View.GONE, progressBar.visibility)
                assertTrue(cardMultilineWidget.isEnabled)

                `when`(viewModel.createPaymentMethod(PaymentMethodCreateParamsFixtures.DEFAULT_CARD))
                    .thenReturn(createSuccessLiveData(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
                activity.createPaymentMethod(viewModel, PaymentMethodCreateParamsFixtures.DEFAULT_CARD)

                assertEquals(View.VISIBLE, progressBar.visibility)
                assertFalse(cardMultilineWidget.isEnabled)

                verify(customerSession).attachPaymentMethod(
                    paymentMethodIdCaptor.capture(),
                    productUsageArgumentCaptor.capture(),
                    listenerArgumentCaptor.capture()
                )

                assertEquals(
                    setOf(
                        AddPaymentMethodActivity.PRODUCT_TOKEN,
                        PaymentSession.PRODUCT_TOKEN
                    ),
                    productUsageArgumentCaptor.firstValue
                )

                assertEquals(EXPECTED_PAYMENT_METHOD.id, paymentMethodIdCaptor.firstValue)

                val error: StripeException = mock()
                `when`(error.localizedMessage).thenReturn(ERROR_MESSAGE)
                listenerArgumentCaptor.firstValue.onError(400, ERROR_MESSAGE, null)

                val intent = shadowOf(activity).resultIntent
                assertNull(intent)
                assertFalse(activity.isFinishing)
                assertEquals(View.GONE, progressBar.visibility)

                verifyDialogWithMessage(ERROR_MESSAGE)
            }
        }
    }

    private fun verifyDialogWithMessage(expectedMessage: String) {
        val dialog = ShadowAlertDialog.getShownDialogs().first()
        val actualMessage = dialog.findViewById<TextView>(android.R.id.message).text
        assertEquals(
            expectedMessage,
            actualMessage
        )
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
        initCustomerSessionTokens: Boolean = false,
        billingAddressFields: BillingAddressFields = BillingAddressFields.PostalCode
    ): AddPaymentMethodActivityStarter.Args {
        return AddPaymentMethodActivityStarter.Args.Builder()
            .setShouldAttachToCustomer(true)
            .setIsPaymentSessionActive(true)
            .setShouldInitCustomerSessionTokens(initCustomerSessionTokens)
            .setPaymentMethodType(paymentMethodType)
            .setPaymentConfiguration(PaymentConfiguration.getInstance(context))
            .setBillingAddressFields(billingAddressFields)
            .build()
    }

    private companion object {
        private const val ERROR_MESSAGE = "Oh no! An Error!"

        private val BASE_CARD_ARGS =
            AddPaymentMethodActivityStarter.Args.Builder()
                .setPaymentMethodType(PaymentMethod.Type.Card)
                .build()

        private val EXPECTED_PAYMENT_METHOD = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        private fun createSuccessLiveData(
            paymentMethod: PaymentMethod
        ): LiveData<AddPaymentMethodViewModel.PaymentMethodResult> {
            return MutableLiveData<AddPaymentMethodViewModel.PaymentMethodResult>().apply {
                value = AddPaymentMethodViewModel.PaymentMethodResult.Success(paymentMethod)
            }
        }

        private fun createErrorLiveData(): LiveData<AddPaymentMethodViewModel.PaymentMethodResult> {
            return MutableLiveData<AddPaymentMethodViewModel.PaymentMethodResult>().apply {
                value = AddPaymentMethodViewModel.PaymentMethodResult.Error(ERROR_MESSAGE)
            }
        }
    }
}
