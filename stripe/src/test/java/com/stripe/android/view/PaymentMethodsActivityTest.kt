package com.stripe.android.view

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Test class for [PaymentMethodsActivity].
 */
@RunWith(RobolectricTestRunner::class)
class PaymentMethodsActivityTest {
    @Mock
    private lateinit var customerSession: CustomerSession

    private lateinit var listenerArgumentCaptor: KArgumentCaptor<CustomerSession.PaymentMethodsRetrievalListener>

    private val context: Context by lazy {
        ApplicationProvider.getApplicationContext<Context>()
    }
    private val activityScenarioFactory: ActivityScenarioFactory by lazy {
        ActivityScenarioFactory(context)
    }

    @BeforeTest
    fun setup() {
        MockitoAnnotations.initMocks(this)
        listenerArgumentCaptor = argumentCaptor()

        CustomerSession.instance = customerSession
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
    }

    @Test
    fun onCreate_callsApiAndDisplaysProgressBarWhileWaiting() {
        activityScenarioFactory.create<PaymentMethodsActivity>(
            PaymentMethodsActivityStarter.Args.Builder()
                .setPaymentConfiguration(PaymentConfiguration.getInstance(context))
                .build()
        ).use { activityScenario ->
            activityScenario.onActivity {
                val progressBar: ProgressBar = it.findViewById(R.id.payment_methods_progress_bar)
                val recyclerView: RecyclerView = it.findViewById(R.id.payment_methods_recycler)
                val addCardView: View = it.findViewById(R.id.stripe_payment_methods_add_card)

                verify(customerSession).getPaymentMethods(
                    eq(PaymentMethod.Type.Card),
                    listenerArgumentCaptor.capture()
                )

                assertEquals(View.VISIBLE, progressBar.visibility)
                assertEquals(View.VISIBLE, addCardView.visibility)
                assertEquals(View.VISIBLE, recyclerView.visibility)

                listenerArgumentCaptor.firstValue
                    .onPaymentMethodsRetrieved(PaymentMethodFixtures.CARD_PAYMENT_METHODS)

                assertEquals(View.GONE, progressBar.visibility)
            }
        }
    }

    @Test
    fun onCreate_initialGivenPaymentMethodIsSelected() {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHODS[0]

        activityScenarioFactory.create<PaymentMethodsActivity>(
            PaymentMethodsActivityStarter.Args.Builder()
                .setInitialPaymentMethodId(paymentMethod.id)
                .setPaymentConfiguration(PaymentConfiguration.getInstance(context))
                .build()
        ).use { activityScenario ->
            activityScenario.onActivity {
                val recyclerView: RecyclerView = it.findViewById(R.id.payment_methods_recycler)

                verify(customerSession)
                    .getPaymentMethods(
                        eq(PaymentMethod.Type.Card),
                        listenerArgumentCaptor.capture()
                    )

                listenerArgumentCaptor.firstValue
                    .onPaymentMethodsRetrieved(PaymentMethodFixtures.CARD_PAYMENT_METHODS)

                val paymentMethodsAdapter =
                    recyclerView.adapter as PaymentMethodsAdapter
                assertEquals(paymentMethod.id, paymentMethodsAdapter.selectedPaymentMethod?.id)
            }
        }
    }

    @Test
    fun onClickAddClickView_withoutPaymentSession_launchesWithoutLog() {
        activityScenarioFactory.create<PaymentMethodsActivity>(
            PaymentMethodsActivityStarter.Args.Builder()
                .setPaymentConfiguration(PaymentConfiguration.getInstance(context))
                .build()
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                val addCardView: View = activity
                    .findViewById(R.id.stripe_payment_methods_add_card)

                addCardView.performClick()

                val intentForResult =
                    shadowOf(activity).nextStartedActivityForResult
                val component = intentForResult.intent.component
                assertEquals(AddPaymentMethodActivity::class.java.name, component?.className)
            }
        }
    }

    @Test
    fun onClickAddCardView_whenStartedFromPaymentSession_launchesActivityWithLog() {
        activityScenarioFactory.create<PaymentMethodsActivity>(
            PaymentMethodsActivityStarter.Args.Builder()
                .setPaymentConfiguration(PaymentConfiguration.getInstance(context))
                .setIsPaymentSessionActive(true)
                .build()
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                val addCardView: View = activity
                    .findViewById(R.id.stripe_payment_methods_add_card)
                addCardView.performClick()
                val intentForResult = shadowOf(activity).nextStartedActivityForResult
                val component = intentForResult.intent.component
                assertEquals(AddPaymentMethodActivity::class.java.name, component?.className)
                assertTrue(AddPaymentMethodActivityStarter.Args.create(intentForResult.intent)
                    .isPaymentSessionActive)
            }
        }
    }

    @Test
    fun onActivityResult_withValidPaymentMethod_refreshesPaymentMethods() {
        activityScenarioFactory.create<PaymentMethodsActivity>(
            PaymentMethodsActivityStarter.Args.Builder()
                .setPaymentConfiguration(PaymentConfiguration.getInstance(context))
                .build()
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                val progressBar: ProgressBar = activity.findViewById(R.id.payment_methods_progress_bar)
                val recyclerView: RecyclerView = activity.findViewById(R.id.payment_methods_recycler)

                val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHODS[2]
                assertNotNull(paymentMethod)

                val resultIntent = Intent()
                    .putExtras(AddPaymentMethodActivityStarter.Result(paymentMethod).toBundle())
                activity.onActivityResult(
                    AddPaymentMethodActivityStarter.REQUEST_CODE, RESULT_OK, resultIntent
                )
                assertEquals(View.VISIBLE, progressBar.visibility)
                verify(customerSession, times(2))
                    .getPaymentMethods(eq(PaymentMethod.Type.Card), listenerArgumentCaptor.capture())

                listenerArgumentCaptor.firstValue
                    .onPaymentMethodsRetrieved(PaymentMethodFixtures.CARD_PAYMENT_METHODS)
                assertEquals(View.GONE, progressBar.visibility)
                assertEquals(4, recyclerView.adapter?.itemCount)

                val paymentMethodsAdapter =
                    recyclerView.adapter as PaymentMethodsAdapter

                paymentMethodsAdapter.selectedPaymentMethodId = paymentMethod.id
                assertEquals(paymentMethod.id, paymentMethodsAdapter.selectedPaymentMethod?.id)
            }
        }
    }

    @Test
    fun setSelectionAndFinish_finishedWithExpectedResult() {
        activityScenarioFactory.create<PaymentMethodsActivity>(
            PaymentMethodsActivityStarter.Args.Builder()
                .setPaymentConfiguration(PaymentConfiguration.getInstance(context))
                .build()
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                val progressBar: ProgressBar =
                    activity.findViewById(R.id.payment_methods_progress_bar)
                val recyclerView: RecyclerView =
                    activity.findViewById(R.id.payment_methods_recycler)
                val addCardView: View =
                    activity.findViewById(R.id.stripe_payment_methods_add_card)

                verify(customerSession).getPaymentMethods(
                    eq(PaymentMethod.Type.Card),
                    listenerArgumentCaptor.capture()
                )

                assertEquals(View.VISIBLE, progressBar.visibility)
                assertEquals(View.VISIBLE, addCardView.visibility)
                assertEquals(View.VISIBLE, recyclerView.visibility)

                listenerArgumentCaptor.firstValue
                    .onPaymentMethodsRetrieved(PaymentMethodFixtures.CARD_PAYMENT_METHODS)
                val paymentMethodsAdapter =
                    recyclerView.adapter as PaymentMethodsAdapter
                paymentMethodsAdapter.selectedPaymentMethodId =
                    PaymentMethodFixtures.CARD_PAYMENT_METHODS[0].id

                activity.onBackPressed()

                // Now it should be gone.
                assertEquals(View.GONE, progressBar.visibility)
                assertTrue(activity.isFinishing)

                // `resultCode` is `RESULT_CANCELED` because back was pressed
                assertEquals(RESULT_CANCELED, activityScenario.result.resultCode)

                val result =
                    PaymentMethodsActivityStarter.Result.fromIntent(activityScenario.result.resultData)
                assertEquals(PaymentMethodFixtures.CARD_PAYMENT_METHODS[0], result?.paymentMethod)
            }
        }
    }
}
