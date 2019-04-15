package com.stripe.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.APIException;
import com.stripe.android.exception.AuthenticationException;
import com.stripe.android.exception.CardException;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.model.Customer;
import com.stripe.android.model.Source;
import com.stripe.android.testharness.TestEphemeralKeyProvider;
import com.stripe.android.view.CardInputTestActivity;
import com.stripe.android.view.PaymentMethodsActivity;

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
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;

import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link PaymentSession}
 */
@RunWith(RobolectricTestRunner.class)
public class PaymentSessionTest {

    private TestEphemeralKeyProvider mEphemeralKeyProvider;
    private Activity mActivity;

    @Mock private StripeApiHandler mApiHandler;
    @Mock private ThreadPoolExecutor mThreadPoolExecutor;
    @Mock private PaymentSession.PaymentSessionListener mPaymentSessionListener;

    @Captor private ArgumentCaptor<PaymentSessionData> mPaymentSessionDataArgumentCaptor;

    @Before
    public void setup()
            throws CardException, APIException, InvalidRequestException, AuthenticationException,
            APIConnectionException {
        MockitoAnnotations.initMocks(this);

        PaymentConfiguration.init("pk_test_abc123");

        mEphemeralKeyProvider = new TestEphemeralKeyProvider();
        mActivity = Robolectric.buildActivity(AppCompatActivity.class).create().start().get();

        final Customer firstCustomer = Customer.fromString(FIRST_TEST_CUSTOMER_OBJECT);
        assertNotNull(firstCustomer);
        final Customer secondCustomer = Customer.fromString(SECOND_TEST_CUSTOMER_OBJECT);
        assertNotNull(secondCustomer);

        final Source addedSource =
                Source.fromString(CardInputTestActivity.EXAMPLE_JSON_CARD_SOURCE);
        assertNotNull(addedSource);

        when(mApiHandler.retrieveCustomer(anyString(), anyString()))
                .thenReturn(firstCustomer, secondCustomer);
        when(mApiHandler.addCustomerSource(
                anyString(),
                anyString(),
                ArgumentMatchers.<String>anyList(),
                anyString(),
                anyString(),
                anyString(),
                ArgumentMatchers.<StripeApiHandler.LoggingResponseListener>isNull()))
                .thenReturn(addedSource);
        when(mApiHandler.setDefaultCustomerSource(
                anyString(),
                anyString(),
                ArgumentMatchers.<String>anyList(),
                anyString(),
                anyString(),
                anyString(),
                ArgumentMatchers.<StripeApiHandler.LoggingResponseListener>isNull()))
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
        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        CustomerSession.setInstance(createCustomerSession());

        PaymentSession paymentSession = new PaymentSession(mActivity);
        paymentSession.init(mPaymentSessionListener, new PaymentSessionConfig.Builder().build());

        // We have already tested the functionality up to here.
        reset(mPaymentSessionListener);

        boolean handled = paymentSession.handlePaymentData(
                PaymentSession.PAYMENT_METHOD_REQUEST, RESULT_OK, new Intent());

        assertTrue(handled);
        verify(mPaymentSessionListener).onPaymentSessionDataChanged(any(PaymentSessionData.class));
    }

    @Test
    public void selectPaymentMethod_launchesPaymentMethodsActivityWithLog() {
        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        CustomerSession.setInstance(createCustomerSession());

        PaymentSession paymentSession = new PaymentSession(mActivity);
        paymentSession.init(mPaymentSessionListener, new PaymentSessionConfig.Builder().build());

        paymentSession.presentPaymentMethodSelection();

        ShadowActivity.IntentForResult intentForResult =
                Shadows.shadowOf(mActivity).getNextStartedActivityForResult();
        assertNotNull(intentForResult);
        assertNotNull(intentForResult.intent.getComponent());
        assertEquals(PaymentMethodsActivity.class.getName(),
                intentForResult.intent.getComponent().getClassName());
        assertTrue(intentForResult.intent.hasExtra(EXTRA_PAYMENT_SESSION_ACTIVE));
    }

    @Test
    public void init_withoutSavedState_clearsLoggingTokensAndStartsWithPaymentSession() {
        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        final CustomerSession customerSession = createCustomerSession();
        CustomerSession.setInstance(customerSession);
        customerSession.addProductUsageTokenIfValid("PaymentMethodsActivity");
        assertEquals(1, customerSession.getProductUsageTokens().size());

        PaymentSession paymentSession = new PaymentSession(mActivity);
        paymentSession.init(mPaymentSessionListener, new PaymentSessionConfig.Builder().build());

        // The init removes PaymentMethodsActivity, but then adds PaymentSession
        final Set<String> loggingTokens = customerSession.getProductUsageTokens();
        assertEquals(1, loggingTokens.size());
        assertFalse(loggingTokens.contains("PaymentMethodsActivity"));
        assertTrue(loggingTokens.contains("PaymentSession"));
    }

    @Test
    public void init_withSavedStateBundle_doesNotClearLoggingTokens() {
        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        final CustomerSession customerSession = createCustomerSession();
        CustomerSession.setInstance(customerSession);
        customerSession.addProductUsageTokenIfValid("PaymentMethodsActivity");
        assertEquals(1, customerSession.getProductUsageTokens().size());

        PaymentSession paymentSession = new PaymentSession(mActivity);
        // If it is given any saved state at all, the tokens are not cleared out.
        paymentSession.init(mPaymentSessionListener,
                new PaymentSessionConfig.Builder().build(), new Bundle());

        final Set<String> loggingTokens = customerSession.getProductUsageTokens();
        assertEquals(2, loggingTokens.size());
        assertTrue(loggingTokens.contains("PaymentMethodsActivity"));
        assertTrue(loggingTokens.contains("PaymentSession"));
    }

    @Test
    public void completePayment_withLoggedActions_clearsLoggingTokensAndSetsResult() {
        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        final CustomerSession customerSession = createCustomerSession();
        CustomerSession.setInstance(customerSession);
        customerSession.addProductUsageTokenIfValid("PaymentMethodsActivity");
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
        assertEquals(firstPaymentSessionData.getSelectedPaymentMethodId(),
                secondPaymentSessionData.getSelectedPaymentMethodId());
    }

    @NonNull
    private CustomerSession createCustomerSession() {
        return new CustomerSession(ApplicationProvider.getApplicationContext(),
                mEphemeralKeyProvider, null, mThreadPoolExecutor, mApiHandler);
    }
}
