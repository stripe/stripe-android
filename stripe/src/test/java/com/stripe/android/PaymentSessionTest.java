package com.stripe.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.APIException;
import com.stripe.android.exception.AuthenticationException;
import com.stripe.android.exception.CardException;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.model.Customer;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodCreateParams;
import com.stripe.android.model.PaymentMethodTest;
import com.stripe.android.testharness.TestEphemeralKeyProvider;
import com.stripe.android.view.AddPaymentMethodActivity;
import com.stripe.android.view.PaymentMethodsActivity;
import com.stripe.android.view.PaymentMethodsActivityStarter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.stripe.android.CustomerSessionTest.FIRST_SAMPLE_KEY_RAW;
import static com.stripe.android.CustomerSessionTest.FIRST_TEST_CUSTOMER_OBJECT;
import static com.stripe.android.CustomerSessionTest.SECOND_TEST_CUSTOMER_OBJECT;
import static com.stripe.android.PaymentSession.EXTRA_PAYMENT_SESSION_ACTIVE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link PaymentSession}
 */
@RunWith(RobolectricTestRunner.class)
public class PaymentSessionTest {

    private TestEphemeralKeyProvider mEphemeralKeyProvider;

    @Mock private Activity mActivity;
    @Mock private StripeApiHandler mApiHandler;
    @Mock private ThreadPoolExecutor mThreadPoolExecutor;
    @Mock private PaymentSession.PaymentSessionListener mPaymentSessionListener;
    @Mock private CustomerSession mCustomerSession;
    @Mock private PaymentMethodsActivityStarter mPaymentMethodsActivityStarter;

    @Captor private ArgumentCaptor<PaymentSessionData> mPaymentSessionDataArgumentCaptor;
    @Captor private ArgumentCaptor<Intent> mIntentArgumentCaptor;

    @Before
    public void setup()
            throws CardException, APIException, InvalidRequestException, AuthenticationException,
            APIConnectionException {
        MockitoAnnotations.initMocks(this);

        PaymentConfiguration.init(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY);

        mEphemeralKeyProvider = new TestEphemeralKeyProvider();

        final Customer firstCustomer = Customer.fromString(FIRST_TEST_CUSTOMER_OBJECT);
        assertNotNull(firstCustomer);
        final Customer secondCustomer = Customer.fromString(SECOND_TEST_CUSTOMER_OBJECT);
        assertNotNull(secondCustomer);

        final PaymentMethod paymentMethod =
                PaymentMethod.fromString(PaymentMethodTest.RAW_CARD_JSON);
        assertNotNull(paymentMethod);

        when(mApiHandler.retrieveCustomer(anyString(), anyString()))
                .thenReturn(firstCustomer, secondCustomer);
        when(mApiHandler.createPaymentMethod(
                ArgumentMatchers.<PaymentMethodCreateParams>any(),
                ArgumentMatchers.<RequestOptions>any()
        )).thenReturn(paymentMethod);
        when(mApiHandler.setDefaultCustomerSource(
                anyString(),
                anyString(),
                ArgumentMatchers.<String>anyList(),
                anyString(),
                anyString(),
                anyString()))
                .thenReturn(secondCustomer);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                invocation.<Runnable>getArgument(0).run();
                return null;
            }
        }).when(mThreadPoolExecutor).execute(any(Runnable.class));
    }

    @Test
    public void init_addsPaymentSessionToken_andFetchesCustomer() {
        final CustomerSession customerSession = createCustomerSession();
        CustomerSession.setInstance(customerSession);

        final PaymentSession paymentSession = new PaymentSession(mActivity);
        paymentSession.init(mPaymentSessionListener, new PaymentSessionConfig.Builder().build());

        assertTrue(customerSession.getProductUsageTokens()
                .contains(PaymentSession.TOKEN_PAYMENT_SESSION));

        verify(mPaymentSessionListener).onCommunicatingStateChanged(eq(true));
    }

    @Test
    public void init_whenEphemeralKeyProviderContinues_fetchesCustomerAndNotifiesListener() {
        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        CustomerSession.setInstance(createCustomerSession());

        final PaymentSession paymentSession = new PaymentSession(mActivity);
        paymentSession.init(mPaymentSessionListener, new PaymentSessionConfig.Builder().build());
        verify(mPaymentSessionListener).onCommunicatingStateChanged(eq(true));
        verify(mPaymentSessionListener).onPaymentSessionDataChanged(any(PaymentSessionData.class));
        verify(mPaymentSessionListener).onCommunicatingStateChanged(eq(false));
    }

    @Test
    public void setCartTotal_setsExpectedValueAndNotifiesListener() {
        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        CustomerSession.setInstance(createCustomerSession());

        final PaymentSession paymentSession = new PaymentSession(mActivity);
        paymentSession.init(mPaymentSessionListener, new PaymentSessionConfig.Builder().build());
        paymentSession.setCartTotal(500L);

        verify(mPaymentSessionListener)
                .onPaymentSessionDataChanged(mPaymentSessionDataArgumentCaptor.capture());
        final PaymentSessionData data = mPaymentSessionDataArgumentCaptor.getValue();
        assertNotNull(data);
        assertEquals(500L, data.getCartTotal());
    }

    @Test
    public void handlePaymentData_whenPaymentMethodRequest_notifiesListenerAndFetchesCustomer() {
        CustomerSession.setInstance(createCustomerSession());

        PaymentSession paymentSession = new PaymentSession(mActivity);
        paymentSession.init(mPaymentSessionListener, new PaymentSessionConfig.Builder().build());

        // We have already tested the functionality up to here.
        reset(mPaymentSessionListener);

        boolean handled = paymentSession.handlePaymentData(
                PaymentSession.PAYMENT_METHOD_REQUEST, RESULT_OK,
                new Intent().putExtra(PaymentMethodsActivity.EXTRA_SELECTED_PAYMENT,
                        PaymentMethod.fromString(PaymentMethodTest.RAW_CARD_JSON)));

        assertTrue(handled);

        verify(mPaymentSessionListener)
                .onPaymentSessionDataChanged(mPaymentSessionDataArgumentCaptor.capture());
        final PaymentSessionData data = mPaymentSessionDataArgumentCaptor.getValue();
        assertNotNull(data);
        assertEquals(PaymentMethod.fromString(PaymentMethodTest.RAW_CARD_JSON),
                data.getPaymentMethod());
    }

    @Test
    public void selectPaymentMethod_launchesPaymentMethodsActivityWithLog() {
        CustomerSession.setInstance(createCustomerSession());

        final PaymentSession paymentSession = new PaymentSession(mActivity);
        paymentSession.init(mPaymentSessionListener, new PaymentSessionConfig.Builder().build());

        paymentSession.presentPaymentMethodSelection();

        verify(mActivity).startActivityForResult(mIntentArgumentCaptor.capture(),
                eq(PaymentSession.PAYMENT_METHOD_REQUEST));

        final Intent intent = mIntentArgumentCaptor.getValue();
        assertNotNull(intent.getComponent());
        assertEquals(PaymentMethodsActivity.class.getName(),
                intent.getComponent().getClassName());
        assertTrue(intent.hasExtra(EXTRA_PAYMENT_SESSION_ACTIVE));
        assertFalse(
                intent.getBooleanExtra(AddPaymentMethodActivity.EXTRA_SHOULD_REQUIRE_POSTAL_CODE,
                        false));
    }

    @Test
    public void presentPaymentMethodSelection_withShouldRequirePostalCode_shouldPassInIntent() {
        CustomerSession.setInstance(createCustomerSession());

        final PaymentSession paymentSession = new PaymentSession(mActivity);
        paymentSession.init(mPaymentSessionListener, new PaymentSessionConfig.Builder().build());
        paymentSession.presentPaymentMethodSelection(true);

        verify(mActivity).startActivityForResult(mIntentArgumentCaptor.capture(),
                eq(PaymentSession.PAYMENT_METHOD_REQUEST));

        final Intent intent = mIntentArgumentCaptor.getValue();
        assertTrue(
                intent.getBooleanExtra(AddPaymentMethodActivity.EXTRA_SHOULD_REQUIRE_POSTAL_CODE,
                        false));
    }

    @Test
    public void init_withoutSavedState_clearsLoggingTokensAndStartsWithPaymentSession() {
        final CustomerSession customerSession = createCustomerSession();
        CustomerSession.setInstance(customerSession);
        customerSession
                .addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY);
        assertEquals(1, customerSession.getProductUsageTokens().size());

        PaymentSession paymentSession = new PaymentSession(mActivity);
        paymentSession.init(mPaymentSessionListener, new PaymentSessionConfig.Builder().build());

        // The init removes PaymentMethodsActivity, but then adds PaymentSession
        final Set<String> loggingTokens = customerSession.getProductUsageTokens();
        assertEquals(1, loggingTokens.size());
        assertFalse(loggingTokens.contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY));
        assertTrue(loggingTokens.contains(PaymentSession.TOKEN_PAYMENT_SESSION));
    }

    @Test
    public void init_withSavedStateBundle_doesNotClearLoggingTokens() {
        final CustomerSession customerSession = createCustomerSession();
        CustomerSession.setInstance(customerSession);
        customerSession
                .addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY);
        assertEquals(1, customerSession.getProductUsageTokens().size());

        PaymentSession paymentSession = new PaymentSession(mActivity);
        // If it is given any saved state at all, the tokens are not cleared out.
        paymentSession.init(mPaymentSessionListener,
                new PaymentSessionConfig.Builder().build(), new Bundle());

        final Set<String> loggingTokens = customerSession.getProductUsageTokens();
        assertEquals(2, loggingTokens.size());
        assertTrue(loggingTokens.contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY));
        assertTrue(loggingTokens.contains(PaymentSession.TOKEN_PAYMENT_SESSION));
    }

    @Test
    public void completePayment_withLoggedActions_clearsLoggingTokensAndSetsResult() {
        final CustomerSession customerSession = createCustomerSession();
        CustomerSession.setInstance(customerSession);
        customerSession
                .addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY);
        assertEquals(1, customerSession.getProductUsageTokens().size());

        PaymentSession paymentSession = new PaymentSession(mActivity);
        // If it is given any saved state at all, the tokens are not cleared out.
        paymentSession.init(mPaymentSessionListener,
                new PaymentSessionConfig.Builder().build(), new Bundle());

        final Set<String> loggingTokens = customerSession.getProductUsageTokens();
        assertEquals(2, loggingTokens.size());

        reset(mPaymentSessionListener);
        paymentSession.completePayment(new PaymentCompletionProvider() {
            @Override
            public void completePayment(@NonNull PaymentSessionData data,
                                        @NonNull PaymentResultListener listener) {
                listener.onPaymentResult(PaymentResultListener.SUCCESS);
            }
        });

        ArgumentCaptor<PaymentSessionData> dataArgumentCaptor =
                ArgumentCaptor.forClass(PaymentSessionData.class);
        verify(mPaymentSessionListener).onPaymentSessionDataChanged(dataArgumentCaptor.capture());
        PaymentSessionData capturedData = dataArgumentCaptor.getValue();
        assertNotNull(capturedData);
        assertEquals(PaymentResultListener.SUCCESS, capturedData.getPaymentResult());
        assertTrue(customerSession.getProductUsageTokens().isEmpty());
    }

    @Test
    public void init_withSavedState_setsPaymentSessionData() {
        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        CustomerSession.setInstance(createCustomerSession());

        PaymentSession paymentSession = new PaymentSession(mActivity);
        paymentSession.init(mPaymentSessionListener, new PaymentSessionConfig.Builder().build());

        paymentSession.setCartTotal(300L);

        verify(mPaymentSessionListener)
                .onPaymentSessionDataChanged(mPaymentSessionDataArgumentCaptor.capture());
        final Bundle bundle = new Bundle();
        paymentSession.savePaymentSessionInstanceState(bundle);
        PaymentSessionData firstPaymentSessionData = mPaymentSessionDataArgumentCaptor.getValue();

        PaymentSession.PaymentSessionListener secondListener =
                mock(PaymentSession.PaymentSessionListener.class);

        paymentSession.init(secondListener, new PaymentSessionConfig.Builder().build(), bundle);
        verify(secondListener)
                .onPaymentSessionDataChanged(mPaymentSessionDataArgumentCaptor.capture());

        final PaymentSessionData secondPaymentSessionData =
                mPaymentSessionDataArgumentCaptor.getValue();
        assertEquals(firstPaymentSessionData.getCartTotal(),
                secondPaymentSessionData.getCartTotal());
        assertEquals(firstPaymentSessionData.getPaymentMethod(),
                secondPaymentSessionData.getPaymentMethod());
    }

    @Test
    public void handlePaymentData_withInvalidRequestCode_aborts() {
        final PaymentSession paymentSession = new PaymentSession(mActivity, mCustomerSession,
                mPaymentMethodsActivityStarter, new PaymentSessionData());
        assertFalse(paymentSession.handlePaymentData(-1, RESULT_CANCELED, new Intent()));
        verify(mCustomerSession, never()).retrieveCurrentCustomer(
                ArgumentMatchers.<CustomerSession.CustomerRetrievalListener>any());
    }

    @Test
    public void handlePaymentData_withValidRequestCodeAndCanceledResult_retrievesCustomer() {
        final PaymentSession paymentSession = new PaymentSession(mActivity, mCustomerSession,
                mPaymentMethodsActivityStarter, new PaymentSessionData());
        assertFalse(paymentSession.handlePaymentData(PaymentSession.PAYMENT_METHOD_REQUEST,
                RESULT_CANCELED, new Intent()));
        verify(mCustomerSession).retrieveCurrentCustomer(
                ArgumentMatchers.<CustomerSession.CustomerRetrievalListener>any());
    }

    @NonNull
    private CustomerSession createCustomerSession() {
        return new CustomerSession(ApplicationProvider.getApplicationContext(),
                mEphemeralKeyProvider, null, mThreadPoolExecutor, mApiHandler);
    }
}
