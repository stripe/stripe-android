package com.stripe.android.view

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
import com.stripe.android.CustomerSessionTestHelper
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentSession.Companion.EXTRA_PAYMENT_SESSION_ACTIVE
import com.stripe.android.R
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowActivity

/**
 * Test class for [PaymentMethodsActivity].
 */
@RunWith(RobolectricTestRunner::class)
class PaymentMethodsActivityTest : BaseViewTest<PaymentMethodsActivity>(PaymentMethodsActivity::class.java) {
    @Mock
    private lateinit var customerSession: CustomerSession

    private lateinit var listenerArgumentCaptor: KArgumentCaptor<CustomerSession.PaymentMethodsRetrievalListener>

    private lateinit var context: Context
    private lateinit var paymentMethodsActivity: PaymentMethodsActivity
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var addCardView: View
    private lateinit var shadowActivity: ShadowActivity

    @BeforeTest
    fun setup() {
        MockitoAnnotations.initMocks(this)
        listenerArgumentCaptor = argumentCaptor()

        context = ApplicationProvider.getApplicationContext()

        CustomerSessionTestHelper.setInstance(customerSession)
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        paymentMethodsActivity = createActivity(
            PaymentMethodsActivityStarter.Args.Builder()
                .setPaymentConfiguration(PaymentConfiguration.getInstance(context))
                .build()
        )
        shadowActivity = Shadows.shadowOf(paymentMethodsActivity)

        progressBar = paymentMethodsActivity.findViewById(R.id.payment_methods_progress_bar)
        recyclerView = paymentMethodsActivity.findViewById(R.id.payment_methods_recycler)
        addCardView = paymentMethodsActivity
            .findViewById(R.id.stripe_payment_methods_add_card)
    }

    @AfterTest
    override fun tearDown() {
        super.tearDown()
    }

    @Test
    fun onCreate_callsApiAndDisplaysProgressBarWhileWaiting() {
        assertNotNull(progressBar)
        assertNotNull(recyclerView)
        assertNotNull(addCardView)

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

    @Test
    fun onCreate_initialGivenPaymentMethodIsSelected() {
        // reset the mock because the activity is being re-created again
        reset<CustomerSession>(customerSession)
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHODS[0]
        assertNotNull(paymentMethod)
        paymentMethodsActivity = createActivity(PaymentMethodsActivityStarter.Args.Builder()
            .setInitialPaymentMethodId(paymentMethod.id)
            .build())
        recyclerView = paymentMethodsActivity.findViewById(R.id.payment_methods_recycler)

        verify(customerSession)
            .getPaymentMethods(eq(PaymentMethod.Type.Card), listenerArgumentCaptor.capture())

        listenerArgumentCaptor.firstValue
            .onPaymentMethodsRetrieved(PaymentMethodFixtures.CARD_PAYMENT_METHODS)

        val paymentMethodsAdapter =
            recyclerView.adapter as PaymentMethodsAdapter
        assertEquals(paymentMethod.id, paymentMethodsAdapter.selectedPaymentMethod?.id)
    }

    @Test
    fun onClickAddSourceView_withoutPaymentSession_launchesAddSourceActivityWithoutLog() {
        addCardView.performClick()
        val intentForResult = shadowActivity.nextStartedActivityForResult
        val component = intentForResult.intent.component
        assertEquals(AddPaymentMethodActivity::class.java.name, component?.className)
        assertFalse(intentForResult.intent.hasExtra(EXTRA_PAYMENT_SESSION_ACTIVE))
    }

    @Test
    fun onClickAddSourceView_whenStartedFromPaymentSession_launchesActivityWithLog() {
        paymentMethodsActivity = createActivity(PaymentMethodsActivityStarter.Args.Builder()
            .setPaymentConfiguration(PaymentConfiguration.getInstance(context))
            .setIsPaymentSessionActive(true)
            .build())
        shadowActivity = Shadows.shadowOf(paymentMethodsActivity)
        addCardView = paymentMethodsActivity.findViewById(R.id.stripe_payment_methods_add_card)
        addCardView.performClick()
        val intentForResult = shadowActivity.nextStartedActivityForResult
        val component = intentForResult.intent.component
        assertEquals(AddPaymentMethodActivity::class.java.name, component?.className)
        assertTrue(AddPaymentMethodActivityStarter.Args.create(intentForResult.intent)
            .isPaymentSessionActive)
    }

    @Test
    fun onActivityResult_withValidPaymentMethod_refreshesPaymentMethods() {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHODS[2]
        assertNotNull(paymentMethod)

        val resultIntent = Intent()
            .putExtras(AddPaymentMethodActivityStarter.Result(paymentMethod).toBundle())
        paymentMethodsActivity.onActivityResult(
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

    @Test
    fun setSelectionAndFinish_finishedWithExpectedResult() {
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
            recyclerView.adapter as PaymentMethodsAdapter?
        assertNotNull(paymentMethodsAdapter)
        paymentMethodsAdapter.selectedPaymentMethodId =
            PaymentMethodFixtures.CARD_PAYMENT_METHODS[0].id

        paymentMethodsActivity.onBackPressed()

        // Now it should be gone.
        assertEquals(View.GONE, progressBar.visibility)
        assertTrue(paymentMethodsActivity.isFinishing)
        assertEquals(RESULT_OK, shadowActivity.resultCode)
        val intent = shadowActivity.resultIntent
        assertNotNull(intent)

        val result =
            PaymentMethodsActivityStarter.Result.fromIntent(intent)
        assertEquals(PaymentMethodFixtures.CARD_PAYMENT_METHODS[0], result?.paymentMethod)
    }
}
