package com.stripe.android;

import android.app.Activity;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.APIException;
import com.stripe.android.exception.AuthenticationException;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.model.PaymentIntentFixtures;
import com.stripe.android.model.PaymentIntentParams;
import com.stripe.android.model.SetupIntentFixtures;
import com.stripe.android.model.SetupIntentParams;
import com.stripe.android.model.Stripe3ds2AuthResult;
import com.stripe.android.model.Stripe3ds2AuthResultFixtures;
import com.stripe.android.model.Stripe3ds2Fingerprint;
import com.stripe.android.stripe3ds2.service.StripeThreeDs2Service;
import com.stripe.android.stripe3ds2.transaction.MessageVersionRegistry;
import com.stripe.android.stripe3ds2.transaction.Transaction;
import com.stripe.android.stripe3ds2.views.ChallengeProgressDialogActivity;
import com.stripe.android.view.ActivityStarter;
import com.stripe.android.view.StripeIntentResultExtras;

import java.util.Objects;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class PaymentControllerTest {

    private static final String MESSAGE_VERSION = "2.1.0";
    private static final String PUBLISHABLE_KEY = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY;
    private static final int MAX_TIMEOUT = 5;
    private static final String SOURCE_ID = "src_123";

    private static final PaymentAuthConfig CONFIG = new PaymentAuthConfig.Builder()
            .set3ds2Config(new PaymentAuthConfig.Stripe3ds2Config.Builder()
                    .setTimeout(5)
                    .build())
            .build();

    private PaymentController mController;

    @Mock private Stripe mStripe;
    @Mock private Activity mActivity;
    @Mock private StripeThreeDs2Service mThreeDs2Service;
    @Mock private Transaction mTransaction;
    @Mock private StripeApiHandler mApiHandler;
    @Mock private MessageVersionRegistry mMessageVersionRegistry;
    @Mock private ActivityStarter<Stripe3ds2CompletionStarter.StartData> m3ds2Starter;
    @Mock private ApiResultCallback<PaymentIntentResult> mPaymentAuthResultCallback;
    @Mock private ApiResultCallback<SetupIntentResult> mSetupAuthResultCallback;
    @Mock private PaymentRelayStarter mPaymentRelayStarter;

    @Captor private ArgumentCaptor<PaymentRelayStarter.Data> mRelayStarterDataArgumentCaptor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(mTransaction.getAuthenticationRequestParameters())
                .thenReturn(Stripe3ds2Fixtures.AREQ_PARAMS);
        when(mMessageVersionRegistry.getCurrent()).thenReturn(MESSAGE_VERSION);
        when(mActivity.getApplicationContext())
                .thenReturn(ApplicationProvider.getApplicationContext());
        mController = new PaymentController(
                ApplicationProvider.getApplicationContext(),
                mThreeDs2Service,
                mApiHandler,
                mMessageVersionRegistry,
                CONFIG);
    }

    @Test
    public void handleNextAction_withVisaAnd3ds2() {
        when(mThreeDs2Service.createTransaction(
                Stripe3ds2Fingerprint.DirectoryServer.Visa.id,
                MESSAGE_VERSION,
                false,
                Stripe3ds2Fingerprint.DirectoryServer.Visa.name))
                .thenReturn(mTransaction);
        mController.handleNextAction(mActivity, PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2,
                PUBLISHABLE_KEY);
        verify(mThreeDs2Service).createTransaction(
                Stripe3ds2Fingerprint.DirectoryServer.Visa.id,
                MESSAGE_VERSION,
                false,
                Stripe3ds2Fingerprint.DirectoryServer.Visa.name);
        verify(mApiHandler).start3ds2Auth(ArgumentMatchers.<Stripe3ds2AuthParams>any(),
                eq(PUBLISHABLE_KEY),
                ArgumentMatchers.<ApiResultCallback<Stripe3ds2AuthResult>>any());

        verify(mActivity).startActivity(eq(
                new Intent(mActivity, ChallengeProgressDialogActivity.class)
                        .putExtra(ChallengeProgressDialogActivity.EXTRA_DIRECTORY_SERVER_NAME,
                                Stripe3ds2Fingerprint.DirectoryServer.Visa.name)));
    }

    @Test
    public void handleNextAction_withAmexAnd3ds2() {
        when(mThreeDs2Service.createTransaction(
                Stripe3ds2Fingerprint.DirectoryServer.Amex.id,
                MESSAGE_VERSION,
                false,
                Stripe3ds2Fingerprint.DirectoryServer.Amex.name))
                .thenReturn(mTransaction);
        mController.handleNextAction(mActivity, PaymentIntentFixtures.PI_REQUIRES_AMEX_3DS2,
                PUBLISHABLE_KEY);
        verify(mThreeDs2Service).createTransaction(
                Stripe3ds2Fingerprint.DirectoryServer.Amex.id,
                MESSAGE_VERSION,
                false,
                Stripe3ds2Fingerprint.DirectoryServer.Amex.name);
        verify(mApiHandler).start3ds2Auth(ArgumentMatchers.<Stripe3ds2AuthParams>any(),
                eq(PUBLISHABLE_KEY),
                ArgumentMatchers.<ApiResultCallback<Stripe3ds2AuthResult>>any());

        verify(mActivity).startActivity(eq(
                new Intent(mActivity, ChallengeProgressDialogActivity.class)
                        .putExtra(ChallengeProgressDialogActivity.EXTRA_DIRECTORY_SERVER_NAME,
                                Stripe3ds2Fingerprint.DirectoryServer.Amex.name)));
    }

    @Test
    public void handleNextAction_when3dsRedirect() {
        mController.handleNextAction(mActivity, PaymentIntentFixtures.PI_REQUIRES_REDIRECT,
                PUBLISHABLE_KEY);
        verify(mActivity).startActivityForResult(any(Intent.class),
                eq(PaymentController.PAYMENT_REQUEST_CODE));
    }

    @Test
    public void handleNextAction_when3dsRedirectWithSetupIntent() {
        mController.handleNextAction(mActivity, SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT,
                PUBLISHABLE_KEY);
        verify(mActivity).startActivityForResult(any(Intent.class),
                eq(PaymentController.SETUP_REQUEST_CODE));
    }

    @Test
    public void shouldHandleResult_withInvalidResultCode() {
        assertFalse(mController.shouldHandlePaymentResult(500, Activity.RESULT_OK, new Intent()));
        assertFalse(mController.shouldHandleSetupResult(500, Activity.RESULT_OK, new Intent()));
    }

    @Test
    public void getRequestCode_withIntents_correctCodeReturned() {
        assertEquals(PaymentController.PAYMENT_REQUEST_CODE,
                PaymentController.getRequestCode(PaymentIntentFixtures.PI_REQUIRES_3DS1));
        assertEquals(PaymentController.SETUP_REQUEST_CODE,
                PaymentController.getRequestCode(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT));
    }

    @Test
    public void getRequestCode_withParams_correctCodeReturned() {
        assertEquals(PaymentController.PAYMENT_REQUEST_CODE,
                PaymentController.getRequestCode(PaymentIntentParams.createCustomParams()));
        assertEquals(PaymentController.SETUP_REQUEST_CODE,
                PaymentController.getRequestCode(
                        SetupIntentParams.createRetrieveSetupIntentParams("")));
    }

    @Test
    public void test3ds2Completion_whenCanceled_shouldCallStarterWithCancelStatus() {
        new PaymentController.PaymentAuth3ds2ChallengeStatusReceiver(mActivity,
                m3ds2Starter, mApiHandler, PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2,
                "src_123", ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
                .cancelled();
        verify(mApiHandler).complete3ds2Auth(eq("src_123"),
                eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                ArgumentMatchers.<ApiResultCallback<Boolean>>any());
    }

    @Test
    public void createPaymentIntentParams_shouldCreateParams() {
        final Intent data = new Intent()
                .putExtra(StripeIntentResultExtras.CLIENT_SECRET,
                        PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2.getClientSecret());

        assertNotNull(PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2.getClientSecret());
        final PaymentIntentParams expectedPaymentParams = PaymentIntentParams.createCustomParams()
                .setClientSecret(PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2.getClientSecret());

        assertEquals(expectedPaymentParams, mController.createPaymentIntentParams(data));
    }

    @Test
    public void createSetupIntentParams_shouldCreateParams() {
        final Intent data = new Intent()
                .putExtra(StripeIntentResultExtras.CLIENT_SECRET,
                        SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT.getClientSecret());

        assertNotNull(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT.getClientSecret());
        final SetupIntentParams expectedSetupParams = SetupIntentParams.createCustomParams()
                .setClientSecret(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT.getClientSecret());

        assertEquals(expectedSetupParams, mController.createSetupIntentParams(data));
    }

    @Test
    public void handlePaymentResult_withAuthException_shouldCallCallbackOnError() {
        final Exception exception = new RuntimeException();
        final Intent intent = new Intent()
                .putExtra(StripeIntentResultExtras.AUTH_EXCEPTION, exception);

        mController
                .handlePaymentResult(mStripe, intent, PUBLISHABLE_KEY, mPaymentAuthResultCallback);
        verify(mPaymentAuthResultCallback).onError(exception);
        verify(mPaymentAuthResultCallback, never())
                .onSuccess(ArgumentMatchers.<PaymentIntentResult>any());
    }

    @Test
    public void handleSetupResult_withAuthException_shouldCallCallbackOnError() {
        final Exception exception = new RuntimeException();
        final Intent intent = new Intent()
                .putExtra(StripeIntentResultExtras.AUTH_EXCEPTION, exception);

        mController.handleSetupResult(mStripe, intent, PUBLISHABLE_KEY, mSetupAuthResultCallback);

        verify(mSetupAuthResultCallback).onError(exception);
        verify(mSetupAuthResultCallback, never())
                .onSuccess(ArgumentMatchers.<SetupIntentResult>any());
    }

    @Test
    public void handleSetupResult_shouldCallbackOnSuccess()
            throws APIException, AuthenticationException, InvalidRequestException,
            APIConnectionException {
        assertNotNull(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT.getClientSecret());
        
        final Intent intent = new Intent()
                .putExtra(StripeIntentResultExtras.AUTH_STATUS,
                        StripeIntentResult.Status.SUCCEEDED)
                .putExtra(StripeIntentResultExtras.CLIENT_SECRET,
                        SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT.getClientSecret());

        when(mStripe.retrieveSetupIntentSynchronous(
                eq(SetupIntentParams.createRetrieveSetupIntentParams(
                        SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT.getClientSecret())),
                eq(PUBLISHABLE_KEY)))
                .thenReturn(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT);

        mController.handleSetupResult(mStripe, intent, PUBLISHABLE_KEY, mSetupAuthResultCallback);

        final ArgumentCaptor<SetupIntentResult> resultCaptor =
                ArgumentCaptor.forClass(SetupIntentResult.class);
        verify(mSetupAuthResultCallback).onSuccess(resultCaptor.capture());
        final SetupIntentResult result = resultCaptor.getValue();
        assertEquals(StripeIntentResult.Status.SUCCEEDED, result.getStatus());
        assertEquals(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT, result.getIntent());
    }

    @Test
    public void authCallback_withChallengeFlow_shouldNotStartRelayActivity() {
        final PaymentController.Stripe3ds2AuthCallback authCallback =
                new PaymentController.Stripe3ds2AuthCallback(mActivity, mApiHandler,
                        mTransaction, MAX_TIMEOUT,
                        PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2, SOURCE_ID,
                        ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, mPaymentRelayStarter);
        authCallback.onSuccess(Stripe3ds2AuthResultFixtures.ARES_CHALLENGE_FLOW);
        verify(mPaymentRelayStarter, never())
                .start(ArgumentMatchers.<PaymentRelayStarter.Data>any());
    }

    @Test
    public void authCallback_withFrictionlessFlow_shouldStartRelayActivityWithPaymentIntent() {
        final PaymentController.Stripe3ds2AuthCallback authCallback =
                new PaymentController.Stripe3ds2AuthCallback(mActivity, mApiHandler,
                        mTransaction, MAX_TIMEOUT,
                        PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2, SOURCE_ID,
                        ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                        mPaymentRelayStarter);
        authCallback.onSuccess(Stripe3ds2AuthResultFixtures.ARES_FRICTIONLESS_FLOW);
        verify(mPaymentRelayStarter)
                .start(mRelayStarterDataArgumentCaptor.capture());
        assertEquals(PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2,
                mRelayStarterDataArgumentCaptor.getValue().stripeIntent);
    }

    @Test
    public void authCallback_withError_shouldStartRelayActivityWithException() {
        final PaymentController.Stripe3ds2AuthCallback authCallback =
                new PaymentController.Stripe3ds2AuthCallback(mActivity, mApiHandler,
                        mTransaction, MAX_TIMEOUT,
                        PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2, SOURCE_ID,
                        ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, mPaymentRelayStarter);
        authCallback.onSuccess(Stripe3ds2AuthResultFixtures.ERROR);
        verify(mPaymentRelayStarter).start(mRelayStarterDataArgumentCaptor.capture());
        final Exception exception = Objects.requireNonNull(
                mRelayStarterDataArgumentCaptor.getValue().exception);
        assertEquals("Error encountered during 3DS2 authentication request. " +
                        "Code: 302, Detail: null, " +
                        "Description: Data could not be decrypted by the receiving system due to technical or other reason., " +
                        "Component: D",
                exception.getMessage());
    }
}
