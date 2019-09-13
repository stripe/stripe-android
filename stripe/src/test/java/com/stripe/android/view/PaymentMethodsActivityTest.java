package com.stripe.android.view;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ProgressBar;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.ApiKeyFixtures;
import com.stripe.android.CustomerSession;
import com.stripe.android.CustomerSessionTestHelper;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.R;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodFixtures;

import java.util.Objects;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;

import static android.app.Activity.RESULT_OK;
import static com.stripe.android.PaymentSession.EXTRA_PAYMENT_SESSION_ACTIVE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


/**
 * Test class for {@link PaymentMethodsActivity}.
 */
@RunWith(RobolectricTestRunner.class)
public class PaymentMethodsActivityTest extends BaseViewTest<PaymentMethodsActivity> {
    @Mock private CustomerSession mCustomerSession;
    @Captor private ArgumentCaptor<CustomerSession.PaymentMethodsRetrievalListener> mListenerArgumentCaptor;

    private Context mContext;
    private PaymentMethodsActivity mPaymentMethodsActivity;
    private ProgressBar mProgressBar;
    private RecyclerView mRecyclerView;
    private View mAddCardView;
    private ShadowActivity mShadowActivity;

    public PaymentMethodsActivityTest() {
        super(PaymentMethodsActivity.class);
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mContext = ApplicationProvider.getApplicationContext();

        CustomerSessionTestHelper.setInstance(mCustomerSession);
        PaymentConfiguration.init(mContext, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY);

        mPaymentMethodsActivity = createActivity(
                new PaymentMethodsActivityStarter.Args.Builder()
                        .setPaymentConfiguration(PaymentConfiguration.getInstance(mContext))
                        .build()
        );
        mShadowActivity = Shadows.shadowOf(mPaymentMethodsActivity);

        mProgressBar = mPaymentMethodsActivity.findViewById(R.id.payment_methods_progress_bar);
        mRecyclerView = mPaymentMethodsActivity.findViewById(R.id.payment_methods_recycler);
        mAddCardView = mPaymentMethodsActivity
                .findViewById(R.id.payment_methods_add_card);
    }

    @After
    @Override
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void onCreate_callsApiAndDisplaysProgressBarWhileWaiting() {
        assertNotNull(mProgressBar);
        assertNotNull(mRecyclerView);
        assertNotNull(mAddCardView);

        verify(mCustomerSession).getPaymentMethods(eq(PaymentMethod.Type.Card),
                mListenerArgumentCaptor.capture());

        assertEquals(View.VISIBLE, mProgressBar.getVisibility());
        assertEquals(View.VISIBLE, mAddCardView.getVisibility());
        assertEquals(View.VISIBLE, mRecyclerView.getVisibility());

        final CustomerSession.PaymentMethodsRetrievalListener listener =
                mListenerArgumentCaptor.getValue();
        assertNotNull(listener);

        listener.onPaymentMethodsRetrieved(PaymentMethodFixtures.CARD_PAYMENT_METHODS);

        assertEquals(View.GONE, mProgressBar.getVisibility());
    }

    @Test
    public void onCreate_initialGivenPaymentMethodIsSelected() {
        // reset the mock because the activity is being re-created again
        reset(mCustomerSession);
        final PaymentMethod paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHODS.get(0);
        assertNotNull(paymentMethod);
        mPaymentMethodsActivity = createActivity(new PaymentMethodsActivityStarter.Args.Builder()
                .setInitialPaymentMethodId(paymentMethod.id)
                .build());
        mRecyclerView = mPaymentMethodsActivity.findViewById(R.id.payment_methods_recycler);

        verify(mCustomerSession).getPaymentMethods(eq(PaymentMethod.Type.Card),
                mListenerArgumentCaptor.capture());

        final CustomerSession.PaymentMethodsRetrievalListener listener =
                mListenerArgumentCaptor.getValue();
        assertNotNull(listener);

        listener.onPaymentMethodsRetrieved(PaymentMethodFixtures.CARD_PAYMENT_METHODS);

        final PaymentMethodsAdapter paymentMethodsAdapter =
                (PaymentMethodsAdapter) mRecyclerView.getAdapter();
        assertNotNull(paymentMethodsAdapter);
        assertNotNull(paymentMethodsAdapter.getSelectedPaymentMethod());
        assertEquals(paymentMethod.id, paymentMethodsAdapter.getSelectedPaymentMethod().id);
    }

    @Test
    public void onClickAddSourceView_withoutPaymentSession_launchesAddSourceActivityWithoutLog() {
        mAddCardView.performClick();
        final ShadowActivity.IntentForResult intentForResult =
                mShadowActivity.getNextStartedActivityForResult();
        assertNotNull(intentForResult);
        assertNotNull(intentForResult.intent.getComponent());
        assertEquals(AddPaymentMethodActivity.class.getName(),
                intentForResult.intent.getComponent().getClassName());
        assertFalse(intentForResult.intent.hasExtra(EXTRA_PAYMENT_SESSION_ACTIVE));
    }

    @Test
    public void onClickAddSourceView_whenStartedFromPaymentSession_launchesActivityWithLog() {
        mPaymentMethodsActivity = createActivity(new PaymentMethodsActivityStarter.Args.Builder()
                .setPaymentConfiguration(PaymentConfiguration.getInstance(mContext))
                .setIsPaymentSessionActive(true)
                .build());
        mShadowActivity = Shadows.shadowOf(mPaymentMethodsActivity);
        mAddCardView =
                mPaymentMethodsActivity.findViewById(R.id.payment_methods_add_card);
        mAddCardView.performClick();
        final ShadowActivity.IntentForResult intentForResult =
                mShadowActivity.getNextStartedActivityForResult();
        assertNotNull(intentForResult);
        assertNotNull(intentForResult.intent.getComponent());
        assertEquals(AddPaymentMethodActivity.class.getName(),
                intentForResult.intent.getComponent().getClassName());
        assertTrue(AddPaymentMethodActivityStarter.Args.create(intentForResult.intent)
                .isPaymentSessionActive);
    }

    @Test
    public void onActivityResult_withValidPaymentMethod_refreshesPaymentMethods() {
        final PaymentMethod paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHODS.get(2);
        assertNotNull(paymentMethod);

        final Intent resultIntent = new Intent()
                .putExtra(AddPaymentMethodActivity.EXTRA_NEW_PAYMENT_METHOD, paymentMethod);

        mPaymentMethodsActivity.onActivityResult(
                AddPaymentMethodActivityStarter.REQUEST_CODE, RESULT_OK, resultIntent
        );
        assertEquals(View.VISIBLE, mProgressBar.getVisibility());
        verify(mCustomerSession, times(2)).getPaymentMethods(
                eq(PaymentMethod.Type.Card), mListenerArgumentCaptor.capture());

        final CustomerSession.PaymentMethodsRetrievalListener listener =
                mListenerArgumentCaptor.getValue();
        assertNotNull(listener);

        listener.onPaymentMethodsRetrieved(PaymentMethodFixtures.CARD_PAYMENT_METHODS);
        assertEquals(View.GONE, mProgressBar.getVisibility());
        assertNotNull(mRecyclerView.getAdapter());
        assertEquals(4, mRecyclerView.getAdapter().getItemCount());

        final PaymentMethodsAdapter paymentMethodsAdapter =
                (PaymentMethodsAdapter) mRecyclerView.getAdapter();
        assertNotNull(paymentMethodsAdapter);
        assertNotNull(paymentMethodsAdapter.getSelectedPaymentMethod());
        assertEquals(paymentMethod.id, paymentMethodsAdapter.getSelectedPaymentMethod().id);
    }

    @Test
    public void setSelectionAndFinish_finishedWithExpectedResult() {
        verify(mCustomerSession).getPaymentMethods(eq(PaymentMethod.Type.Card),
                mListenerArgumentCaptor.capture());

        assertEquals(View.VISIBLE, mProgressBar.getVisibility());
        assertEquals(View.VISIBLE, mAddCardView.getVisibility());
        assertEquals(View.VISIBLE, mRecyclerView.getVisibility());

        final CustomerSession.PaymentMethodsRetrievalListener listener =
                mListenerArgumentCaptor.getValue();
        assertNotNull(listener);

        listener.onPaymentMethodsRetrieved(PaymentMethodFixtures.CARD_PAYMENT_METHODS);
        final PaymentMethodsAdapter paymentMethodsAdapter =
                (PaymentMethodsAdapter) mRecyclerView.getAdapter();
        assertNotNull(paymentMethodsAdapter);
        paymentMethodsAdapter.setSelectedIndex(0);

        mPaymentMethodsActivity.setSelectionAndFinish(
                PaymentMethodFixtures.CARD_PAYMENT_METHODS.get(0)
        );

        // Now it should be gone.
        assertEquals(View.GONE, mProgressBar.getVisibility());
        assertTrue(mPaymentMethodsActivity.isFinishing());
        assertEquals(RESULT_OK, mShadowActivity.getResultCode());
        final Intent intent = mShadowActivity.getResultIntent();
        assertNotNull(intent);

        final PaymentMethodsActivityStarter.Result result =
                Objects.requireNonNull(PaymentMethodsActivityStarter.Result.fromIntent(intent));
        assertEquals(PaymentMethodFixtures.CARD_PAYMENT_METHODS.get(0), result.paymentMethod);
    }
}
