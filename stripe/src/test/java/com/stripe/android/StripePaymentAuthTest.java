package com.stripe.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.model.PaymentIntentFixtures;
import com.stripe.android.model.PaymentIntentParams;
import com.stripe.android.model.SetupIntentParams;

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
    @Mock private ApiResultCallback<PaymentIntentResult> mPaymentCallback;
    @Mock private ApiResultCallback<SetupIntentResult> mSetupCallback;

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
    public void confirmSetupIntent_shouldConfirmAndAuth() {
        final Stripe stripe = createStripe();
        final SetupIntentParams setupIntentParams =
                SetupIntentParams.createConfirmSetupIntenParamsWithPaymentMethodId(
                        "pm_card_threeDSecure2Required",
                        "client_secret",
                        "yourapp://post-authentication-return-url");
        stripe.confirmSetupIntent(mActivity, setupIntentParams);
        verify(mPaymentController).startConfirmAndAuth(eq(stripe), eq(mActivity),
                eq(setupIntentParams), eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY));
    }

    @Test
    public void authenticatePayment_shouldAuth() {
        final Stripe stripe = createStripe();
        stripe.authenticatePayment(mActivity, PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2);
        verify(mPaymentController).startAuth(
                eq(mActivity),
                eq(PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2),
                eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        );
    }

    @Test
    public void onPaymentResult_whenShouldHandleResultIsTrue_shouldCallHandleResult() {
        final Intent data = new Intent();
        when(mPaymentController.shouldHandlePaymentResult(
                PaymentController.PAYMENT_REQUEST_CODE, Activity.RESULT_OK, data))
                .thenReturn(true);
        final Stripe stripe = createStripe();
        stripe.onPaymentResult(PaymentController.PAYMENT_REQUEST_CODE, Activity.RESULT_OK,
                data, mPaymentCallback);

        verify(mPaymentController).handlePaymentResult(stripe, data,
                ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, mPaymentCallback);
    }

    @Test
    public void onSetupResult_whenShouldHandleResultIsTrue_shouldCallHandleResult() {
        final Intent data = new Intent();
        when(mPaymentController.shouldHandleSetupResult(
                PaymentController.SETUP_REQUEST_CODE, Activity.RESULT_OK, data))
                .thenReturn(true);
        final Stripe stripe = createStripe();
        stripe.onSetupResult(PaymentController.SETUP_REQUEST_CODE, Activity.RESULT_OK,
                data, mSetupCallback);

        verify(mPaymentController).handleSetupResult(stripe, data,
                ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, mSetupCallback);
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
