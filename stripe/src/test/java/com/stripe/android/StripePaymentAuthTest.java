package com.stripe.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.model.ConfirmPaymentIntentParams;
import com.stripe.android.model.ConfirmSetupIntentParams;
import com.stripe.android.model.PaymentIntentFixtures;
import com.stripe.android.model.SetupIntentFixtures;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Objects;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class StripePaymentAuthTest {

    private static final ApiRequest.Options REQUEST_OPTIONS =
            ApiRequest.Options.create(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY);

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
        final ConfirmPaymentIntentParams confirmPaymentIntentParams =
                ConfirmPaymentIntentParams.createWithPaymentMethodId(
                        "pm_card_threeDSecure2Required",
                        "client_secret",
                        "yourapp://post-authentication-return-url");
        stripe.confirmPayment(mActivity, confirmPaymentIntentParams);
        verify(mPaymentController).startConfirmAndAuth(eq(mActivity),
                eq(confirmPaymentIntentParams), eq(REQUEST_OPTIONS));
    }

    @Test
    public void confirmSetupIntent_shouldConfirmAndAuth() {
        final Stripe stripe = createStripe();
        final ConfirmSetupIntentParams confirmSetupIntentParams =
                ConfirmSetupIntentParams.create(
                        "pm_card_threeDSecure2Required",
                        "client_secret",
                        "yourapp://post-authentication-return-url");
        stripe.confirmSetupIntent(mActivity, confirmSetupIntentParams);
        verify(mPaymentController).startConfirmAndAuth(eq(mActivity),
                eq(confirmSetupIntentParams), eq(REQUEST_OPTIONS));
    }

    @Test
    public void authenticatePayment_shouldAuth() {
        final Stripe stripe = createStripe();
        final String clientSecret = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.getClientSecret();
        stripe.authenticatePayment(mActivity, Objects.requireNonNull(clientSecret));
        verify(mPaymentController).startAuth(
                eq(mActivity),
                eq(clientSecret),
                eq(REQUEST_OPTIONS)
        );
    }

    @Test
    public void authenticateSetup_shouldAuth() {
        final Stripe stripe = createStripe();
        final String clientSecret = SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT.getClientSecret();
        stripe.authenticateSetup(mActivity, Objects.requireNonNull(clientSecret));
        verify(mPaymentController).startAuth(
                eq(mActivity),
                eq(clientSecret),
                eq(REQUEST_OPTIONS)
        );
    }

    @Test
    public void onPaymentResult_whenShouldHandleResultIsTrue_shouldCallHandleResult() {
        final Intent data = new Intent();
        when(mPaymentController.shouldHandlePaymentResult(
                PaymentController.PAYMENT_REQUEST_CODE, data))
                .thenReturn(true);
        final Stripe stripe = createStripe();
        stripe.onPaymentResult(PaymentController.PAYMENT_REQUEST_CODE, data, mPaymentCallback);

        verify(mPaymentController).handlePaymentResult(data,
                ApiRequest.Options.create(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                mPaymentCallback);
    }

    @Test
    public void onSetupResult_whenShouldHandleResultIsTrue_shouldCallHandleResult() {
        final Intent data = new Intent();
        when(mPaymentController.shouldHandleSetupResult(
                PaymentController.SETUP_REQUEST_CODE, data))
                .thenReturn(true);
        final Stripe stripe = createStripe();
        stripe.onSetupResult(PaymentController.SETUP_REQUEST_CODE, data, mSetupCallback);

        verify(mPaymentController).handleSetupResult(data,
                ApiRequest.Options.create(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                mSetupCallback);
    }

    @NonNull
    private Stripe createStripe() {
        return new Stripe(
                new StripeApiHandler(
                        mContext,
                        new StripeApiRequestExecutor(),
                        new FakeFireAndForgetRequestExecutor(),
                        null),
                new StripeNetworkUtils(mContext),
                mPaymentController,
                ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        );
    }
}
