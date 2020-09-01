package com.stripe.android.view

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import org.junit.runner.RunWith
import org.mockito.Mockito.times
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class for [PaymentMethodsActivity].
 */
@RunWith(RobolectricTestRunner::class)
class PaymentMethodsActivityTest {
    private val customerSession: CustomerSession = mock()

    private val listenerArgumentCaptor: KArgumentCaptor<CustomerSession.PaymentMethodsRetrievalListener> = argumentCaptor()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val activityScenarioFactory = ActivityScenarioFactory(context)

    @BeforeTest
    fun setup() {
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
                val progressBar = it.viewBinding.progressBar
                val recyclerView = it.viewBinding.recycler
                val addCardView: View = it.findViewById(R.id.stripe_payment_methods_add_card)

                verify(customerSession).getPaymentMethods(
                    eq(PaymentMethod.Type.Card),
                    isNull(),
                    isNull(),
                    isNull(),
                    eq(setOf(PaymentMethodsActivity.PRODUCT_TOKEN)),
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
                val recyclerView = it.viewBinding.recycler

                verify(customerSession)
                    .getPaymentMethods(
                        eq(PaymentMethod.Type.Card),
                        isNull(),
                        isNull(),
                        isNull(),
                        eq(setOf(PaymentMethodsActivity.PRODUCT_TOKEN)),
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
                assertTrue(
                    AddPaymentMethodActivityStarter.Args.create(intentForResult.intent)
                        .isPaymentSessionActive
                )
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
                val progressBar = activity.viewBinding.progressBar
                val recyclerView = activity.viewBinding.recycler

                val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHODS[2]
                assertNotNull(paymentMethod)

                val resultIntent = Intent()
                    .putExtras(
                        AddPaymentMethodActivityStarter.Result.Success(paymentMethod)
                            .toBundle()
                    )
                activity.onActivityResult(
                    AddPaymentMethodActivityStarter.REQUEST_CODE,
                    RESULT_OK,
                    resultIntent
                )
                assertEquals(View.VISIBLE, progressBar.visibility)
                verify(customerSession, times(2))
                    .getPaymentMethods(
                        eq(PaymentMethod.Type.Card),
                        isNull(),
                        isNull(),
                        isNull(),
                        eq(setOf(PaymentMethodsActivity.PRODUCT_TOKEN)),
                        listenerArgumentCaptor.capture()
                    )

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
                val progressBar = activity.viewBinding.progressBar
                val recyclerView = activity.viewBinding.recycler
                val addCardView: View = activity.findViewById(R.id.stripe_payment_methods_add_card)

                verify(customerSession).getPaymentMethods(
                    eq(PaymentMethod.Type.Card),
                    isNull(),
                    isNull(),
                    isNull(),
                    eq(setOf(PaymentMethodsActivity.PRODUCT_TOKEN)),
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
