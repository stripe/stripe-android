package com.stripe.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.model.PaymentIntentParams;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class StripePaymentAuthTest {

    private Context mContext;

    @Mock private Activity mActivity;
    @Mock private PaymentAuthenticationController mPaymentAuthenticationController;
    @Mock private ApiResultCallback<PaymentAuthResult> mCallback;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void startPaymentAuth_shouldCallControllerConfirmAndAuth() {
        final Stripe stripe = createStripe();
        stripe.setDefaultPublishableKey("pk_test");
        final PaymentIntentParams paymentIntentParams =
                PaymentIntentParams.createConfirmPaymentIntentWithPaymentMethodId(
                        "pm_card_threeDSecure2Required",
                        "client_secret",
                        "yourapp://post-authentication-return-url");
        stripe.startPaymentAuth(mActivity, paymentIntentParams);
        verify(mPaymentAuthenticationController).confirmAndAuth(eq(stripe), eq(mActivity),
                eq(paymentIntentParams), eq("pk_test"));
    }

    @Test
    public void onPaymentAuthResult_whenShouldHandleResultIsTrue_shouldCallHandleResult() {
        final Intent data = new Intent();
        when(mPaymentAuthenticationController.shouldHandleResult(
                PaymentAuthenticationController.REQUEST_CODE, Activity.RESULT_OK, data))
                .thenReturn(true);
        final Stripe stripe = createStripe();
        stripe.setDefaultPublishableKey("pk_test");
        stripe.onPaymentAuthResult(PaymentAuthenticationController.REQUEST_CODE, Activity.RESULT_OK,
                data, mCallback);

        verify(mPaymentAuthenticationController).handleResult(stripe, data,
                "pk_test", mCallback);
    }

    @NonNull
    private Stripe createStripe() {
        return new Stripe(
                new StripeApiHandler(
                        mContext,
                        new RequestExecutor(),
                        false),
                new LoggingUtils(mContext),
                new StripeNetworkUtils(mContext),
                mPaymentAuthenticationController);
    }
}
