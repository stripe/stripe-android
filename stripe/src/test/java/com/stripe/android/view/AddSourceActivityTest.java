package com.stripe.android.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ProgressBar;

import com.stripe.android.BuildConfig;
import com.stripe.android.CustomerSession;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.R;
import com.stripe.android.SourceCallback;
import com.stripe.android.Stripe;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceParams;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import java.util.Calendar;

import static android.app.Activity.RESULT_OK;
import static com.stripe.android.CustomerSession.ACTION_API_EXCEPTION;
import static com.stripe.android.CustomerSession.EXTRA_EXCEPTION;
import static com.stripe.android.PaymentSession.EXTRA_PAYMENT_SESSION_ACTIVE;
import static com.stripe.android.PaymentSession.TOKEN_PAYMENT_SESSION;
import static com.stripe.android.view.AddSourceActivity.ADD_SOURCE_ACTIVITY;
import static com.stripe.android.view.AddSourceActivity.EXTRA_PROXY_DELAY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
                .findViewById(R.id.progress_bar_as);
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
        intent.putExtra(EXTRA_PAYMENT_SESSION_ACTIVE, true);
        mActivityController = Robolectric.buildActivity(AddSourceActivity.class, intent)
                .create().start().resume().visible();
        mCardMultilineWidget = mActivityController.get()
                .findViewById(R.id.add_source_card_entry_widget);
        mProgressBar = mActivityController.get()
                .findViewById(R.id.progress_bar_as);
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
        mActivityController.get().initCustomerSessionTokens();
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
    public void softEnterKey_whenDataIsValid_hidesKeyboardAndFinishesWithIntent() {
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

        mWidgetControlGroup.cvcEditText.onEditorAction(EditorInfo.IME_ACTION_DONE);
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
    public void softEnterKey_whenDataIsNotValid_doesNotHideKeyboardAndDoesNotFinish() {
        setUpForLocalTest();
        // Note: these values do not match what is being mock-sent back in the result.
        mWidgetControlGroup.cardNumberEditText.append(CardInputTestActivity.VALID_AMEX_NO_SPACES);
        mWidgetControlGroup.expiryDateEditText.append("12");
        mWidgetControlGroup.expiryDateEditText.append("50");
        mWidgetControlGroup.cvcEditText.append("12");

        PaymentConfiguration.init("pk_test_abc123");
        MenuItem menuItem = mock(MenuItem.class);
        when(menuItem.getItemId()).thenReturn(R.id.action_save);

        assertEquals(View.GONE, mProgressBar.getVisibility());

        mWidgetControlGroup.cvcEditText.onEditorAction(EditorInfo.IME_ACTION_DONE);
        verify(mStripe,never()).createSource(
                any(SourceParams.class),
                any(SourceCallback.class));
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
        verify(mCustomerSessionProxy).addProductUsageTokenIfValid(ADD_SOURCE_ACTIVITY);
        verify(mCustomerSessionProxy).addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION);
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

        StripeActivity.AlertMessageListener alertMessageListener =
                Mockito.mock(StripeActivity.AlertMessageListener.class);
        mActivityController.get().setAlertMessageListener(alertMessageListener);

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

        StripeException error = mock(StripeException.class);
        final String errorMessage = "Oh no! An Error!";
        when(error.getLocalizedMessage()).thenReturn(errorMessage);
        callback.onError(error);

        Intent intent = mShadowActivity.getResultIntent();
        assertNull(intent);
        assertFalse(mShadowActivity.isFinishing());
        assertEquals(View.GONE, mProgressBar.getVisibility());
        verify(alertMessageListener, times(1)).onAlertMessageDisplayed(errorMessage);
    }

    @Test
    public void addCardData_whenSourceCreationWorksButAddToCustomerFails_showsErrorNotFinish() {
        setUpForProxySessionTest();
        ArgumentCaptor<SourceParams> paramsArgumentCaptor =
                ArgumentCaptor.forClass(SourceParams.class);
        ArgumentCaptor<SourceCallback> callbackArgumentCaptor =
                ArgumentCaptor.forClass(SourceCallback.class);
        StripeActivity.AlertMessageListener alertMessageListener =
                Mockito.mock(StripeActivity.AlertMessageListener.class);
        mActivityController.get().setAlertMessageListener(alertMessageListener);

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
        verify(mCustomerSessionProxy).addProductUsageTokenIfValid(ADD_SOURCE_ACTIVITY);
        verify(mCustomerSessionProxy).addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION);
        verify(mCustomerSessionProxy).addCustomerSource(
                sourceIdCaptor.capture(),
                listenerArgumentCaptor.capture());

        assertEquals(expectedSource.getId(), sourceIdCaptor.getValue());
        CustomerSession.SourceRetrievalListener listener = listenerArgumentCaptor.getValue();
        assertNotNull(listener);

        StripeException error = mock(StripeException.class);
        final String errorMessage = "Oh no! An Error!";
        when(error.getLocalizedMessage()).thenReturn(errorMessage);
        listener.onError(400, errorMessage);

        // We're mocking the CustomerSession, so we have to replicate its broadcast behavior.
        Bundle bundle = new Bundle();
        bundle.putSerializable(EXTRA_EXCEPTION, error);
        Intent errorIntent = new Intent(ACTION_API_EXCEPTION);
        errorIntent.putExtras(bundle);
        LocalBroadcastManager.getInstance(mActivityController.get()).sendBroadcast(errorIntent);

        Intent intent = mShadowActivity.getResultIntent();
        assertNull(intent);
        assertFalse(mShadowActivity.isFinishing());
        assertEquals(View.GONE, mProgressBar.getVisibility());
        verify(alertMessageListener, times(1)).onAlertMessageDisplayed(errorMessage);
    }
}
