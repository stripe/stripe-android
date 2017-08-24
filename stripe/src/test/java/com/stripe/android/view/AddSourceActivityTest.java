package com.stripe.android.view;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.stripe.android.BuildConfig;
import com.stripe.android.CustomerSession;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.R;
import com.stripe.android.SourceCallback;
import com.stripe.android.Stripe;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceParams;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import java.util.Calendar;

import static android.app.Activity.RESULT_OK;
import static com.stripe.android.view.AddSourceActivity.EXTRA_PROXY_DELAY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

/**
 * Test class for {@link AddSourceActivity}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class AddSourceActivityTest {

    private ActivityController<AddSourceActivity> mActivityController;
    private CardMultilineWidget mCardMultilineWidget;
    private CardMultilineWidgetTest.WidgetControlGroup mWidgetControlGroup;
    private ProgressBar mProgressBar;
    private ShadowActivity mShadowActivity;

    @Mock Stripe mStripe;
    @Mock AddSourceActivity.CustomerSessionProxy mCustomerSessionProxy;

    @Before
    public void setup() {
        // The input in this test class will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);
        MockitoAnnotations.initMocks(this);
    }

    private void setUpForLocalTest() {
        mActivityController = Robolectric.buildActivity(AddSourceActivity.class)
                .create().start().resume().visible();
        mCardMultilineWidget = mActivityController.get()
                .findViewById(R.id.add_source_card_entry_widget);
        mProgressBar = mActivityController.get()
                .findViewById(R.id.add_source_progress_bar);
        mWidgetControlGroup = new CardMultilineWidgetTest.WidgetControlGroup(mCardMultilineWidget);

        mShadowActivity = shadowOf(mActivityController.get());
        AddSourceActivity.StripeProvider mockStripeProvider =
                new AddSourceActivity.StripeProvider() {
                    @Override
                    public Stripe getStripe(@NonNull Context context) {
                        return mStripe;
                    }
                };
        mActivityController.get().setStripeProvider(mockStripeProvider);
    }

    private void setUpForProxySessionTest() {
        Intent intent = AddSourceActivity.newIntent(RuntimeEnvironment.application, true, true);
        intent.putExtra(EXTRA_PROXY_DELAY, true);
        mActivityController = Robolectric.buildActivity(AddSourceActivity.class, intent)
                .create().start().resume().visible();
        mCardMultilineWidget = mActivityController.get()
                .findViewById(R.id.add_source_card_entry_widget);
        mProgressBar = mActivityController.get()
                .findViewById(R.id.add_source_progress_bar);
        mWidgetControlGroup = new CardMultilineWidgetTest.WidgetControlGroup(mCardMultilineWidget);

        mShadowActivity = shadowOf(mActivityController.get());
        AddSourceActivity.StripeProvider mockStripeProvider =
                new AddSourceActivity.StripeProvider() {
                    @Override
                    public Stripe getStripe(@NonNull Context context) {
                        return mStripe;
                    }
                };
        mActivityController.get().setStripeProvider(mockStripeProvider);
        mActivityController.get().setCustomerSessionProxy(mCustomerSessionProxy);
    }

    @Test
    public void testConstructionForLocal() {
        setUpForLocalTest();
        assertNotNull(mCardMultilineWidget);
        assertEquals(View.GONE, mWidgetControlGroup.postalCodeInputLayout.getVisibility());
    }

    @Test
    public void testConstructionForCustomerSession() {
        setUpForProxySessionTest();
        assertNotNull(mCardMultilineWidget);
        assertEquals(View.VISIBLE, mWidgetControlGroup.postalCodeInputLayout.getVisibility());
    }

    @Test
    public void addCardData_whenDataIsValidAndServerReturnsSuccess_finishesWithIntent() {
        setUpForLocalTest();
        ArgumentCaptor<SourceParams> paramsArgumentCaptor =
                ArgumentCaptor.forClass(SourceParams.class);
        ArgumentCaptor<SourceCallback> callbackArgumentCaptor =
                ArgumentCaptor.forClass(SourceCallback.class);

        // Note: these values do not match what is being mock-sent back in the result.
        mWidgetControlGroup.cardNumberEditText.append(CardInputTestActivity.VALID_AMEX_NO_SPACES);
        mWidgetControlGroup.expiryDateEditText.append("12");
        mWidgetControlGroup.expiryDateEditText.append("50");
        mWidgetControlGroup.cvcEditText.append("1234");

        PaymentConfiguration.init("pk_test_abc123");
        MenuItem menuItem = mock(MenuItem.class);
        when(menuItem.getItemId()).thenReturn(R.id.action_save);

        assertEquals(View.GONE, mProgressBar.getVisibility());

        mActivityController.get().onOptionsItemSelected(menuItem);
        verify(mStripe).createSource(
                paramsArgumentCaptor.capture(),
                callbackArgumentCaptor.capture());
        SourceParams params = paramsArgumentCaptor.getValue();
        SourceCallback callback = callbackArgumentCaptor.getValue();
        assertNotNull(params);
        assertNotNull(callback);

        assertEquals(View.VISIBLE, mProgressBar.getVisibility());
        assertEquals(Source.CARD, params.getType());

        callback.onSuccess(Source.fromString(CardInputTestActivity.EXAMPLE_JSON_CARD_SOURCE));
        assertEquals(RESULT_OK, mShadowActivity.getResultCode());
        Intent intent = mShadowActivity.getResultIntent();

        assertTrue(mShadowActivity.isFinishing());
        assertTrue(intent.hasExtra(AddSourceActivity.EXTRA_NEW_SOURCE));
        Source source =
                Source.fromString(intent.getStringExtra(AddSourceActivity.EXTRA_NEW_SOURCE));
        assertNotNull(source);
        assertEquals(Source.CARD, source.getType());
    }

    @Test
    public void addCardData_whenServerReturnsSuccessAndUpdatesCustomer_finishesWithIntent() {
        setUpForProxySessionTest();
        ArgumentCaptor<SourceParams> paramsArgumentCaptor =
                ArgumentCaptor.forClass(SourceParams.class);
        ArgumentCaptor<SourceCallback> callbackArgumentCaptor =
                ArgumentCaptor.forClass(SourceCallback.class);

        // Note: these values do not match what is being mock-sent back in the result.
        mWidgetControlGroup.cardNumberEditText.append(CardInputTestActivity.VALID_AMEX_NO_SPACES);
        mWidgetControlGroup.expiryDateEditText.append("12");
        mWidgetControlGroup.expiryDateEditText.append("50");
        mWidgetControlGroup.cvcEditText.append("1234");
        mWidgetControlGroup.postalCodeEditText.append("90210");

        PaymentConfiguration.init("pk_test_abc123");
        MenuItem menuItem = mock(MenuItem.class);
        when(menuItem.getItemId()).thenReturn(R.id.action_save);

        assertEquals(View.GONE, mProgressBar.getVisibility());
        assertTrue(mCardMultilineWidget.isEnabled());

        mActivityController.get().onOptionsItemSelected(menuItem);
        verify(mStripe).createSource(
                paramsArgumentCaptor.capture(),
                callbackArgumentCaptor.capture());
        SourceParams params = paramsArgumentCaptor.getValue();
        SourceCallback callback = callbackArgumentCaptor.getValue();
        assertNotNull(params);
        assertNotNull(callback);

        assertEquals(View.VISIBLE, mProgressBar.getVisibility());
        assertFalse(mCardMultilineWidget.isEnabled());
        assertEquals(Source.CARD, params.getType());

        Source expectedSource = Source.fromString(CardInputTestActivity.EXAMPLE_JSON_CARD_SOURCE);
        assertNotNull(expectedSource);
        callback.onSuccess(expectedSource);

        ArgumentCaptor<String> sourceIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CustomerSession.SourceRetrievalListener> listenerArgumentCaptor =
                ArgumentCaptor.forClass(CustomerSession.SourceRetrievalListener.class);
        verify(mCustomerSessionProxy).addCustomerSource(
                sourceIdCaptor.capture(),
                listenerArgumentCaptor.capture());

        assertEquals(expectedSource.getId(), sourceIdCaptor.getValue());
        CustomerSession.SourceRetrievalListener listener = listenerArgumentCaptor.getValue();
        assertNotNull(listener);

        listener.onSourceRetrieved(expectedSource);

        assertEquals(RESULT_OK, mShadowActivity.getResultCode());
        Intent intent = mShadowActivity.getResultIntent();

        assertTrue(mShadowActivity.isFinishing());
        assertTrue(intent.hasExtra(AddSourceActivity.EXTRA_NEW_SOURCE));
        Source source =
                Source.fromString(intent.getStringExtra(AddSourceActivity.EXTRA_NEW_SOURCE));
        assertNotNull(source);
        assertEquals(Source.CARD, source.getType());
    }

    @Test
    public void addCardData_whenDataIsValidButServerReturnsError_doesNotFinish() {
        setUpForLocalTest();
        ArgumentCaptor<SourceParams> paramsArgumentCaptor =
                ArgumentCaptor.forClass(SourceParams.class);
        ArgumentCaptor<SourceCallback> callbackArgumentCaptor =
                ArgumentCaptor.forClass(SourceCallback.class);

        // Note: these values do not match what is being mock-sent back in the result.
        mWidgetControlGroup.cardNumberEditText.append(CardInputTestActivity.VALID_AMEX_NO_SPACES);
        mWidgetControlGroup.expiryDateEditText.append("12");
        mWidgetControlGroup.expiryDateEditText.append("50");
        mWidgetControlGroup.cvcEditText.append("1234");

        PaymentConfiguration.init("pk_test_abc123");
        MenuItem menuItem = mock(MenuItem.class);
        when(menuItem.getItemId()).thenReturn(R.id.action_save);

        assertEquals(View.GONE, mProgressBar.getVisibility());

        mActivityController.get().onOptionsItemSelected(menuItem);
        verify(mStripe).createSource(
                paramsArgumentCaptor.capture(),
                callbackArgumentCaptor.capture());
        SourceParams params = paramsArgumentCaptor.getValue();
        SourceCallback callback = callbackArgumentCaptor.getValue();
        assertNotNull(params);
        assertNotNull(callback);

        assertEquals(View.VISIBLE, mProgressBar.getVisibility());
        assertEquals(Source.CARD, params.getType());

        Exception error = mock(Exception.class);
        final String errorMessage = "Oh no! An Error!";
        when(error.getLocalizedMessage()).thenReturn(errorMessage);
        callback.onError(error);

        Intent intent = mShadowActivity.getResultIntent();
        assertNull(intent);

        assertFalse(mShadowActivity.isFinishing());
        assertEquals(View.GONE, mProgressBar.getVisibility());
    }
}
