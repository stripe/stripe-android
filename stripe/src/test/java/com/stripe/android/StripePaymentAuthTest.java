package com.stripe.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.model.PaymentIntentFixtures;
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
    @Mock private PaymentController mPaymentController;
    @Mock private ApiResultCallback<PaymentIntentResult> mCallback;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void confirmPayment_shouldConfirmAndAuth() {
        final Stripe stripe = createStripe();
        final PaymentIntentParams paymentIntentParams =
                PaymentIntentParams.createConfirmPaymentIntentWithPaymentMethodId(
                        "pm_card_threeDSecure2Required",
                        "client_secret",
                        "yourapp://post-authentication-return-url");
        stripe.confirmPayment(mActivity, paymentIntentParams);
        verify(mPaymentController).startConfirmAndAuth(eq(stripe), eq(mActivity),
                eq(paymentIntentParams), eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY));
    }

    @Test
    public void authenticatePayment_shouldAuth() {
        final Stripe stripe = createStripe();
        stripe.authenticatePayment(mActivity, PaymentIntentFixtures.PI_REQUIRES_3DS2);
        verify(mPaymentController).startAuth(
                eq(mActivity),
                eq(PaymentIntentFixtures.PI_REQUIRES_3DS2),
                eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        );
    }

    @Test
    public void onPaymentResult_whenShouldHandleResultIsTrue_shouldCallHandleResult() {
        final Intent data = new Intent();
        when(mPaymentController.shouldHandleResult(
                PaymentController.REQUEST_CODE, Activity.RESULT_OK, data))
                .thenReturn(true);
        final Stripe stripe = createStripe();
        stripe.onPaymentResult(PaymentController.REQUEST_CODE, Activity.RESULT_OK,
                data, mCallback);

        verify(mPaymentController).handleResult(stripe, data,
                ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, mCallback);
    }

    @NonNull
    private Stripe createStripe() {
        return new Stripe(
                new StripeApiHandler(
                        mContext,
                        new RequestExecutor(),
                        false,
                        null),
                new StripeNetworkUtils(mContext),
                mPaymentController,
                ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        );
    }
}
