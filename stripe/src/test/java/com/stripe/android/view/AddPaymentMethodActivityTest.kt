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
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentSession
import com.stripe.android.R
import com.stripe.android.exception.StripeException
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.runner.RunWith
import org.mockito.Mockito.never
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlertDialog
import java.util.Calendar
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test class for [AddPaymentMethodActivity].
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class AddPaymentMethodActivityTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val customerSession: CustomerSession = mock()
    private val viewModel: AddPaymentMethodViewModel = mock()

    private val paymentMethodIdCaptor: KArgumentCaptor<String> = argumentCaptor()
    private val listenerArgumentCaptor: KArgumentCaptor<CustomerSession.PaymentMethodRetrievalListener> = argumentCaptor()
    private val productUsageArgumentCaptor: KArgumentCaptor<Set<String>> = argumentCaptor()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val activityScenarioFactory = ActivityScenarioFactory(context)

    @BeforeTest
    fun setup() {
        // The input in this test class will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050)
        PaymentConfiguration.init(
            context,
            ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
            "acct_12345"
        )
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
                    CardMultilineWidgetTest.WidgetControlGroup(cardMultilineWidget, testDispatcher)
                assertEquals(View.VISIBLE, widgetControlGroup.postalCodeInputLayout.visibility)
            }
        }
    }

    @Test
    fun testConstructionForCustomerSession() {
        activityScenarioFactory.create<AddPaymentMethodActivity>(
            createArgs(PaymentMethod.Type.Card)
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                val cardMultilineWidget: CardMultilineWidget =
                    activity.findViewById(R.id.card_multiline_widget)
                val widgetControlGroup =
                    CardMultilineWidgetTest.WidgetControlGroup(cardMultilineWidget, testDispatcher)
                assertEquals(View.VISIBLE, widgetControlGroup.postalCodeInputLayout.visibility)
            }
        }
    }

    @Test
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

                whenever(viewModel.createPaymentMethod(PaymentMethodCreateParamsFixtures.DEFAULT_CARD))
                    .thenReturn(createSuccessLiveData(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
                activity.createPaymentMethod(
                    viewModel,
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD
                )
                verifyFinishesWithResult(activityScenario.result)
            }
        }
    }

    @Test
    fun addFpx_whenServerReturnsSuccessAndUpdatesCustomer_finishesWithIntent() {
        activityScenarioFactory.create<AddPaymentMethodActivity>(
            createArgs(PaymentMethod.Type.Fpx)
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                val progressBar: ProgressBar = activity.findViewById(R.id.progress_bar)

                assertEquals(View.GONE, progressBar.visibility)

                whenever(viewModel.createPaymentMethod(PaymentMethodCreateParamsFixtures.DEFAULT_FPX))
                    .thenReturn(createSuccessLiveData(PaymentMethodFixtures.FPX_PAYMENT_METHOD))
                activity.createPaymentMethod(viewModel, PaymentMethodCreateParamsFixtures.DEFAULT_FPX)

                val expectedPaymentMethod = PaymentMethodFixtures.FPX_PAYMENT_METHOD

                verify(customerSession, never()).attachPaymentMethod(
                    any(),
                    any(),
                    any()
                )

                assertEquals(RESULT_OK, activityScenario.result.resultCode)
                assertThat(
                    getPaymentMethodFromIntent(activityScenario.result.resultData)
                ).isEqualTo(expectedPaymentMethod)
            }
        }
    }

    @Test
    fun addCardData_withFullBillingFieldsRequirement_shouldShowBillingAddressWidget() {
        activityScenarioFactory.create<AddPaymentMethodActivity>(
            createArgs(
                PaymentMethod.Type.Card,
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
            createArgs(PaymentMethod.Type.Card)
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                val progressBar: ProgressBar = activity.findViewById(R.id.progress_bar)
                val cardMultilineWidget: CardMultilineWidget = activity.findViewById(R.id.card_multiline_widget)

                assertEquals(View.GONE, progressBar.visibility)
                assertTrue(cardMultilineWidget.isEnabled)

                whenever(viewModel.createPaymentMethod(PaymentMethodCreateParamsFixtures.DEFAULT_CARD))
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

                whenever(viewModel.createPaymentMethod(PaymentMethodCreateParamsFixtures.DEFAULT_CARD))
                    .thenReturn(createErrorLiveData())
                activity.createPaymentMethod(
                    viewModel,
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD
                )

                val result = AddPaymentMethodActivityStarter.Result.fromIntent(
                    shadowOf(activity).resultIntent
                )
                assertThat(result)
                    .isEqualTo(AddPaymentMethodActivityStarter.Result.Canceled)

                assertFalse(activity.isFinishing)
                assertEquals(View.GONE, progressBar.visibility)

                verifyDialogWithMessage(ERROR_MESSAGE)
            }
        }
    }

    @Test
    fun addCardData_whenPaymentMethodCreateWorksButAddToCustomerFails_showErrorNotFinish() {
        activityScenarioFactory.create<AddPaymentMethodActivity>(
            createArgs(PaymentMethod.Type.Card)
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                val progressBar: ProgressBar = activity.findViewById(R.id.progress_bar)
                val cardMultilineWidget: CardMultilineWidget = activity.findViewById(R.id.card_multiline_widget)

                assertEquals(View.GONE, progressBar.visibility)
                assertTrue(cardMultilineWidget.isEnabled)

                whenever(viewModel.createPaymentMethod(PaymentMethodCreateParamsFixtures.DEFAULT_CARD))
                    .thenReturn(createSuccessLiveData(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
                activity.createPaymentMethod(viewModel, PaymentMethodCreateParamsFixtures.DEFAULT_CARD)

                assertEquals(View.VISIBLE, progressBar.visibility)
                assertFalse(cardMultilineWidget.isEnabled)

                verify(customerSession).attachPaymentMethod(
                    paymentMethodIdCaptor.capture(),
                    productUsageArgumentCaptor.capture(),
                    listenerArgumentCaptor.capture()
                )

                assertThat(
                    productUsageArgumentCaptor.firstValue
                ).containsExactly(
                    AddPaymentMethodActivity.PRODUCT_TOKEN,
                    PaymentSession.PRODUCT_TOKEN
                )

                assertEquals(EXPECTED_PAYMENT_METHOD.id, paymentMethodIdCaptor.firstValue)

                val error: StripeException = mock()
                whenever(error.localizedMessage).thenReturn(ERROR_MESSAGE)
                listenerArgumentCaptor.firstValue.onError(400, ERROR_MESSAGE, null)

                val result = AddPaymentMethodActivityStarter.Result.fromIntent(
                    shadowOf(activity).resultIntent
                )
                assertThat(result)
                    .isEqualTo(AddPaymentMethodActivityStarter.Result.Canceled)

                assertFalse(activity.isFinishing)
                assertEquals(View.GONE, progressBar.visibility)

                verifyDialogWithMessage(ERROR_MESSAGE)
            }
        }
    }

    @Test
    fun `createPaymentMethod when CustomerSession is null and should attach should finish Activity`() {
        CustomerSession.instance = null

        whenever(viewModel.createPaymentMethod(PaymentMethodCreateParamsFixtures.DEFAULT_CARD))
            .thenReturn(createSuccessLiveData(PaymentMethodFixtures.CARD_PAYMENT_METHOD))

        activityScenarioFactory.create<AddPaymentMethodActivity>(
            createArgs(PaymentMethod.Type.Card)
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                activity.createPaymentMethod(
                    viewModel,
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD
                )

                val result = AddPaymentMethodActivityStarter.Result.fromIntent(
                    shadowOf(activity).resultIntent
                ) as? AddPaymentMethodActivityStarter.Result.Failure
                assertThat(result?.exception?.message)
                    .isEqualTo("Attempted to get instance of CustomerSession without initialization.")

                assertThat(activity.isFinishing)
                    .isTrue()
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
        assertThat(getPaymentMethodFromIntent(activityResult.resultData))
            .isEqualTo(EXPECTED_PAYMENT_METHOD)
    }

    private fun getPaymentMethodFromIntent(intent: Intent): PaymentMethod? {
        val result =
            AddPaymentMethodActivityStarter.Result.fromIntent(intent)
        return when (result) {
            is AddPaymentMethodActivityStarter.Result.Success -> result.paymentMethod
            else -> null
        }
    }

    private fun createArgs(
        paymentMethodType: PaymentMethod.Type,
        billingAddressFields: BillingAddressFields = BillingAddressFields.PostalCode
    ): AddPaymentMethodActivityStarter.Args {
        return AddPaymentMethodActivityStarter.Args.Builder()
            .setShouldAttachToCustomer(true)
            .setIsPaymentSessionActive(true)
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
        ): LiveData<Result<PaymentMethod>> {
            return MutableLiveData(Result.success(paymentMethod))
        }

        private fun createErrorLiveData(): LiveData<Result<PaymentMethod>> {
            return MutableLiveData(
                Result.failure(RuntimeException(ERROR_MESSAGE))
            )
        }
    }
}
