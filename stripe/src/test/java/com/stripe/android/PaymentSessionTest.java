package com.stripe.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.stripe.android.exception.StripeException;
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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import java.util.Set;

import static android.app.Activity.RESULT_OK;
import static com.stripe.android.CustomerSessionTest.FIRST_SAMPLE_KEY_RAW;
import static com.stripe.android.CustomerSessionTest.FIRST_TEST_CUSTOMER_OBJECT;
import static com.stripe.android.CustomerSessionTest.SECOND_TEST_CUSTOMER_OBJECT;
import static com.stripe.android.PaymentSession.EXTRA_PAYMENT_SESSION_ACTIVE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link PaymentSession}
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class PaymentSessionTest {

    private TestEphemeralKeyProvider mEphemeralKeyProvider;

    private Customer mFirstCustomer;
    private Customer mSecondCustomer;
    private Source mAddedSource;

    private ActivityController<AppCompatActivity> mActivityController;
    private ShadowActivity mShadowActivity;
    @Mock CustomerSession.StripeApiProxy mStripeApiProxy;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        PaymentConfiguration.init("pk_test_abc123");

        mEphemeralKeyProvider = new TestEphemeralKeyProvider();
        CustomerSession.initCustomerSession(mEphemeralKeyProvider, mStripeApiProxy, null);
        mActivityController =
                Robolectric.buildActivity(AppCompatActivity.class).create().start();
        mShadowActivity = Shadows.shadowOf(mActivityController.get());

        mFirstCustomer = Customer.fromString(FIRST_TEST_CUSTOMER_OBJECT);
        assertNotNull(mFirstCustomer);
        mSecondCustomer = Customer.fromString(SECOND_TEST_CUSTOMER_OBJECT);
        assertNotNull(mSecondCustomer);

        mEphemeralKeyProvider = new TestEphemeralKeyProvider();

        mAddedSource = Source.fromString(CardInputTestActivity.EXAMPLE_JSON_CARD_SOURCE);
        assertNotNull(mAddedSource);

        try {
            when(mStripeApiProxy.retrieveCustomerWithKey(anyString(), anyString()))
                    .thenReturn(mFirstCustomer, mSecondCustomer);
            when(mStripeApiProxy.addCustomerSourceWithKey(
                    any(Context.class),
                    anyString(),
                    anyString(),
                    ArgumentMatchers.<String>anyList(),
                    anyString(),
                    anyString(),
                    anyString()))
                    .thenReturn(mAddedSource);
            when(mStripeApiProxy.setDefaultCustomerSourceWithKey(
                    any(Context.class),
                    anyString(),
                    anyString(),
                    ArgumentMatchers.<String>anyList(),
                    anyString(),
                    anyString(),
                    anyString()))
                    .thenReturn(mSecondCustomer);
        } catch (StripeException exception) {
            fail("Exception when accessing mock api proxy: " + exception.getMessage());
        }
    }

    @Test
    public void init_addsPaymentSessionToken_andFetchesCustomer() {
        PaymentSession.PaymentSessionListener mockListener =
                mock(PaymentSession.PaymentSessionListener.class);
        PaymentSession paymentSession = new PaymentSession(mActivityController.get());
        paymentSession.init(mockListener, new PaymentSessionConfig.Builder().build());

        Set<String> tokenSet = CustomerSession.getInstance().getProductUsageTokens();
        assertTrue(tokenSet.contains(PaymentSession.TOKEN_PAYMENT_SESSION));

        verify(mockListener).onCommunicatingStateChanged(eq(true));
    }

    @Test
    public void init_whenEphemeralKeyProviderContinues_fetchesCustomerAndNotifiesListener() {
        CustomerEphemeralKey firstKey = CustomerEphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);

        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        CustomerSession.initCustomerSession(
                mEphemeralKeyProvider,
                mStripeApiProxy,
                null);

        PaymentSession.PaymentSessionListener mockListener =
                mock(PaymentSession.PaymentSessionListener.class);
        PaymentSession paymentSession = new PaymentSession(mActivityController.get());
        paymentSession.init(mockListener, new PaymentSessionConfig.Builder().build());
        verify(mockListener).onCommunicatingStateChanged(eq(true));
        verify(mockListener).onPaymentSessionDataChanged(any(PaymentSessionData.class));
        verify(mockListener).onCommunicatingStateChanged(eq(false));
    }

    @Test
    public void setCartTotal_setsExpectedValueAndNotifiesListener() {
        CustomerEphemeralKey firstKey = CustomerEphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);

        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        CustomerSession.initCustomerSession(
                mEphemeralKeyProvider,
                mStripeApiProxy,
                null);

        PaymentSession.PaymentSessionListener mockListener =
                mock(PaymentSession.PaymentSessionListener.class);
        PaymentSession paymentSession = new PaymentSession(mActivityController.get());
        paymentSession.init(mockListener, new PaymentSessionConfig.Builder().build());

        ArgumentCaptor<PaymentSessionData> dataArgumentCaptor = getDataCaptor();

        paymentSession.setCartTotal(500L);

        verify(mockListener).onPaymentSessionDataChanged(dataArgumentCaptor.capture());
        PaymentSessionData data = dataArgumentCaptor.getValue();
        assertNotNull(data);
        assertEquals(500L, data.getCartTotal());
    }

    @Test
    public void handlePaymentData_whenPaymentMethodRequest_notifiesListenerAndFetchesCustomer() {
        CustomerEphemeralKey firstKey = CustomerEphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);

        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        CustomerSession.initCustomerSession(
                mEphemeralKeyProvider,
                mStripeApiProxy,
                null);

        PaymentSession.PaymentSessionListener mockListener =
                mock(PaymentSession.PaymentSessionListener.class);
        PaymentSession paymentSession = new PaymentSession(mActivityController.get());
        paymentSession.init(mockListener, new PaymentSessionConfig.Builder().build());

        // We have already tested the functionality up to here.
        reset(mockListener);

        boolean handled = paymentSession.handlePaymentData(
                PaymentSession.PAYMENT_METHOD_REQUEST, RESULT_OK, new Intent());

        assertTrue(handled);
        verify(mockListener).onPaymentSessionDataChanged(any(PaymentSessionData.class));
    }

    @Test
    public void selectPaymentMethod_launchesPaymentMethodsActivityWithLog() {
        CustomerEphemeralKey firstKey = CustomerEphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);

        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        CustomerSession.initCustomerSession(
                mEphemeralKeyProvider,
                mStripeApiProxy,
                null);

        PaymentSession.PaymentSessionListener mockListener =
                mock(PaymentSession.PaymentSessionListener.class);
        PaymentSession paymentSession = new PaymentSession(mActivityController.get());
        paymentSession.init(mockListener, new PaymentSessionConfig.Builder().build());

        paymentSession.presentPaymentMethodSelection();

        ShadowActivity.IntentForResult intentForResult =
                mShadowActivity.getNextStartedActivityForResult();
        assertNotNull(intentForResult);
        assertEquals(PaymentMethodsActivity.class.getName(),
                intentForResult.intent.getComponent().getClassName());
        assertTrue(intentForResult.intent.hasExtra(EXTRA_PAYMENT_SESSION_ACTIVE));
    }

    @Test
    public void init_withoutSavedState_clearsLoggingTokensAndStartsWithPaymentSession() {
        CustomerEphemeralKey firstKey = CustomerEphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);

        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        CustomerSession.initCustomerSession(
                mEphemeralKeyProvider,
                mStripeApiProxy,
                null);

        CustomerSession.getInstance().addProductUsageTokenIfValid("PaymentMethodsActivity");
        assertEquals(1, CustomerSession.getInstance().getProductUsageTokens().size());

        PaymentSession.PaymentSessionListener mockListener =
                mock(PaymentSession.PaymentSessionListener.class);
        PaymentSession paymentSession = new PaymentSession(mActivityController.get());
        paymentSession.init(mockListener, new PaymentSessionConfig.Builder().build());

        // The init removes PaymentMethodsActivity, but then adds PaymentSession
        Set<String> loggingTokens = CustomerSession.getInstance().getProductUsageTokens();
        assertEquals(1, loggingTokens.size());
        assertFalse(loggingTokens.contains("PaymentMethodsActivity"));
        assertTrue(loggingTokens.contains("PaymentSession"));
    }

    @Test
    public void init_withSavedStateBundle_doesNotClearLoggingTokens() {
        CustomerEphemeralKey firstKey = CustomerEphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);

        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        CustomerSession.initCustomerSession(
                mEphemeralKeyProvider,
                mStripeApiProxy,
                null);

        CustomerSession.getInstance().addProductUsageTokenIfValid("PaymentMethodsActivity");
        assertEquals(1, CustomerSession.getInstance().getProductUsageTokens().size());

        PaymentSession.PaymentSessionListener mockListener =
                mock(PaymentSession.PaymentSessionListener.class);
        PaymentSession paymentSession = new PaymentSession(mActivityController.get());
        // If it is given any saved state at all, the tokens are not cleared out.
        paymentSession.init(mockListener, new PaymentSessionConfig.Builder().build(), new Bundle());

        Set<String> loggingTokens = CustomerSession.getInstance().getProductUsageTokens();
        assertEquals(2, loggingTokens.size());
        assertTrue(loggingTokens.contains("PaymentMethodsActivity"));
        assertTrue(loggingTokens.contains("PaymentSession"));
    }

    @Test
    public void completePayment_withLoggedActions_clearsLoggingTokensAndSetsResult() {
        CustomerEphemeralKey firstKey = CustomerEphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);

        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        CustomerSession.initCustomerSession(
                mEphemeralKeyProvider,
                mStripeApiProxy,
                null);

        CustomerSession.getInstance().addProductUsageTokenIfValid("PaymentMethodsActivity");
        assertEquals(1, CustomerSession.getInstance().getProductUsageTokens().size());

        PaymentSession.PaymentSessionListener mockListener =
                mock(PaymentSession.PaymentSessionListener.class);
        PaymentSession paymentSession = new PaymentSession(mActivityController.get());
        // If it is given any saved state at all, the tokens are not cleared out.
        paymentSession.init(mockListener, new PaymentSessionConfig.Builder().build(), new Bundle());

        Set<String> loggingTokens = CustomerSession.getInstance().getProductUsageTokens();
        assertEquals(2, loggingTokens.size());

        reset(mockListener);
        paymentSession.completePayment(new PaymentCompletionProvider() {
            @Override
            public void completePayment(@NonNull PaymentSessionData data,
                                        @NonNull PaymentResultListener listener) {
                listener.onPaymentResult(PaymentResultListener.SUCCESS);
            }
        });

        ArgumentCaptor<PaymentSessionData> dataArgumentCaptor =
                ArgumentCaptor.forClass(PaymentSessionData.class);
        verify(mockListener).onPaymentSessionDataChanged(dataArgumentCaptor.capture());
        PaymentSessionData capturedData = dataArgumentCaptor.getValue();
        assertNotNull(capturedData);
        assertEquals(PaymentResultListener.SUCCESS, capturedData.getPaymentResult());
        assertTrue(CustomerSession.getInstance().getProductUsageTokens().isEmpty());
    }

    @Test
    public void init_withSavedState_setsPaymentSessionData() {
        CustomerEphemeralKey firstKey = CustomerEphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);

        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        CustomerSession.initCustomerSession(
                mEphemeralKeyProvider,
                mStripeApiProxy,
                null);

        PaymentSession.PaymentSessionListener mockListener =
                mock(PaymentSession.PaymentSessionListener.class);
        PaymentSession paymentSession = new PaymentSession(mActivityController.get());
        paymentSession.init(mockListener, new PaymentSessionConfig.Builder().build());

        ArgumentCaptor<PaymentSessionData> paySessionDataCaptor = getDataCaptor();
        paymentSession.setCartTotal(300L);

        verify(mockListener).onPaymentSessionDataChanged(paySessionDataCaptor.capture());
        Bundle bundle = new Bundle();
        paymentSession.savePaymentSessionInstanceState(bundle);

        PaymentSession.PaymentSessionListener secondListener =
                mock(PaymentSession.PaymentSessionListener.class);
        ArgumentCaptor<PaymentSessionData> secondSessionDataCaptor = getDataCaptor();

        paymentSession.init(secondListener, new PaymentSessionConfig.Builder().build(), bundle);
        verify(secondListener).onPaymentSessionDataChanged(secondSessionDataCaptor.capture());

        PaymentSessionData firstData = paySessionDataCaptor.getValue();
        PaymentSessionData secondData = secondSessionDataCaptor.getValue();
        assertEquals(firstData.getCartTotal(), secondData.getCartTotal());
        assertEquals(firstData.getSelectedPaymentMethodId(),
                secondData.getSelectedPaymentMethodId());
    }

    private ArgumentCaptor<PaymentSessionData> getDataCaptor() {
        return ArgumentCaptor.forClass(PaymentSessionData.class);
    }
}
