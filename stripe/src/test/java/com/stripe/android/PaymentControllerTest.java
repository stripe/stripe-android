package com.stripe.android;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.APIException;
import com.stripe.android.exception.AuthenticationException;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.model.ConfirmPaymentIntentParams;
import com.stripe.android.model.PaymentIntentFixtures;
import com.stripe.android.model.SetupIntentFixtures;
import com.stripe.android.model.Stripe3ds2AuthResult;
import com.stripe.android.model.Stripe3ds2AuthResultFixtures;
import com.stripe.android.model.Stripe3ds2Fingerprint;
import com.stripe.android.model.Stripe3ds2FingerprintTest;
import com.stripe.android.stripe3ds2.service.StripeThreeDs2Service;
import com.stripe.android.stripe3ds2.transaction.CompletionEvent;
import com.stripe.android.stripe3ds2.transaction.ErrorMessage;
import com.stripe.android.stripe3ds2.transaction.MessageVersionRegistry;
import com.stripe.android.stripe3ds2.transaction.ProtocolErrorEvent;
import com.stripe.android.stripe3ds2.transaction.RuntimeErrorEvent;
import com.stripe.android.stripe3ds2.transaction.Transaction;
import com.stripe.android.stripe3ds2.views.ChallengeProgressDialogActivity;
import com.stripe.android.view.ActivityStarter;
import com.stripe.android.view.StripeIntentResultExtras;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    private AnalyticsDataFactory mAnalyticsDataFactory;

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
    @Mock private FireAndForgetRequestExecutor mFireAndForgetRequestExecutor;

    @Captor private ArgumentCaptor<PaymentRelayStarter.Data> mRelayStarterDataArgumentCaptor;
    @Captor private ArgumentCaptor<Intent> mIntentArgumentCaptor;
    @Captor private ArgumentCaptor<StripeRequest> mApiRequestArgumentCaptor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mAnalyticsDataFactory =
                new AnalyticsDataFactory(ApplicationProvider.getApplicationContext());
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
                CONFIG,
                mFireAndForgetRequestExecutor,
                mAnalyticsDataFactory);
    }

    @Test
    public void handleNextAction_withVisaAnd3ds2() {
        when(mThreeDs2Service.createTransaction(
                Stripe3ds2Fingerprint.DirectoryServer.Visa.id,
                MESSAGE_VERSION,
                PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2.isLiveMode(),
                Stripe3ds2Fingerprint.DirectoryServer.Visa.name,
                Stripe3ds2FingerprintTest.DS_CERTIFICATE_RSA,
                null))
                .thenReturn(mTransaction);
        mController.handleNextAction(mActivity, PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2,
                PUBLISHABLE_KEY);
        verify(mThreeDs2Service).createTransaction(
                Stripe3ds2Fingerprint.DirectoryServer.Visa.id,
                MESSAGE_VERSION,
                PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2.isLiveMode(),
                Stripe3ds2Fingerprint.DirectoryServer.Visa.name,
                Stripe3ds2FingerprintTest.DS_CERTIFICATE_RSA,
                null);
        verify(mApiHandler).start3ds2Auth(ArgumentMatchers.<Stripe3ds2AuthParams>any(),
                eq(Objects.requireNonNull(PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2.getId())),
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
                PaymentIntentFixtures.PI_REQUIRES_AMEX_3DS2.isLiveMode(),
                Stripe3ds2Fingerprint.DirectoryServer.Amex.name,
                Stripe3ds2FingerprintTest.DS_CERTIFICATE_RSA,
                PaymentIntentFixtures.KEY_ID))
                .thenReturn(mTransaction);
        mController.handleNextAction(mActivity, PaymentIntentFixtures.PI_REQUIRES_AMEX_3DS2,
                PUBLISHABLE_KEY);
        verify(mThreeDs2Service).createTransaction(
                Stripe3ds2Fingerprint.DirectoryServer.Amex.id,
                MESSAGE_VERSION,
                PaymentIntentFixtures.PI_REQUIRES_AMEX_3DS2.isLiveMode(),
                Stripe3ds2Fingerprint.DirectoryServer.Amex.name,
                Stripe3ds2FingerprintTest.DS_CERTIFICATE_RSA,
                PaymentIntentFixtures.KEY_ID);
        verify(mApiHandler).start3ds2Auth(ArgumentMatchers.<Stripe3ds2AuthParams>any(),
                eq(Objects.requireNonNull(PaymentIntentFixtures.PI_REQUIRES_AMEX_3DS2.getId())),
                eq(PUBLISHABLE_KEY),
                ArgumentMatchers.<ApiResultCallback<Stripe3ds2AuthResult>>any());

        verify(mActivity).startActivity(eq(
                new Intent(mActivity, ChallengeProgressDialogActivity.class)
                        .putExtra(ChallengeProgressDialogActivity.EXTRA_DIRECTORY_SERVER_NAME,
                                Stripe3ds2Fingerprint.DirectoryServer.Amex.name)));
    }

    @Test
    public void handleNextAction_whenSdk3ds1() {
        mController.handleNextAction(mActivity, PaymentIntentFixtures.PI_REQUIRES_3DS1,
                PUBLISHABLE_KEY);
        verify(mActivity).startActivityForResult(mIntentArgumentCaptor.capture(),
                eq(PaymentController.PAYMENT_REQUEST_CODE));
        final Intent intent = mIntentArgumentCaptor.getValue();
        assertEquals(
                "https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecve7CRMbs6FrXfm8AxXMIh/src_client_secret_F79yszOBAiuaZTuIhbn3LPUW",
                intent.getStringExtra(PaymentAuthWebViewStarter.EXTRA_AUTH_URL)
        );
        assertNull(intent.getStringExtra(PaymentAuthWebViewStarter.EXTRA_RETURN_URL));
    }

    @Test
    public void handleNextAction_whenBrowser3ds1() {
        mController.handleNextAction(mActivity, PaymentIntentFixtures.PI_REQUIRES_REDIRECT,
                PUBLISHABLE_KEY);
        verify(mActivity).startActivityForResult(mIntentArgumentCaptor.capture(),
                eq(PaymentController.PAYMENT_REQUEST_CODE));
        final Intent intent = mIntentArgumentCaptor.getValue();
        assertEquals(
                "https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecaz6CRMbs6FrXfuYKBRSUG/src_client_secret_F6octeOshkgxT47dr0ZxSZiv",
                intent.getStringExtra(PaymentAuthWebViewStarter.EXTRA_AUTH_URL)
        );
        assertEquals("stripe://deeplink",
                intent.getStringExtra(PaymentAuthWebViewStarter.EXTRA_RETURN_URL)
        );

        verify(mFireAndForgetRequestExecutor).executeAsync(mApiRequestArgumentCaptor.capture());
        final StripeRequest analyticsRequest = mApiRequestArgumentCaptor.getValue();
        final Map<String, ?> analyticsParams = Objects.requireNonNull(analyticsRequest.params);
        assertEquals("stripe_android.url_redirect_next_action",
                analyticsParams.get(AnalyticsDataFactory.FIELD_EVENT));
        assertEquals("pi_1EZlvVCRMbs6FrXfKpq2xMmy",
                analyticsParams.get(AnalyticsDataFactory.FIELD_INTENT_ID));
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
        assertFalse(mController.shouldHandlePaymentResult(500, new Intent()));
        assertFalse(mController.shouldHandleSetupResult(500, new Intent()));
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
                PaymentController.getRequestCode(
                        ConfirmPaymentIntentParams.createWithPaymentMethodId(
                                "pm_123", "client_secret", "")));
    }

    @Test
    public void test3ds2Receiver_whenCompleted_shouldFireAnalyticsRequest() {
        final CompletionEvent completionEvent = new CompletionEvent() {
            @Override
            public String getSDKTransactionID() {
                return "8dd3413f-0b45-4234-bc45-6cc40fb1b0f1";
            }

            @Override
            public String getTransactionStatus() {
                return "C";
            }
        };

        new PaymentController.PaymentAuth3ds2ChallengeStatusReceiver(mActivity,
                m3ds2Starter, mApiHandler, PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2,
                "src_123", ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                mFireAndForgetRequestExecutor, mAnalyticsDataFactory, mTransaction)
                .completed(completionEvent, "01");

        verify(mFireAndForgetRequestExecutor, times(2))
                .executeAsync(mApiRequestArgumentCaptor.capture());
        final List<StripeRequest> analyticsRequests = mApiRequestArgumentCaptor.getAllValues();

        final Map<String, ?> analyticsParamsFirst =
                Objects.requireNonNull(analyticsRequests.get(0).params);
        assertEquals("stripe_android.3ds2_challenge_flow_completed",
                analyticsParamsFirst.get(AnalyticsDataFactory.FIELD_EVENT));

        final Map<String, ?> analyticsParamsSecond =
                Objects.requireNonNull(analyticsRequests.get(1).params);
        assertEquals("stripe_android.3ds2_challenge_flow_presented",
                analyticsParamsSecond.get(AnalyticsDataFactory.FIELD_EVENT));
    }

    @Test
    public void test3ds2Receiver_whenTimedout_shouldFireAnalyticsRequest() {
        new PaymentController.PaymentAuth3ds2ChallengeStatusReceiver(mActivity,
                m3ds2Starter, mApiHandler, PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2,
                "src_123", ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                mFireAndForgetRequestExecutor, mAnalyticsDataFactory, mTransaction)
                .timedout("01");
        verify(mFireAndForgetRequestExecutor, times(2))
                .executeAsync(mApiRequestArgumentCaptor.capture());
        final List<StripeRequest> analyticsRequests = mApiRequestArgumentCaptor.getAllValues();

        final Map<String, ?> analyticsParamsFirst =
                Objects.requireNonNull(analyticsRequests.get(0).params);
        assertEquals("stripe_android.3ds2_challenge_flow_timed_out",
                analyticsParamsFirst.get(AnalyticsDataFactory.FIELD_EVENT));

        final Map<String, ?> analyticsParamsSecond =
                Objects.requireNonNull(analyticsRequests.get(1).params);
        assertEquals("stripe_android.3ds2_challenge_flow_presented",
                analyticsParamsSecond.get(AnalyticsDataFactory.FIELD_EVENT));
    }

    @Test
    public void test3ds2Receiver_whenCanceled_shouldFireAnalyticsRequest() {
        new PaymentController.PaymentAuth3ds2ChallengeStatusReceiver(mActivity,
                m3ds2Starter, mApiHandler, PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2,
                "src_123", ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                mFireAndForgetRequestExecutor, mAnalyticsDataFactory, mTransaction)
                .cancelled("01");

        verify(mFireAndForgetRequestExecutor, times(2))
                .executeAsync(mApiRequestArgumentCaptor.capture());
        final List<StripeRequest> analyticsRequests = mApiRequestArgumentCaptor.getAllValues();

        final Map<String, ?> analyticsParamsFirst =
                Objects.requireNonNull(analyticsRequests.get(0).params);
        assertEquals("stripe_android.3ds2_challenge_flow_canceled",
                analyticsParamsFirst.get(AnalyticsDataFactory.FIELD_EVENT));

        final Map<String, ?> analyticsParamsSecond =
                Objects.requireNonNull(analyticsRequests.get(1).params);
        assertEquals("stripe_android.3ds2_challenge_flow_presented",
                analyticsParamsSecond.get(AnalyticsDataFactory.FIELD_EVENT));
    }

    @Test
    public void test3ds2Receiver_whenRuntimeErrorError_shouldFireAnalyticsRequest() {
        final RuntimeErrorEvent runtimeErrorEvent = new RuntimeErrorEvent() {
            @NonNull
            @Override
            public String getErrorCode() {
                return "404";
            }

            @NonNull
            @Override
            public String getErrorMessage() {
                return "Resource not found";
            }
        };

        new PaymentController.PaymentAuth3ds2ChallengeStatusReceiver(mActivity,
                m3ds2Starter, mApiHandler, PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2,
                "src_123", ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                mFireAndForgetRequestExecutor, mAnalyticsDataFactory, mTransaction)
                .runtimeError(runtimeErrorEvent);


        verify(mFireAndForgetRequestExecutor, times(2))
                .executeAsync(mApiRequestArgumentCaptor.capture());
        final List<StripeRequest> analyticsRequests = mApiRequestArgumentCaptor.getAllValues();

        final Map<String, ?> analyticsParamsFirst =
                Objects.requireNonNull(analyticsRequests.get(0).params);
        assertEquals("stripe_android.3ds2_challenge_flow_errored",
                analyticsParamsFirst.get(AnalyticsDataFactory.FIELD_EVENT));

        final Map<String, String> errorData = (Map<String, String>)
                Objects.requireNonNull(
                        analyticsParamsFirst.get(AnalyticsDataFactory.FIELD_ERROR_DATA));
        assertEquals("404", errorData.get("error_code"));
        assertEquals("Resource not found", errorData.get("error_message"));

        final Map<String, ?> analyticsParamsSecond =
                Objects.requireNonNull(analyticsRequests.get(1).params);
        assertEquals("stripe_android.3ds2_challenge_flow_presented",
                analyticsParamsSecond.get(AnalyticsDataFactory.FIELD_EVENT));
    }

    @Test
    public void test3ds2Receiver_whenProtocolError_shouldFireAnalyticsRequest() {
        final ProtocolErrorEvent protocolErrorEvent = new ProtocolErrorEvent() {
            @NonNull
            @Override
            public String getSDKTransactionID() {
                return "8dd3413f-0b45-4234-bc45-6cc40fb1b0f1";
            }

            @Override
            public ErrorMessage getErrorMessage() {
                return new ErrorMessage() {
                    @Override
                    public String getTransactionID() {
                        return "047f76a6-d1d4-48a2-aa65-786abb6f7f46";
                    }

                    @Override
                    public String getErrorCode() {
                        return "201";
                    }

                    @Override
                    public String getErrorDescription() {
                        return "Required element missing";
                    }

                    @Override
                    public String getErrorDetails() {
                        return "eci";
                    }
                };
            }
        };

        new PaymentController.PaymentAuth3ds2ChallengeStatusReceiver(mActivity,
                m3ds2Starter, mApiHandler, PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2,
                "src_123", ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                mFireAndForgetRequestExecutor, mAnalyticsDataFactory, mTransaction)
                .protocolError(protocolErrorEvent);

        verify(mFireAndForgetRequestExecutor, times(2))
                .executeAsync(mApiRequestArgumentCaptor.capture());
        final List<StripeRequest> analyticsRequests = mApiRequestArgumentCaptor.getAllValues();

        final Map<String, ?> analyticsParamsFirst =
                Objects.requireNonNull(analyticsRequests.get(0).params);
        assertEquals("stripe_android.3ds2_challenge_flow_errored",
                analyticsParamsFirst.get(AnalyticsDataFactory.FIELD_EVENT));

        final Map<String, String> errorData = (Map<String, String>)
                Objects.requireNonNull(
                        analyticsParamsFirst.get(AnalyticsDataFactory.FIELD_ERROR_DATA));
        assertEquals("201", errorData.get("error_code"));

        final Map<String, ?> analyticsParamsSecond =
                Objects.requireNonNull(analyticsRequests.get(1).params);
        assertEquals("stripe_android.3ds2_challenge_flow_presented",
                analyticsParamsSecond.get(AnalyticsDataFactory.FIELD_EVENT));
    }

    @Test
    public void test3ds2Completion_whenCanceled_shouldCallStarterWithCancelStatus() {
        new PaymentController.PaymentAuth3ds2ChallengeStatusReceiver(mActivity,
                m3ds2Starter, mApiHandler, PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2,
                "src_123", ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                mFireAndForgetRequestExecutor, mAnalyticsDataFactory, mTransaction)
                .cancelled("01");
        verify(mApiHandler).complete3ds2Auth(eq("src_123"),
                eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                ArgumentMatchers.<ApiResultCallback<Boolean>>any());
    }

    @Test
    public void getClientSecret_shouldGetClientSecretFromIntent() {
        final Intent data = new Intent()
                .putExtra(StripeIntentResultExtras.CLIENT_SECRET,
                        PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2.getClientSecret());
        assertNotNull(PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2.getClientSecret());
        assertEquals(PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2.getClientSecret(),
                mController.getClientSecret(data));
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
                eq(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT.getClientSecret()),
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
                        ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, mPaymentRelayStarter,
                        mFireAndForgetRequestExecutor, mAnalyticsDataFactory);
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
                        mPaymentRelayStarter,
                        mFireAndForgetRequestExecutor,
                        mAnalyticsDataFactory);
        authCallback.onSuccess(Stripe3ds2AuthResultFixtures.ARES_FRICTIONLESS_FLOW);
        verify(mPaymentRelayStarter)
                .start(mRelayStarterDataArgumentCaptor.capture());
        assertEquals(PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2,
                mRelayStarterDataArgumentCaptor.getValue().stripeIntent);

        verify(mFireAndForgetRequestExecutor).executeAsync(mApiRequestArgumentCaptor.capture());
        final StripeRequest analyticsRequest = mApiRequestArgumentCaptor.getValue();
        final Map<String, ?> analyticsParams = Objects.requireNonNull(analyticsRequest.params);
        assertEquals("stripe_android.3ds2_frictionless_flow",
                analyticsParams.get(AnalyticsDataFactory.FIELD_EVENT));
        assertEquals("pi_1EceMnCRMbs6FrXfCXdF8dnx",
                analyticsParams.get(AnalyticsDataFactory.FIELD_INTENT_ID));
    }

    @Test
    public void authCallback_withFallbackRedirectUrl_shouldStartAuthWebView() {
        final PaymentController.Stripe3ds2AuthCallback authCallback =
                new PaymentController.Stripe3ds2AuthCallback(mActivity, mApiHandler,
                        mTransaction, MAX_TIMEOUT,
                        PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2, SOURCE_ID,
                        ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                        mPaymentRelayStarter,
                        mFireAndForgetRequestExecutor,
                        mAnalyticsDataFactory);
        authCallback.onSuccess(Stripe3ds2AuthResultFixtures.FALLBACK_REDIRECT_URL);
        verify(mActivity).startActivityForResult(mIntentArgumentCaptor.capture(),
                eq(PaymentController.PAYMENT_REQUEST_CODE));
        final Intent intent = mIntentArgumentCaptor.getValue();
        assertEquals(
                "https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecve7CRMbs6FrXfm8AxXMIh/src_client_secret_F79yszOBAiuaZTuIhbn3LPUW",
                intent.getStringExtra(PaymentAuthWebViewStarter.EXTRA_AUTH_URL)
        );
        assertNull(intent.getStringExtra(PaymentAuthWebViewStarter.EXTRA_RETURN_URL));
    }

    @Test
    public void authCallback_withError_shouldStartRelayActivityWithException() {
        final PaymentController.Stripe3ds2AuthCallback authCallback =
                new PaymentController.Stripe3ds2AuthCallback(mActivity, mApiHandler,
                        mTransaction, MAX_TIMEOUT,
                        PaymentIntentFixtures.PI_REQUIRES_VISA_3DS2, SOURCE_ID,
                        ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, mPaymentRelayStarter,
                        mFireAndForgetRequestExecutor,
                        mAnalyticsDataFactory);
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
