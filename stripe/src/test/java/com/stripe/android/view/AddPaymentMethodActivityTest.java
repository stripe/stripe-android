package com.stripe.android.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;

import com.stripe.android.ApiKeyFixtures;
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
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity;

import java.util.Calendar;

import static android.app.Activity.RESULT_OK;
import static com.stripe.android.CustomerSession.ACTION_API_EXCEPTION;
import static com.stripe.android.CustomerSession.EXTRA_EXCEPTION;
import static com.stripe.android.PaymentSession.TOKEN_PAYMENT_SESSION;
import static com.stripe.android.view.AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    @Captor
    private ArgumentCaptor<PaymentMethodCreateParams> mParamsArgumentCaptor;
    @Captor
    private ArgumentCaptor<ApiResultCallback<PaymentMethod>> mCallbackArgumentCaptor;
    @Captor
    private ArgumentCaptor<String> mPaymentMethodIdCaptor;
    @Captor
    private ArgumentCaptor<CustomerSession.PaymentMethodRetrievalListener> mListenerArgumentCaptor;

    @Mock
    private Stripe mStripe;
    @Mock
    private CustomerSession mCustomerSession;
    @Mock
    private InputMethodManager mInputMethodManager;

    public AddPaymentMethodActivityTest() {
        super(AddPaymentMethodActivity.class);
    }

    @Before
    public void setup() {
        // The input in this test class will be invalid after 2050. Please update the test.
        assertTrue(Calendar.getInstance().get(Calendar.YEAR) < 2050);
        PaymentConfiguration.init(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY);
        MockitoAnnotations.initMocks(this);
        CustomerSessionTestHelper.setInstance(mCustomerSession);
    }

    @After
    @Override
    public void tearDown() {
        super.tearDown();
    }

    private void setUpForLocalTest() {
        mActivity = createActivity(new AddPaymentMethodActivityStarter.Args.Builder()
                .build());
        mCardMultilineWidget = mActivity.findViewById(R.id.add_source_card_entry_widget);
        mProgressBar = mActivity.findViewById(R.id.progress_bar_as);
        mWidgetControlGroup = new CardMultilineWidgetTest.WidgetControlGroup(mCardMultilineWidget);

        mShadowActivity = shadowOf(mActivity);
    }

    private void setUpForProxySessionTest() {
        mActivity = createActivity(new AddPaymentMethodActivityStarter.Args.Builder()
                .setShouldUpdateCustomer(true)
                .setShouldRequirePostalCode(true)
                .setIsPaymentSessionActive(true)
                .setShouldInitCustomerSessionTokens(false)
                .setPaymentMethodType(PaymentMethod.Type.Card)
                .setPaymentConfiguration(PaymentConfiguration.getInstance())
                .build());
        mCardMultilineWidget = mActivity.findViewById(R.id.add_source_card_entry_widget);
        mProgressBar = mActivity.findViewById(R.id.progress_bar_as);
        mWidgetControlGroup = new CardMultilineWidgetTest.WidgetControlGroup(mCardMultilineWidget);

        mShadowActivity = shadowOf(mActivity);
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
    public void softEnterKey_whenDataIsValid_hidesKeyboardAndAttemptsToSave() {
        final AddPaymentMethodActivity activity = mock(AddPaymentMethodActivity.class);
        when(activity.hasValidCard()).thenReturn(true);
        new AddPaymentMethodActivity.OnEditorActionListenerImpl(activity, mInputMethodManager)
                .onEditorAction(null, EditorInfo.IME_ACTION_DONE, null);
        verify(mInputMethodManager).hideSoftInputFromWindow(null, 0);
        verify(activity).onActionSave();
    }

    @Test
    public void softEnterKey_whenDataIsNotValid_doesNotHideKeyboardAndDoesNotFinish() {
        setUpForLocalTest();
        // Note: these values do not match what is being mock-sent back in the result.
        mWidgetControlGroup.cardNumberEditText.append(CardInputTestActivity.VALID_AMEX_NO_SPACES);
        mWidgetControlGroup.expiryDateEditText.append("12");
        mWidgetControlGroup.expiryDateEditText.append("50");
        mWidgetControlGroup.cvcEditText.append("12");


        assertEquals(View.GONE, mProgressBar.getVisibility());

        mActivity.createPaymentMethod(mStripe);
        verify(mStripe, never()).createPaymentMethod(
                any(PaymentMethodCreateParams.class),
                ArgumentMatchers.<ApiResultCallback<PaymentMethod>>any());
    }

    @Test
    public void addCardData_whenDataIsValidAndServerReturnsSuccess_finishesWithIntent() {
        setUpForLocalTest();

        // Note: these values do not match what is being mock-sent back in the result.
        mWidgetControlGroup.cardNumberEditText.append(CardInputTestActivity.VALID_AMEX_NO_SPACES);
        mWidgetControlGroup.expiryDateEditText.append("12");
        mWidgetControlGroup.expiryDateEditText.append("50");
        mWidgetControlGroup.cvcEditText.append("1234");

        assertEquals(View.GONE, mProgressBar.getVisibility());

        mActivity.createPaymentMethod(mStripe);
        verifyFinishesWithIntent();
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

        assertEquals(View.GONE, mProgressBar.getVisibility());
        assertTrue(mCardMultilineWidget.isEnabled());

        mActivity.createPaymentMethod(mStripe);
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
                PaymentMethod.fromString(PaymentMethodTest.PM_CARD_JSON);
        assertNotNull(expectedPaymentMethod);
        callback.onSuccess(expectedPaymentMethod);

        verify(mCustomerSession).addProductUsageTokenIfValid(TOKEN_ADD_PAYMENT_METHOD_ACTIVITY);
        verify(mCustomerSession).addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION);
        verify(mCustomerSession).attachPaymentMethod(
                mPaymentMethodIdCaptor.capture(),
                mListenerArgumentCaptor.capture());

        assertEquals(expectedPaymentMethod.id, mPaymentMethodIdCaptor.getValue());
        final CustomerSession.PaymentMethodRetrievalListener listener =
                mListenerArgumentCaptor.getValue();
        assertNotNull(listener);

        listener.onPaymentMethodRetrieved(expectedPaymentMethod);

        assertEquals(RESULT_OK, mShadowActivity.getResultCode());
        final Intent intent = mShadowActivity.getResultIntent();

        assertTrue(mActivity.isFinishing());
        assertTrue(intent.hasExtra(AddPaymentMethodActivity.EXTRA_NEW_PAYMENT_METHOD));
        final PaymentMethod paymentMethod =
                intent.getParcelableExtra(AddPaymentMethodActivity.EXTRA_NEW_PAYMENT_METHOD);
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

        assertEquals(View.GONE, mProgressBar.getVisibility());

        mActivity.createPaymentMethod(mStripe);
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

        assertNull(mShadowActivity.getResultIntent());
        assertFalse(mActivity.isFinishing());
        assertEquals(View.GONE, mProgressBar.getVisibility());
        verify(alertMessageListener).onAlertMessageDisplayed(errorMessage);
    }

    @Test
    public void addCardData_whenPaymentMethodCreateWorksButAddToCustomerFails_showErrorNotFinish() {
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

        assertEquals(View.GONE, mProgressBar.getVisibility());
        assertTrue(mCardMultilineWidget.isEnabled());

        mActivity.createPaymentMethod(mStripe);
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
                PaymentMethod.fromString(PaymentMethodTest.PM_CARD_JSON);
        assertNotNull(expectedPaymentMethod);
        callback.onSuccess(expectedPaymentMethod);

        verify(mCustomerSession).addProductUsageTokenIfValid(TOKEN_ADD_PAYMENT_METHOD_ACTIVITY);
        verify(mCustomerSession).addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION);
        verify(mCustomerSession).attachPaymentMethod(
                mPaymentMethodIdCaptor.capture(),
                mListenerArgumentCaptor.capture());

        assertEquals(expectedPaymentMethod.id, mPaymentMethodIdCaptor.getValue());
        final CustomerSession.PaymentMethodRetrievalListener listener =
                mListenerArgumentCaptor.getValue();
        assertNotNull(listener);

        final StripeException error = mock(StripeException.class);
        final String errorMessage = "Oh no! An Error!";
        when(error.getLocalizedMessage()).thenReturn(errorMessage);
        listener.onError(400, errorMessage, null);

        // We're mocking the CustomerSession, so we have to replicate its broadcast behavior.
        final Bundle bundle = new Bundle();
        bundle.putSerializable(EXTRA_EXCEPTION, error);
        LocalBroadcastManager.getInstance(mActivity)
                .sendBroadcast(new Intent(ACTION_API_EXCEPTION)
                        .putExtras(bundle));

        final Intent intent = mShadowActivity.getResultIntent();
        assertNull(intent);
        assertFalse(mActivity.isFinishing());
        assertEquals(View.GONE, mProgressBar.getVisibility());
        verify(alertMessageListener).onAlertMessageDisplayed(errorMessage);
    }

    private void verifyFinishesWithIntent() {
        verify(mStripe).createPaymentMethod(
                mParamsArgumentCaptor.capture(),
                mCallbackArgumentCaptor.capture());
        final PaymentMethodCreateParams params = mParamsArgumentCaptor.getValue();
        final ApiResultCallback<PaymentMethod> callback = mCallbackArgumentCaptor.getValue();
        assertNotNull(params);
        assertNotNull(callback);

        assertEquals(View.VISIBLE, mProgressBar.getVisibility());

        final PaymentMethod expectedPaymentMethod =
                PaymentMethod.fromString(PaymentMethodTest.PM_CARD_JSON);
        assertNotNull(expectedPaymentMethod);
        callback.onSuccess(expectedPaymentMethod);
        assertEquals(RESULT_OK, mShadowActivity.getResultCode());
        final Intent intent = mShadowActivity.getResultIntent();

        assertTrue(mActivity.isFinishing());
        assertTrue(intent.hasExtra(AddPaymentMethodActivity.EXTRA_NEW_PAYMENT_METHOD));
        final PaymentMethod newPaymentMethod =
                intent.getParcelableExtra(AddPaymentMethodActivity.EXTRA_NEW_PAYMENT_METHOD);
        assertNotNull(newPaymentMethod);
        assertEquals(expectedPaymentMethod, newPaymentMethod);
    }
}
