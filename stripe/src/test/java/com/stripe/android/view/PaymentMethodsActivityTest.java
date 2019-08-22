package com.stripe.android.view;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.stripe.android.ApiKeyFixtures;
import com.stripe.android.CustomerSession;
import com.stripe.android.CustomerSessionTestHelper;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.R;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodTest;

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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;
import static com.stripe.android.PaymentSession.EXTRA_PAYMENT_SESSION_ACTIVE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link PaymentMethodsActivity}.
 */
@RunWith(RobolectricTestRunner.class)
public class PaymentMethodsActivityTest extends BaseViewTest<PaymentMethodsActivity> {
    private static final List<PaymentMethod> PAYMENT_METHODS = Arrays.asList(
            PaymentMethod.fromString(PaymentMethodTest.PM_CARD_JSON),
            PaymentMethod.fromString(PaymentMethodsAdapterTest.PAYMENT_METHOD_JSON)
    );

    @Mock private CustomerSession mCustomerSession;
    @Captor private ArgumentCaptor<CustomerSession.PaymentMethodsRetrievalListener> mListenerArgumentCaptor;

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
        CustomerSessionTestHelper.setInstance(mCustomerSession);
        PaymentConfiguration.init(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY);

        mPaymentMethodsActivity = createActivity(
                new PaymentMethodsActivityStarter.Args.Builder()
                        .setPaymentConfiguration(PaymentConfiguration.getInstance())
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

        listener.onPaymentMethodsRetrieved(PAYMENT_METHODS);

        assertEquals(View.GONE, mProgressBar.getVisibility());
    }

    @Test
    public void onCreate_initialGivenPaymentMethodIsSelected() {
        // reset the mock because the activity is being re-created again
        reset(mCustomerSession);
        final PaymentMethod paymentMethod =
                PaymentMethod.fromString(PaymentMethodTest.PM_CARD_JSON);
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

        listener.onPaymentMethodsRetrieved(PAYMENT_METHODS);

        final PaymentMethodsAdapter paymentMethodsAdapter = (PaymentMethodsAdapter) mRecyclerView.getAdapter();
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
                .setPaymentConfiguration(PaymentConfiguration.getInstance())
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
        final PaymentMethod paymentMethod =
                PaymentMethod.fromString(PaymentMethodTest.PM_CARD_JSON);
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

        listener.onPaymentMethodsRetrieved(PAYMENT_METHODS);
        assertEquals(View.GONE, mProgressBar.getVisibility());
        assertNotNull(mRecyclerView.getAdapter());
        assertEquals(2, mRecyclerView.getAdapter().getItemCount());

        final PaymentMethodsAdapter paymentMethodsAdapter =
                (PaymentMethodsAdapter) mRecyclerView.getAdapter();
        assertNotNull(paymentMethodsAdapter);
        assertNotNull(paymentMethodsAdapter.getSelectedPaymentMethod());
        assertEquals(paymentMethod.id, paymentMethodsAdapter.getSelectedPaymentMethod().id);
    }

    @Test
    public void onSaveMenuItem_finishedWithExpectedResult() {

        verify(mCustomerSession).getPaymentMethods(eq(PaymentMethod.Type.Card),
                mListenerArgumentCaptor.capture());

        assertEquals(View.VISIBLE, mProgressBar.getVisibility());
        assertEquals(View.VISIBLE, mAddCardView.getVisibility());
        assertEquals(View.VISIBLE, mRecyclerView.getVisibility());

        final CustomerSession.PaymentMethodsRetrievalListener listener =
                mListenerArgumentCaptor.getValue();
        assertNotNull(listener);

        listener.onPaymentMethodsRetrieved(PAYMENT_METHODS);
        final PaymentMethodsAdapter paymentMethodsAdapter = (PaymentMethodsAdapter) mRecyclerView.getAdapter();
        assertNotNull(paymentMethodsAdapter);
        paymentMethodsAdapter.setSelectedIndex(0);

        final MenuItem menuItem = mock(MenuItem.class);
        when(menuItem.getItemId()).thenReturn(R.id.action_save);

        mPaymentMethodsActivity.onOptionsItemSelected(menuItem);

        // Now it should be gone.
        assertEquals(View.GONE, mProgressBar.getVisibility());
        assertTrue(mPaymentMethodsActivity.isFinishing());
        assertEquals(RESULT_OK, mShadowActivity.getResultCode());
        final Intent intent = mShadowActivity.getResultIntent();
        assertNotNull(intent);

        final PaymentMethodsActivityStarter.Result result =
                Objects.requireNonNull(PaymentMethodsActivityStarter.Result.fromIntent(intent));
        assertEquals(PAYMENT_METHODS.get(0), result.paymentMethod);
    }
}
