package com.stripe.android.view;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.stripe.android.BuildConfig;
import com.stripe.android.CustomerSession;
import com.stripe.android.CustomerSessionTest;
import com.stripe.android.R;
import com.stripe.android.model.Customer;
import com.stripe.android.model.CustomerSource;
import com.stripe.android.model.Source;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import java.util.List;

import static android.app.Activity.RESULT_OK;
import static com.stripe.android.PaymentSession.EXTRA_PAYMENT_SESSION_ACTIVE;
import static com.stripe.android.view.PaymentMethodsActivity.EXTRA_PROXY_DELAY;
import static com.stripe.android.view.PaymentMethodsActivity.EXTRA_SELECTED_PAYMENT;
import static com.stripe.android.view.PaymentMethodsActivity.REQUEST_CODE_ADD_CARD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link PaymentMethodsActivity}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class PaymentMethodsActivityTest {

    static final String TEST_CUSTOMER_OBJECT_WITH_SOURCES =
            "{\n" +
                    "  \"id\": \"cus_AQsHpvKfKwJDrF\",\n" +
                    "  \"object\": \"customer\",\n" +
                    "  \"default_source\": \"src_19t3xKBZqEXluyI4uz2dxAfQ\",\n" +
                    "  \"sources\": {\n" +
                    "    \"object\": \"list\",\n" +
                    "    \"data\": [\n" +
                         CardInputTestActivity.EXAMPLE_JSON_CARD_SOURCE + ",\n" +
                         CardInputTestActivity.EXAMPLE_JSON_CARD_SOURCE_SECOND +
                    "\n" +
                    "    ],\n" +
                    "    \"has_more\": false,\n" +
                    "    \"total_count\": 2,\n" +
                    "    \"url\": \"/v1/customers/cus_AQsHpvKfKwJDrF/sources\"\n" +
                    "  }\n" +
                    "}";

    private static final String TEST_CUSTOMER_OBJECT_WITH_ONE_SOURCE_NO_SELECTION =
            "{\n" +
                    "  \"id\": \"cus_AQsHpvKfKwJDrF\",\n" +
                    "  \"object\": \"customer\",\n" +
                    "  \"default_source\": null,\n" +
                    "  \"sources\": {\n" +
                    "    \"object\": \"list\",\n" +
                    "    \"data\": [\n" +
                    CardInputTestActivity.EXAMPLE_JSON_CARD_SOURCE + ",\n" +
                    "\n" +
                    "    ],\n" +
                    "    \"has_more\": false,\n" +
                    "    \"total_count\": 2,\n" +
                    "    \"url\": \"/v1/customers/cus_AQsHpvKfKwJDrF/sources\"\n" +
                    "  }\n" +
                    "}";

    @Mock PaymentMethodsActivity.CustomerSessionProxy mCustomerSessionProxy;

    private ActivityController<PaymentMethodsActivity> mActivityController;
    private ProgressBar mProgressBar;
    private RecyclerView mRecyclerView;
    private View mAddCardView;
    private ShadowActivity mShadowActivity;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Intent intent = PaymentMethodsActivity.newIntent(RuntimeEnvironment.application);
        intent.putExtra(EXTRA_PROXY_DELAY, true);
        mActivityController = Robolectric.buildActivity(PaymentMethodsActivity.class, intent)
                .create().start().resume().visible();
        mActivityController.get().setCustomerSessionProxy(mCustomerSessionProxy);
        mShadowActivity = Shadows.shadowOf(mActivityController.get());

        mProgressBar = mActivityController.get().findViewById(R.id.payment_methods_progress_bar);
        mRecyclerView = mActivityController.get().findViewById(R.id.payment_methods_recycler);
        mAddCardView = mActivityController.get().findViewById(R.id.payment_methods_add_payment_container);
    }

    @Test
    public void onCreate_withCachedCustomer_showsUi() {
        Customer customer = Customer.fromString(CustomerSessionTest.FIRST_TEST_CUSTOMER_OBJECT);
        when(mCustomerSessionProxy.getCachedCustomer()).thenReturn(customer);
        mActivityController.get().initializeCustomerSourceData();

        assertNotNull(mProgressBar);
        assertNotNull(mRecyclerView);
        assertNotNull(mAddCardView);
        assertEquals(View.VISIBLE, mAddCardView.getVisibility());
        assertEquals(View.VISIBLE, mRecyclerView.getVisibility());
        assertEquals(View.GONE, mProgressBar.getVisibility());
    }

    @Test
    public void onCreate_withoutCacheCustomer_callsApiAndDisplaysProgressBarWhileWaiting() {
        Customer customer = Customer.fromString(CustomerSessionTest.FIRST_TEST_CUSTOMER_OBJECT);
        assertNotNull(customer);
        when(mCustomerSessionProxy.getCachedCustomer()).thenReturn(null);
        ArgumentCaptor<CustomerSession.CustomerRetrievalListener> listenerArgumentCaptor =
                ArgumentCaptor.forClass(CustomerSession.CustomerRetrievalListener.class);

        assertNotNull(mProgressBar);
        assertNotNull(mRecyclerView);
        assertNotNull(mAddCardView);

        mActivityController.get().initializeCustomerSourceData();
        verify(mCustomerSessionProxy).retrieveCurrentCustomer(listenerArgumentCaptor.capture());
        assertEquals(View.VISIBLE, mProgressBar.getVisibility());
        assertEquals(View.VISIBLE, mAddCardView.getVisibility());
        assertEquals(View.VISIBLE, mRecyclerView.getVisibility());

        CustomerSession.CustomerRetrievalListener listener = listenerArgumentCaptor.getValue();
        assertNotNull(listener);

        listener.onCustomerRetrieved(customer);

        assertEquals(View.GONE, mProgressBar.getVisibility());
    }

    @Test
    public void onClickAddSourceView_withoutPaymentSessoin_launchesAddSourceActivityWithoutLog() {
        Customer customer = Customer.fromString(CustomerSessionTest.FIRST_TEST_CUSTOMER_OBJECT);
        when(mCustomerSessionProxy.getCachedCustomer()).thenReturn(customer);
        mActivityController.get().initializeCustomerSourceData();

        mAddCardView.performClick();
        ShadowActivity.IntentForResult intentForResult =
                mShadowActivity.getNextStartedActivityForResult();
        assertNotNull(intentForResult);
        assertEquals(AddSourceActivity.class.getName(),
                intentForResult.intent.getComponent().getClassName());
        assertFalse(intentForResult.intent.hasExtra(EXTRA_PAYMENT_SESSION_ACTIVE));
    }

    @Test
    public void onClickAddSourceView_whenStartedFromPaymentSession_launchesActivityWithLog() {
        Intent intent = PaymentMethodsActivity.newIntent(RuntimeEnvironment.application);
        intent.putExtra(EXTRA_PROXY_DELAY, true);
        intent.putExtra(EXTRA_PAYMENT_SESSION_ACTIVE, true);
        mActivityController = Robolectric.buildActivity(PaymentMethodsActivity.class, intent)
                .create().start().resume().visible();
        mActivityController.get().setCustomerSessionProxy(mCustomerSessionProxy);
        mShadowActivity = Shadows.shadowOf(mActivityController.get());

        mAddCardView = mActivityController.get().findViewById(R.id.payment_methods_add_payment_container);

        Customer customer = Customer.fromString(CustomerSessionTest.FIRST_TEST_CUSTOMER_OBJECT);
        when(mCustomerSessionProxy.getCachedCustomer()).thenReturn(customer);
        mActivityController.get().initializeCustomerSourceData();

        mAddCardView.performClick();
        ShadowActivity.IntentForResult intentForResult =
                mShadowActivity.getNextStartedActivityForResult();
        assertNotNull(intentForResult);
        assertEquals(AddSourceActivity.class.getName(),
                intentForResult.intent.getComponent().getClassName());
        assertTrue(intentForResult.intent.hasExtra(EXTRA_PAYMENT_SESSION_ACTIVE));
    }

    @Test
    public void onActivityResult_withValidSource_refreshesCustomer() {
        Customer customer = Customer.fromString(CustomerSessionTest.FIRST_TEST_CUSTOMER_OBJECT);
        when(mCustomerSessionProxy.getCachedCustomer()).thenReturn(customer);
        mActivityController.get().initializeCustomerSourceData();

        Source source = Source.fromString(CardInputTestActivity.EXAMPLE_JSON_CARD_SOURCE);
        assertNotNull(source);

        Intent resultIntent = new Intent();
        resultIntent.putExtra(AddSourceActivity.EXTRA_NEW_SOURCE, source.toString());

        ArgumentCaptor<CustomerSession.CustomerRetrievalListener> listenerArgumentCaptor =
                ArgumentCaptor.forClass(CustomerSession.CustomerRetrievalListener.class);

        mActivityController.get().onActivityResult(REQUEST_CODE_ADD_CARD, RESULT_OK, resultIntent);
        assertEquals(View.VISIBLE, mProgressBar.getVisibility());
        verify(mCustomerSessionProxy).updateCurrentCustomer(listenerArgumentCaptor.capture());

        CustomerSession.CustomerRetrievalListener listener = listenerArgumentCaptor.getValue();
        assertNotNull(listener);

        // Note - this doesn't make sense as the actual update; just testing that the customer
        // changes
        Customer updatedCustomer = Customer.fromString(TEST_CUSTOMER_OBJECT_WITH_SOURCES);
        assertNotNull(updatedCustomer);

        listener.onCustomerRetrieved(updatedCustomer);
        assertEquals(View.GONE, mProgressBar.getVisibility());
        assertEquals(2, mRecyclerView.getAdapter().getItemCount());
    }

    @Test
    public void onActivityResult_whenOneSourceButNoSelection_updatesSelectedItem() {
        Customer customer = Customer.fromString(CustomerSessionTest.FIRST_TEST_CUSTOMER_OBJECT);
        when(mCustomerSessionProxy.getCachedCustomer()).thenReturn(customer);
        mActivityController.get().initializeCustomerSourceData();

        Source source = Source.fromString(CardInputTestActivity.EXAMPLE_JSON_CARD_SOURCE);
        assertNotNull(source);

        Intent resultIntent = new Intent();
        resultIntent.putExtra(AddSourceActivity.EXTRA_NEW_SOURCE, source.toString());

        ArgumentCaptor<CustomerSession.CustomerRetrievalListener> listenerArgumentCaptor =
                ArgumentCaptor.forClass(CustomerSession.CustomerRetrievalListener.class);

        mActivityController.get().onActivityResult(REQUEST_CODE_ADD_CARD, RESULT_OK, resultIntent);
        assertEquals(View.VISIBLE, mProgressBar.getVisibility());
        verify(mCustomerSessionProxy).updateCurrentCustomer(listenerArgumentCaptor.capture());

        CustomerSession.CustomerRetrievalListener listener = listenerArgumentCaptor.getValue();
        assertNotNull(listener);

        // Note - this doesn't make sense as the actual update; just testing that the customer
        // changes and sending an appropriate customer for continuing the flow of the test
        Customer updatedCustomer =
                Customer.fromString(TEST_CUSTOMER_OBJECT_WITH_ONE_SOURCE_NO_SELECTION);
        assertNotNull(updatedCustomer);

        ArgumentCaptor<CustomerSession.CustomerRetrievalListener> selectionCaptor =
                ArgumentCaptor.forClass(CustomerSession.CustomerRetrievalListener.class);
        ArgumentCaptor<String> stringArgumentCaptor =
                ArgumentCaptor.forClass(String.class);
        listener.onCustomerRetrieved(updatedCustomer);
        // Progress bar stays visible because we have another server trip to make
        assertEquals(View.VISIBLE, mProgressBar.getVisibility());

        verify(mCustomerSessionProxy).setCustomerDefaultSource(
                stringArgumentCaptor.capture(),
                eq(Source.CARD),
                selectionCaptor.capture());

        CustomerSession.CustomerRetrievalListener updateListener = selectionCaptor.getValue();
        assertEquals(updatedCustomer.getSources().get(0).getId(), stringArgumentCaptor.getValue());
        assertNotNull(updateListener);

        // Finally, just make sure we update the adapter and turn off the progress bar
        Customer anotherCustomer = Customer.fromString(TEST_CUSTOMER_OBJECT_WITH_SOURCES);
        assertNotNull(anotherCustomer);

        updateListener.onCustomerRetrieved(anotherCustomer);
        assertEquals(View.GONE, mProgressBar.getVisibility());
        assertEquals(2, mRecyclerView.getAdapter().getItemCount());
    }

    @Test
    public void onSaveMenuItem_sendsSelectionToApi_finishedWithExpectedResult() {
        Customer customer = Customer.fromString(TEST_CUSTOMER_OBJECT_WITH_SOURCES);
        assertNotNull(customer);
        assertNotNull(customer.getDefaultSource());
        List<CustomerSource> sourceList = customer.getSources();
        // Make sure our customer is set up correctly.
        assertEquals(2, sourceList.size());
        assertEquals(customer.getDefaultSource(), sourceList.get(0).getId());

        when(mCustomerSessionProxy.getCachedCustomer()).thenReturn(customer);
        mActivityController.get().initializeCustomerSourceData();

        assertEquals(View.GONE, mProgressBar.getVisibility());

        MenuItem menuItem = mock(MenuItem.class);
        when(menuItem.getItemId()).thenReturn(R.id.action_save);
        ArgumentCaptor<String> selectionArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CustomerSession.CustomerRetrievalListener> listenerArgumentCaptor =
                ArgumentCaptor.forClass(CustomerSession.CustomerRetrievalListener.class);

        mActivityController.get().onOptionsItemSelected(menuItem);

        verify(mCustomerSessionProxy).setCustomerDefaultSource(
                selectionArgumentCaptor.capture(),
                eq(Source.CARD),
                listenerArgumentCaptor.capture());

        assertEquals(customer.getDefaultSource(), selectionArgumentCaptor.getValue());
        CustomerSession.CustomerRetrievalListener listener = listenerArgumentCaptor.getValue();
        assertNotNull(listener);

        // We should be displaying the progress bar now.
        assertEquals(View.VISIBLE, mProgressBar.getVisibility());

        listener.onCustomerRetrieved(customer);
        // Now it should be gone.
        assertEquals(View.GONE, mProgressBar.getVisibility());
        assertTrue(mShadowActivity.isFinishing());
        assertEquals(RESULT_OK, mShadowActivity.getResultCode());
        Intent intent = mShadowActivity.getResultIntent();
        assertNotNull(intent);

        CustomerSource customerSource = customer.getSourceById(customer.getDefaultSource());
        assertNotNull(customerSource);
        assertEquals(customerSource.toString(), intent.getStringExtra(EXTRA_SELECTED_PAYMENT));
    }
}
