package com.stripe.android.view;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.stripe.android.BuildConfig;
import com.stripe.android.CustomerSessionTest;
import com.stripe.android.R;
import com.stripe.android.model.Customer;
import com.stripe.android.model.CustomerTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import static com.stripe.android.view.PaymentMethodsActivity.EXTRA_PROXY_DELAY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link PaymentMethodsActivity}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class PaymentMethodsActivityTest {

    @Mock PaymentMethodsActivity.CustomerSessionProxy mCustomerSessionProxy;

    private ActivityController<PaymentMethodsActivity> mActivityController;
    private TextView mErrorTextView;
    private FrameLayout mErrorLayout;
    private ProgressBar mProgressBar;
    private RecyclerView mRecyclerView;
    private View mAddCardView;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Intent intent = PaymentMethodsActivity.newIntent(RuntimeEnvironment.application);
        intent.putExtra(EXTRA_PROXY_DELAY, true);
        mActivityController = Robolectric.buildActivity(PaymentMethodsActivity.class, intent)
                .create().start().resume().visible();
        mActivityController.get().setCustomerSessionProxy(mCustomerSessionProxy);
        mProgressBar = mActivityController.get().findViewById(R.id.payment_methods_progress_bar);
        mRecyclerView = mActivityController.get().findViewById(R.id.payment_methods_recycler);
        mErrorLayout = mActivityController.get().findViewById(R.id.payment_methods_error_container);
        mErrorTextView = mActivityController.get().findViewById(R.id.tv_payment_methods_error);
        mAddCardView = mActivityController.get().findViewById(R.id.payment_methods_add_payment_container);
    }

    @Test
    public void onCreate_withCachedCustomer_showsUi() {
        Customer customer = Customer.fromString(CustomerSessionTest.FIRST_TEST_CUSTOMER_OBJECT);
        when(mCustomerSessionProxy.getCachedCustomer()).thenReturn(customer);
        mActivityController.get().initializeData();

        assertNotNull(mProgressBar);
        assertNotNull(mRecyclerView);
        assertNotNull(mErrorLayout);
        assertNotNull(mErrorTextView);
        assertNotNull(mAddCardView);
        assertEquals(View.VISIBLE, mAddCardView.getVisibility());
        assertEquals(View.VISIBLE, mRecyclerView.getVisibility());
        assertEquals(View.GONE, mErrorTextView.getVisibility());
        assertEquals(View.VISIBLE, mErrorLayout.getVisibility());
    }
}
