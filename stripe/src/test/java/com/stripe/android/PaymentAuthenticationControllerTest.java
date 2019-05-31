package com.stripe.android;

import android.app.Activity;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.model.PaymentIntentFixtures;
import com.stripe.android.model.Stripe3ds2AuthResult;
import com.stripe.android.model.Stripe3ds2AuthResultFixtures;
import com.stripe.android.stripe3ds2.service.StripeThreeDs2Service;
import com.stripe.android.stripe3ds2.transaction.MessageVersionRegistry;
import com.stripe.android.stripe3ds2.transaction.Transaction;
import com.stripe.android.view.ActivityStarter;
import com.stripe.android.view.PaymentAuthenticationExtras;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class PaymentAuthenticationControllerTest {

    private static final String DIRECTORY_SERVER_ID = "F000000000";
    private static final String MESSAGE_VERSION = "2.1.0";
    private static final String PUBLISHABLE_KEY = "pk_test";
    private static final int MAX_TIMEOUT = 5;

    private static final PaymentAuthConfig CONFIG = new PaymentAuthConfig.Builder()
            .set3ds2Config(new PaymentAuthConfig.Stripe3ds2Config.Builder()
                    .setTimeout(10)
                    .build())
            .build();

    private PaymentAuthenticationController mController;

    @Mock private Stripe mStripe;
    @Mock private Activity mActivity;
    @Mock private StripeThreeDs2Service mThreeDs2Service;
    @Mock private Transaction mTransaction;
    @Mock private StripeApiHandler mApiHandler;
    @Mock private MessageVersionRegistry mMessageVersionRegistry;
    @Mock private ActivityStarter<Stripe3ds2CompletionStarter.StartData> m3ds2Starter;
    @Mock private ApiResultCallback<PaymentAuthResult> mPaymentAuthResultCallback;
    @Mock private PaymentAuthRelayStarter mPaymentAuthRelayStarter;

    @Captor private ArgumentCaptor<PaymentAuthRelayStarter.Data> mRelayStarterDataArgumentCaptor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(mTransaction.getAuthenticationRequestParameters())
                .thenReturn(Stripe3ds2Fixtures.AREQ_PARAMS);
        when(mThreeDs2Service.createTransaction(DIRECTORY_SERVER_ID, MESSAGE_VERSION, false))
                .thenReturn(mTransaction);
        when(mMessageVersionRegistry.getCurrent()).thenReturn(MESSAGE_VERSION);
        mController = new PaymentAuthenticationController(
                ApplicationProvider.getApplicationContext(),
                mThreeDs2Service,
                mApiHandler,
                mMessageVersionRegistry,
                DIRECTORY_SERVER_ID,
                CONFIG);
    }

    @Test
    public void handleNextAction_with3ds2() {
        mController.handleNextAction(mActivity, PaymentIntentFixtures.PI_REQUIRES_3DS2,
                PUBLISHABLE_KEY);
        verify(mThreeDs2Service).createTransaction(DIRECTORY_SERVER_ID, MESSAGE_VERSION, false);
        verify(mApiHandler).start3ds2Auth(ArgumentMatchers.<Stripe3ds2AuthParams>any(),
                eq(PUBLISHABLE_KEY),
                ArgumentMatchers.<ApiResultCallback<Stripe3ds2AuthResult>>any());
    }

    @Test
    public void handleNextAction_when3dsRedirect() {
        mController.handleNextAction(mActivity, PaymentIntentFixtures.PI_REQUIRES_REDIRECT,
                PUBLISHABLE_KEY);
        verify(mActivity).startActivityForResult(any(Intent.class),
                eq(PaymentAuthenticationController.REQUEST_CODE));
    }

    @Test
    public void shouldHandleResult_withInvalidResultCode() {
        assertFalse(mController.shouldHandleResult(500, Activity.RESULT_OK, new Intent()));
    }

    @Test
    public void test3ds2Completion_whenCanceled_shouldCallStarterWithCancelStatus() {
        new PaymentAuthenticationController.PaymentAuth3ds2ChallengeStatusReceiver(m3ds2Starter,
                PaymentIntentFixtures.PI_REQUIRES_3DS2)
                .cancelled();
        verify(m3ds2Starter).start(
                new Stripe3ds2CompletionStarter.StartData(PaymentIntentFixtures.PI_REQUIRES_3DS2,
                        Stripe3ds2CompletionStarter.Status.CANCEL));
    }

    @Test
    public void handleResult_withAuthException_shouldCallCallbackOnError() {
        final Exception exception = new RuntimeException();
        final Intent intent = new Intent()
                .putExtra(PaymentAuthenticationExtras.AUTH_EXCEPTION, exception);

        mController.handleResult(mStripe, intent, PUBLISHABLE_KEY, mPaymentAuthResultCallback);
        verify(mPaymentAuthResultCallback).onError(exception);
        verify(mPaymentAuthResultCallback, never())
                .onSuccess(ArgumentMatchers.<PaymentAuthResult>any());
    }

    @Test
    public void authCallback_withChallengeFlow_shouldNotStartRelayActivity() {
        final PaymentAuthenticationController.Stripe3ds2AuthCallback authCallback =
                new PaymentAuthenticationController.Stripe3ds2AuthCallback(mActivity, mTransaction,
                        MAX_TIMEOUT, PaymentIntentFixtures.PI_REQUIRES_3DS2,
                        mPaymentAuthRelayStarter);
        authCallback.onSuccess(Stripe3ds2AuthResultFixtures.ARES_CHALLENGE_FLOW);
        verify(mPaymentAuthRelayStarter, never())
                .start(ArgumentMatchers.<PaymentAuthRelayStarter.Data>any());
    }

    @Test
    public void authCallback_withFrictionlessFlow_shouldStartRelayActivityWithPaymentIntent() {
        final PaymentAuthenticationController.Stripe3ds2AuthCallback authCallback =
                new PaymentAuthenticationController.Stripe3ds2AuthCallback(mActivity, mTransaction,
                        MAX_TIMEOUT, PaymentIntentFixtures.PI_REQUIRES_3DS2,
                        mPaymentAuthRelayStarter);
        authCallback.onSuccess(Stripe3ds2AuthResultFixtures.ARES_FRICTIONLESS_FLOW);
        verify(mPaymentAuthRelayStarter)
                .start(mRelayStarterDataArgumentCaptor.capture());
        assertEquals(PaymentIntentFixtures.PI_REQUIRES_3DS2,
                mRelayStarterDataArgumentCaptor.getValue().paymentIntent);
    }

    @Test
    public void authCallback_withError_shouldStartRelayActivityWithException() {
        final PaymentAuthenticationController.Stripe3ds2AuthCallback authCallback =
                new PaymentAuthenticationController.Stripe3ds2AuthCallback(mActivity, mTransaction,
                        MAX_TIMEOUT, PaymentIntentFixtures.PI_REQUIRES_3DS2,
                        mPaymentAuthRelayStarter);
        authCallback.onSuccess(Stripe3ds2AuthResultFixtures.ERROR);
        verify(mPaymentAuthRelayStarter).start(mRelayStarterDataArgumentCaptor.capture());
        final Exception exception = Objects.requireNonNull(
                mRelayStarterDataArgumentCaptor.getValue().exception);
        assertEquals("Error encountered during 3DS2 authentication request. " +
                        "Code: 302, Detail: null, " +
                        "Description: Data could not be decrypted by the receiving system due to technical or other reason., " +
                        "Component: D",
                exception.getMessage());
    }
}
