package com.stripe.android.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
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
import com.stripe.android.model.PaymentMethodCreateParamsFixtures;
import com.stripe.android.model.PaymentMethodFixtures;
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
import static org.mockito.ArgumentMatchers.anyString;
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
                .setPaymentMethodType(PaymentMethod.Type.Card)
                .build());
        mCardMultilineWidget = mActivity.findViewById(R.id.add_source_card_entry_widget);
        mProgressBar = mActivity.findViewById(R.id.progress_bar_as);
        mWidgetControlGroup = new CardMultilineWidgetTest.WidgetControlGroup(mCardMultilineWidget);

        mShadowActivity = shadowOf(mActivity);
    }

    private void setUpForProxySessionTest(@NonNull PaymentMethod.Type paymentMethodType) {
        mActivity = createActivity(new AddPaymentMethodActivityStarter.Args.Builder()
                .setShouldAttachToCustomer(true)
                .setShouldRequirePostalCode(true)
                .setIsPaymentSessionActive(true)
                .setShouldInitCustomerSessionTokens(false)
                .setPaymentMethodType(paymentMethodType)
                .setPaymentConfiguration(PaymentConfiguration.getInstance())
                .build());

        mProgressBar = mActivity.findViewById(R.id.progress_bar_as);

        if (PaymentMethod.Type.Card == paymentMethodType) {
            mCardMultilineWidget = mActivity.findViewById(R.id.add_source_card_entry_widget);
            mWidgetControlGroup = new CardMultilineWidgetTest.WidgetControlGroup(mCardMultilineWidget);
        }

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
        setUpForProxySessionTest(PaymentMethod.Type.Card);
        assertNotNull(mCardMultilineWidget);
        assertEquals(View.VISIBLE, mWidgetControlGroup.postalCodeInputLayout.getVisibility());
    }

    @Test
    public void softEnterKey_whenDataIsNotValid_doesNotHideKeyboardAndDoesNotFinish() {
        setUpForLocalTest();
        assertEquals(View.GONE, mProgressBar.getVisibility());
        mActivity.createPaymentMethod(mStripe, null);
        verify(mStripe, never()).createPaymentMethod(
                ArgumentMatchers.<PaymentMethodCreateParams>any(),
                ArgumentMatchers.<ApiResultCallback<PaymentMethod>>any()
        );
    }

    @Test
    public void addCardData_whenDataIsValidAndServerReturnsSuccess_finishesWithIntent() {
        setUpForLocalTest();
        assertEquals(View.GONE, mProgressBar.getVisibility());
        mActivity.createPaymentMethod(mStripe, PaymentMethodCreateParamsFixtures.DEFAULT_CARD);
        verifyFinishesWithIntent();
    }

    @Test
    public void addFpx_whenServerReturnsSuccessAndUpdatesCustomer_finishesWithIntent() {
        setUpForProxySessionTest(PaymentMethod.Type.Fpx);

        assertEquals(View.GONE, mProgressBar.getVisibility());

        mActivity.createPaymentMethod(mStripe, PaymentMethodCreateParamsFixtures.DEFAULT_FPX);
        verify(mStripe).createPaymentMethod(
                mParamsArgumentCaptor.capture(),
                mCallbackArgumentCaptor.capture());
        final PaymentMethodCreateParams params = mParamsArgumentCaptor.getValue();
        final ApiResultCallback<PaymentMethod> callback = mCallbackArgumentCaptor.getValue();
        assertNotNull(params);
        assertNotNull(callback);

        assertEquals(View.VISIBLE, mProgressBar.getVisibility());

        final PaymentMethod expectedPaymentMethod = PaymentMethodFixtures.FPX_PAYMENT_METHOD;
        assertNotNull(expectedPaymentMethod);
        callback.onSuccess(expectedPaymentMethod);

        verify(mCustomerSession).addProductUsageTokenIfValid(TOKEN_ADD_PAYMENT_METHOD_ACTIVITY);
        verify(mCustomerSession).addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION);
        verify(mCustomerSession, never()).attachPaymentMethod(
                anyString(),
                ArgumentMatchers.<CustomerSession.PaymentMethodRetrievalListener>any()
        );

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
    public void addCardData_whenServerReturnsSuccessAndUpdatesCustomer_finishesWithIntent() {
        setUpForProxySessionTest(PaymentMethod.Type.Card);

        assertEquals(View.GONE, mProgressBar.getVisibility());
        assertTrue(mCardMultilineWidget.isEnabled());

        mActivity.createPaymentMethod(mStripe, PaymentMethodCreateParamsFixtures.DEFAULT_CARD);
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

        assertEquals(View.GONE, mProgressBar.getVisibility());

        mActivity.createPaymentMethod(mStripe, PaymentMethodCreateParamsFixtures.DEFAULT_CARD);
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
        setUpForProxySessionTest(PaymentMethod.Type.Card);
        final StripeActivity.AlertMessageListener alertMessageListener =
                Mockito.mock(StripeActivity.AlertMessageListener.class);
        mActivity.setAlertMessageListener(alertMessageListener);

        assertEquals(View.GONE, mProgressBar.getVisibility());
        assertTrue(mCardMultilineWidget.isEnabled());

        mActivity.createPaymentMethod(mStripe, PaymentMethodCreateParamsFixtures.DEFAULT_CARD);
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
                mCallbackArgumentCaptor.capture()
        );
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
