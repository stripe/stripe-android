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

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.ApiResultCallback;
import com.stripe.android.CustomerSession;
import com.stripe.android.CustomerSessionTestHelper;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.R;
import com.stripe.android.Stripe;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodCreateParams;
import com.stripe.android.model.PaymentMethodTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity;

import java.util.Calendar;

import static android.app.Activity.RESULT_OK;
import static com.stripe.android.CustomerSession.ACTION_API_EXCEPTION;
import static com.stripe.android.CustomerSession.EXTRA_EXCEPTION;
import static com.stripe.android.PaymentSession.EXTRA_PAYMENT_SESSION_ACTIVE;
import static com.stripe.android.PaymentSession.TOKEN_PAYMENT_SESSION;
import static com.stripe.android.view.AddPaymentMethodActivity.EXTRA_PROXY_DELAY;
import static com.stripe.android.view.AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY;
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
 * Test class for {@link AddPaymentMethodActivity}.
 */
@RunWith(RobolectricTestRunner.class)
public class AddPaymentMethodActivityTest extends BaseViewTest<AddPaymentMethodActivity> {

    private CardMultilineWidget mCardMultilineWidget;
    private CardMultilineWidgetTest.WidgetControlGroup mWidgetControlGroup;
    private ProgressBar mProgressBar;
    private AddPaymentMethodActivity mActivity;
    private ShadowActivity mShadowActivity;
    private ArgumentCaptor<PaymentMethodCreateParams> mParamsArgumentCaptor;
    private ArgumentCaptor<ApiResultCallback<PaymentMethod>> mCallbackArgumentCaptor;


    @Mock private Stripe mStripe;
    @Mock private CustomerSession mCustomerSession;

    public AddPaymentMethodActivityTest() {
        super(AddPaymentMethodActivity.class);
    }

    @Before
    public void setup() {
        // The input in this test class will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);
        MockitoAnnotations.initMocks(this);
        CustomerSessionTestHelper.setInstance(mCustomerSession);

        mParamsArgumentCaptor = ArgumentCaptor.forClass(PaymentMethodCreateParams.class);
        mCallbackArgumentCaptor = ArgumentCaptor.forClass(ApiResultCallback.class);
    }

    @After
    @Override
    public void tearDown() {
        super.tearDown();
    }

    private void setUpForLocalTest() {
        mActivity = createActivity();
        mCardMultilineWidget = mActivity.findViewById(R.id.add_source_card_entry_widget);
        mProgressBar = mActivity.findViewById(R.id.progress_bar_as);
        mWidgetControlGroup = new CardMultilineWidgetTest.WidgetControlGroup(mCardMultilineWidget);

        mShadowActivity = shadowOf(mActivity);
        mActivity.setStripeProvider(new AddPaymentMethodActivity.StripeProvider() {
            @NonNull
            @Override
            public Stripe getStripe(@NonNull Context context) {
                return mStripe;
            }
        });
    }

    private void setUpForProxySessionTest() {
        final Intent intent = AddPaymentMethodActivity
                .newIntent(ApplicationProvider.getApplicationContext(), true, true)
                .putExtra(EXTRA_PROXY_DELAY, true)
                .putExtra(EXTRA_PAYMENT_SESSION_ACTIVE, true);
        mActivity = createActivity(intent);
        mCardMultilineWidget = mActivity.findViewById(R.id.add_source_card_entry_widget);
        mProgressBar = mActivity.findViewById(R.id.progress_bar_as);
        mWidgetControlGroup = new CardMultilineWidgetTest.WidgetControlGroup(mCardMultilineWidget);

        mShadowActivity = shadowOf(mActivity);
        mActivity.setStripeProvider(new AddPaymentMethodActivity.StripeProvider() {
            @NonNull
            @Override
            public Stripe getStripe(@NonNull Context context) {
                return mStripe;
            }
        });
        mActivity.initCustomerSessionTokens();
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

        // Note: these values do not match what is being mock-sent back in the result.
        mWidgetControlGroup.cardNumberEditText.append(CardInputTestActivity.VALID_AMEX_NO_SPACES);
        mWidgetControlGroup.expiryDateEditText.append("12");
        mWidgetControlGroup.expiryDateEditText.append("50");
        mWidgetControlGroup.cvcEditText.append("1234");

        PaymentConfiguration.init("pk_test_abc123");
        final MenuItem menuItem = mock(MenuItem.class);
        when(menuItem.getItemId()).thenReturn(R.id.action_save);

        assertEquals(View.GONE, mProgressBar.getVisibility());

        mWidgetControlGroup.cvcEditText.onEditorAction(EditorInfo.IME_ACTION_DONE);
        verify(mStripe).createPaymentMethod(
                mParamsArgumentCaptor.capture(),
                mCallbackArgumentCaptor.capture());
        final PaymentMethodCreateParams params = mParamsArgumentCaptor.getValue();
        final ApiResultCallback<PaymentMethod> callback = mCallbackArgumentCaptor.getValue();
        assertNotNull(params);
        assertNotNull(callback);

        assertEquals(View.VISIBLE, mProgressBar.getVisibility());

        final PaymentMethod expectedPaymentMethod =
                PaymentMethod.fromString(PaymentMethodTest.RAW_CARD_JSON);
        assertNotNull(expectedPaymentMethod);
        callback.onSuccess(expectedPaymentMethod);
        assertEquals(RESULT_OK, mShadowActivity.getResultCode());
        Intent intent = mShadowActivity.getResultIntent();

        assertTrue(mActivity.isFinishing());
        assertTrue(intent.hasExtra(AddPaymentMethodActivity.EXTRA_NEW_PAYMENT_METHOD));
        final PaymentMethod newPaymentMethod =
                PaymentMethod.fromString(
                        intent.getStringExtra(AddPaymentMethodActivity.EXTRA_NEW_PAYMENT_METHOD));
        assertNotNull(newPaymentMethod);
        assertEquals(expectedPaymentMethod, newPaymentMethod);
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
        final MenuItem menuItem = mock(MenuItem.class);
        when(menuItem.getItemId()).thenReturn(R.id.action_save);

        assertEquals(View.GONE, mProgressBar.getVisibility());

        mWidgetControlGroup.cvcEditText.onEditorAction(EditorInfo.IME_ACTION_DONE);
        verify(mStripe, never()).createPaymentMethod(
                any(PaymentMethodCreateParams.class),
                any(ApiResultCallback.class));
    }

    @Test
    public void addCardData_whenDataIsValidAndServerReturnsSuccess_finishesWithIntent() {
        setUpForLocalTest();

        // Note: these values do not match what is being mock-sent back in the result.
        mWidgetControlGroup.cardNumberEditText.append(CardInputTestActivity.VALID_AMEX_NO_SPACES);
        mWidgetControlGroup.expiryDateEditText.append("12");
        mWidgetControlGroup.expiryDateEditText.append("50");
        mWidgetControlGroup.cvcEditText.append("1234");

        PaymentConfiguration.init("pk_test_abc123");
        final MenuItem menuItem = mock(MenuItem.class);
        when(menuItem.getItemId()).thenReturn(R.id.action_save);

        assertEquals(View.GONE, mProgressBar.getVisibility());

        mActivity.onOptionsItemSelected(menuItem);
        verify(mStripe).createPaymentMethod(
                mParamsArgumentCaptor.capture(),
                mCallbackArgumentCaptor.capture());
        final PaymentMethodCreateParams params = mParamsArgumentCaptor.getValue();
        final ApiResultCallback<PaymentMethod> callback = mCallbackArgumentCaptor.getValue();
        assertNotNull(params);
        assertNotNull(callback);

        assertEquals(View.VISIBLE, mProgressBar.getVisibility());

        final PaymentMethod expectedPaymentMethod =
                PaymentMethod.fromString(PaymentMethodTest.RAW_CARD_JSON);
        assertNotNull(expectedPaymentMethod);
        callback.onSuccess(expectedPaymentMethod);
        assertEquals(RESULT_OK, mShadowActivity.getResultCode());
        final Intent intent = mShadowActivity.getResultIntent();

        assertTrue(mActivity.isFinishing());
        assertTrue(intent.hasExtra(AddPaymentMethodActivity.EXTRA_NEW_PAYMENT_METHOD));
        final PaymentMethod newPaymentMethod =
                PaymentMethod.fromString(
                        intent.getStringExtra(AddPaymentMethodActivity.EXTRA_NEW_PAYMENT_METHOD));
        assertNotNull(newPaymentMethod);
        assertEquals(expectedPaymentMethod, newPaymentMethod);
    }

    @Test
    public void addCardData_whenServerReturnsSuccessAndUpdatesCustomer_finishesWithIntent() {
        setUpForProxySessionTest();

        // Note: these values do not match what is being mock-sent back in the result.
        mWidgetControlGroup.cardNumberEditText.append(CardInputTestActivity.VALID_AMEX_NO_SPACES);
        mWidgetControlGroup.expiryDateEditText.append("12");
        mWidgetControlGroup.expiryDateEditText.append("50");
        mWidgetControlGroup.cvcEditText.append("1234");
        mWidgetControlGroup.postalCodeEditText.append("90210");

        PaymentConfiguration.init("pk_test_abc123");
        final MenuItem menuItem = mock(MenuItem.class);
        when(menuItem.getItemId()).thenReturn(R.id.action_save);

        assertEquals(View.GONE, mProgressBar.getVisibility());
        assertTrue(mCardMultilineWidget.isEnabled());

        mActivity.onOptionsItemSelected(menuItem);
        verify(mStripe).createPaymentMethod(
                mParamsArgumentCaptor.capture(),
                mCallbackArgumentCaptor.capture());
        final PaymentMethodCreateParams params = mParamsArgumentCaptor.getValue();
        final ApiResultCallback<PaymentMethod> callback = mCallbackArgumentCaptor.getValue();
        assertNotNull(params);
        assertNotNull(callback);

        assertEquals(View.VISIBLE, mProgressBar.getVisibility());
        assertFalse(mCardMultilineWidget.isEnabled());

        final PaymentMethod expectedPaymentMethod =
                PaymentMethod.fromString(PaymentMethodTest.RAW_CARD_JSON);
        assertNotNull(expectedPaymentMethod);
        callback.onSuccess(expectedPaymentMethod);

        final ArgumentCaptor<String> paymentMethodIdCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<CustomerSession.PaymentMethodRetrievalListener> listenerArgumentCaptor =
                ArgumentCaptor.forClass(CustomerSession.PaymentMethodRetrievalListener.class);
        verify(mCustomerSession).addProductUsageTokenIfValid(TOKEN_ADD_PAYMENT_METHOD_ACTIVITY);
        verify(mCustomerSession).addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION);
        verify(mCustomerSession).attachPaymentMethod(
                paymentMethodIdCaptor.capture(),
                listenerArgumentCaptor.capture());

        assertEquals(expectedPaymentMethod.id, paymentMethodIdCaptor.getValue());
        final CustomerSession.PaymentMethodRetrievalListener listener =
                listenerArgumentCaptor.getValue();
        assertNotNull(listener);

        listener.onPaymentMethodRetrieved(expectedPaymentMethod);

        assertEquals(RESULT_OK, mShadowActivity.getResultCode());
        final Intent intent = mShadowActivity.getResultIntent();

        assertTrue(mActivity.isFinishing());
        assertTrue(intent.hasExtra(AddPaymentMethodActivity.EXTRA_NEW_PAYMENT_METHOD));
        final PaymentMethod paymentMethod =
                PaymentMethod.fromString(
                        intent.getStringExtra(AddPaymentMethodActivity.EXTRA_NEW_PAYMENT_METHOD));
        assertNotNull(paymentMethod);
        assertEquals(expectedPaymentMethod, paymentMethod);
    }

    @Test
    public void addCardData_whenDataIsValidButServerReturnsError_doesNotFinish() {
        setUpForLocalTest();

        final StripeActivity.AlertMessageListener alertMessageListener =
                Mockito.mock(StripeActivity.AlertMessageListener.class);
        mActivity.setAlertMessageListener(alertMessageListener);

        // Note: these values do not match what is being mock-sent back in the result.
        mWidgetControlGroup.cardNumberEditText.append(CardInputTestActivity.VALID_AMEX_NO_SPACES);
        mWidgetControlGroup.expiryDateEditText.append("12");
        mWidgetControlGroup.expiryDateEditText.append("50");
        mWidgetControlGroup.cvcEditText.append("1234");

        PaymentConfiguration.init("pk_test_abc123");
        final MenuItem menuItem = mock(MenuItem.class);
        when(menuItem.getItemId()).thenReturn(R.id.action_save);

        assertEquals(View.GONE, mProgressBar.getVisibility());

        mActivity.onOptionsItemSelected(menuItem);
        verify(mStripe).createPaymentMethod(
                mParamsArgumentCaptor.capture(),
                mCallbackArgumentCaptor.capture());
        final PaymentMethodCreateParams params = mParamsArgumentCaptor.getValue();
        final ApiResultCallback<PaymentMethod> callback = mCallbackArgumentCaptor.getValue();
        assertNotNull(params);
        assertNotNull(callback);

        assertEquals(View.VISIBLE, mProgressBar.getVisibility());

        final StripeException error = mock(StripeException.class);
        final String errorMessage = "Oh no! An Error!";
        when(error.getLocalizedMessage()).thenReturn(errorMessage);
        callback.onError(error);

        final Intent intent = mShadowActivity.getResultIntent();
        assertNull(intent);
        assertFalse(mActivity.isFinishing());
        assertEquals(View.GONE, mProgressBar.getVisibility());
        verify(alertMessageListener, times(1)).onAlertMessageDisplayed(errorMessage);
    }

    @Test
    public void addCardData_whenPaymentMethodCreationWorksButAddToCustomerFails_showsErrorNotFinish() {
        setUpForProxySessionTest();
        final StripeActivity.AlertMessageListener alertMessageListener =
                Mockito.mock(StripeActivity.AlertMessageListener.class);
        mActivity.setAlertMessageListener(alertMessageListener);

        // Note: these values do not match what is being mock-sent back in the result.
        mWidgetControlGroup.cardNumberEditText.append(CardInputTestActivity.VALID_AMEX_NO_SPACES);
        mWidgetControlGroup.expiryDateEditText.append("12");
        mWidgetControlGroup.expiryDateEditText.append("50");
        mWidgetControlGroup.cvcEditText.append("1234");
        mWidgetControlGroup.postalCodeEditText.append("90210");

        PaymentConfiguration.init("pk_test_abc123");
        final MenuItem menuItem = mock(MenuItem.class);
        when(menuItem.getItemId()).thenReturn(R.id.action_save);

        assertEquals(View.GONE, mProgressBar.getVisibility());
        assertTrue(mCardMultilineWidget.isEnabled());

        mActivity.onOptionsItemSelected(menuItem);
        verify(mStripe).createPaymentMethod(
                mParamsArgumentCaptor.capture(),
                mCallbackArgumentCaptor.capture());
        final PaymentMethodCreateParams params = mParamsArgumentCaptor.getValue();
        final ApiResultCallback<PaymentMethod> callback = mCallbackArgumentCaptor.getValue();
        assertNotNull(params);
        assertNotNull(callback);

        assertEquals(View.VISIBLE, mProgressBar.getVisibility());
        assertFalse(mCardMultilineWidget.isEnabled());

        final PaymentMethod expectedPaymentMethod =
                PaymentMethod.fromString(PaymentMethodTest.RAW_CARD_JSON);
        assertNotNull(expectedPaymentMethod);
        callback.onSuccess(expectedPaymentMethod);

        final ArgumentCaptor<String> paymentMethodIdCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<CustomerSession.PaymentMethodRetrievalListener> listenerArgumentCaptor =
                ArgumentCaptor.forClass(CustomerSession.PaymentMethodRetrievalListener.class);
        verify(mCustomerSession).addProductUsageTokenIfValid(TOKEN_ADD_PAYMENT_METHOD_ACTIVITY);
        verify(mCustomerSession).addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION);
        verify(mCustomerSession).attachPaymentMethod(
                paymentMethodIdCaptor.capture(),
                listenerArgumentCaptor.capture());

        assertEquals(expectedPaymentMethod.id, paymentMethodIdCaptor.getValue());
        CustomerSession.PaymentMethodRetrievalListener listener = listenerArgumentCaptor.getValue();
        assertNotNull(listener);

        final StripeException error = mock(StripeException.class);
        final String errorMessage = "Oh no! An Error!";
        when(error.getLocalizedMessage()).thenReturn(errorMessage);
        listener.onError(400, errorMessage, null);

        // We're mocking the CustomerSession, so we have to replicate its broadcast behavior.
        final Bundle bundle = new Bundle();
        bundle.putSerializable(EXTRA_EXCEPTION, error);
        final Intent errorIntent = new Intent(ACTION_API_EXCEPTION);
        errorIntent.putExtras(bundle);
        LocalBroadcastManager.getInstance(mActivity).sendBroadcast(errorIntent);

        final Intent intent = mShadowActivity.getResultIntent();
        assertNull(intent);
        assertFalse(mActivity.isFinishing());
        assertEquals(View.GONE, mProgressBar.getVisibility());
        verify(alertMessageListener, times(1)).onAlertMessageDisplayed(errorMessage);
    }
}
