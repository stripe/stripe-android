package com.stripe.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.stripe.android.exception.StripeException;
import com.stripe.android.model.ConfirmPaymentIntentParams;
import com.stripe.android.model.ConfirmSetupIntentParams;
import com.stripe.android.model.ConfirmStripeIntentParams;
import com.stripe.android.model.PaymentIntent;
import com.stripe.android.model.SetupIntent;
import com.stripe.android.model.Stripe3ds2AuthResult;
import com.stripe.android.model.Stripe3ds2Fingerprint;
import com.stripe.android.model.Stripe3dsRedirect;
import com.stripe.android.model.StripeIntent;
import com.stripe.android.stripe3ds2.init.StripeConfigParameters;
import com.stripe.android.stripe3ds2.service.StripeThreeDs2Service;
import com.stripe.android.stripe3ds2.service.StripeThreeDs2ServiceImpl;
import com.stripe.android.stripe3ds2.transaction.AuthenticationRequestParameters;
import com.stripe.android.stripe3ds2.transaction.CompletionEvent;
import com.stripe.android.stripe3ds2.transaction.MessageVersionRegistry;
import com.stripe.android.stripe3ds2.transaction.ProtocolErrorEvent;
import com.stripe.android.stripe3ds2.transaction.RuntimeErrorEvent;
import com.stripe.android.stripe3ds2.transaction.StripeChallengeParameters;
import com.stripe.android.stripe3ds2.transaction.StripeChallengeStatusReceiver;
import com.stripe.android.stripe3ds2.transaction.Transaction;
import com.stripe.android.stripe3ds2.views.ChallengeProgressDialogActivity;
import com.stripe.android.view.AuthActivityStarter;
import com.stripe.android.view.StripeIntentResultExtras;

import java.lang.ref.WeakReference;
import java.security.cert.CertificateException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * A controller responsible for confirming and authenticating payment (typically through resolving
 * any required customer action). The payment authentication mechanism (e.g. 3DS) will be determined
 * by the {@link PaymentIntent} or {@link SetupIntent} object.
 */
class PaymentController {
    static final int PAYMENT_REQUEST_CODE = 50000;
    static final int SETUP_REQUEST_CODE = 50001;

    @NonNull private final StripeThreeDs2Service mThreeDs2Service;
    @NonNull private final StripeApiHandler mApiHandler;
    @NonNull private final MessageVersionRegistry mMessageVersionRegistry;
    @NonNull private final PaymentAuthConfig mConfig;
    @NonNull private final FireAndForgetRequestExecutor mAnalyticsRequestExecutor;
    @NonNull private final AnalyticsDataFactory mAnalyticsDataFactory;

    PaymentController(@NonNull Context context,
                      @NonNull StripeApiHandler apiHandler) {
        this(context,
                new StripeThreeDs2ServiceImpl(context, new StripeSSLSocketFactory()),
                apiHandler,
                new MessageVersionRegistry(),
                PaymentAuthConfig.get(),
                new StripeFireAndForgetRequestExecutor(),
                new AnalyticsDataFactory(context.getApplicationContext()));
    }

    @VisibleForTesting
    PaymentController(@NonNull Context context,
                      @NonNull StripeThreeDs2Service threeDs2Service,
                      @NonNull StripeApiHandler apiHandler,
                      @NonNull MessageVersionRegistry messageVersionRegistry,
                      @NonNull PaymentAuthConfig config,
                      @NonNull FireAndForgetRequestExecutor analyticsRequestExecutor,
                      @NonNull AnalyticsDataFactory analyticsDataFactory) {
        mConfig = config;
        mThreeDs2Service = threeDs2Service;
        mThreeDs2Service.initialize(context, new StripeConfigParameters(), null,
                config.stripe3ds2Config.uiCustomization.getUiCustomization());
        mApiHandler = apiHandler;
        mMessageVersionRegistry = messageVersionRegistry;
        mAnalyticsRequestExecutor = analyticsRequestExecutor;
        mAnalyticsDataFactory = analyticsDataFactory;
    }

    /**
     * Confirm the Stripe Intent and resolve any next actions
     */
    void startConfirmAndAuth(@NonNull Activity activity,
                             @NonNull ConfirmStripeIntentParams confirmStripeIntentParams,
                             @NonNull ApiRequest.Options requestOptions) {
        new ConfirmStripeIntentTask(mApiHandler, confirmStripeIntentParams, requestOptions,
                new ConfirmStripeIntentCallback(activity, requestOptions, this,
                        getRequestCode(confirmStripeIntentParams)))
                .execute();
    }

    void startAuth(@NonNull final Activity activity,
                   @NonNull String clientSecret,
                   @NonNull final ApiRequest.Options requestOptions) {
        new RetrieveIntentTask(mApiHandler,
                clientSecret,
                requestOptions,
                new ApiResultCallback<StripeIntent>() {
                    @Override
                    public void onSuccess(@NonNull StripeIntent stripeIntent) {
                        handleNextAction(activity, stripeIntent, requestOptions);
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        handleError(activity, PAYMENT_REQUEST_CODE, e);
                    }
                })
                .execute();
    }

    /**
     * Decide whether {@link #handlePaymentResult(Intent, ApiRequest.Options, ApiResultCallback)}
     * should be called.
     */
    boolean shouldHandlePaymentResult(int requestCode, @Nullable Intent data) {
        return requestCode == PAYMENT_REQUEST_CODE && data != null;
    }

    /**
     * Decide whether {@link #handleSetupResult(Intent, ApiRequest.Options, ApiResultCallback)}
     * should be called.
     */
    boolean shouldHandleSetupResult(int requestCode, @Nullable Intent data) {
        return requestCode == SETUP_REQUEST_CODE && data != null;
    }

    /**
     * If payment authentication triggered an exception, get the exception object and pass to
     * {@link ApiResultCallback#onError(Exception)}.
     *
     * Otherwise, get the PaymentIntent's client_secret from {@param data} and use to retrieve
     * the PaymentIntent object with updated status.
     *
     * @param data the result Intent
     */
    void handlePaymentResult(@NonNull Intent data,
                             @NonNull ApiRequest.Options requestOptions,
                             @NonNull final ApiResultCallback<PaymentIntentResult> callback) {
        final Exception authException = (Exception) data.getSerializableExtra(
                StripeIntentResultExtras.AUTH_EXCEPTION);
        if (authException != null) {
            callback.onError(authException);
            return;
        }

        @StripeIntentResult.Status final int authStatus = data.getIntExtra(
                StripeIntentResultExtras.AUTH_STATUS, StripeIntentResult.Status.UNKNOWN);
        new RetrieveIntentTask(mApiHandler, getClientSecret(data), requestOptions,
                new ApiResultCallback<StripeIntent>() {
                    @Override
                    public void onSuccess(@NonNull StripeIntent stripeIntent) {
                        if (stripeIntent instanceof PaymentIntent) {
                            callback.onSuccess(new PaymentIntentResult.Builder()
                                    .setPaymentIntent((PaymentIntent) stripeIntent)
                                    .setStatus(authStatus)
                                    .build());
                        }
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        callback.onError(e);
                    }
                })
                .execute();
    }

    /**
     * If setup authentication triggered an exception, get the exception object and pass to
     * {@link ApiResultCallback#onError(Exception)}.
     *
     * Otherwise, get the SetupIntent's client_secret from {@param data} and use to retrieve the
     * SetupIntent object with updated status.
     *
     * @param data the result Intent
     */
    void handleSetupResult(@NonNull Intent data,
                           @NonNull ApiRequest.Options requestOptions,
                           @NonNull final ApiResultCallback<SetupIntentResult> callback) {
        final Exception authException = (Exception) data.getSerializableExtra(
                StripeIntentResultExtras.AUTH_EXCEPTION);
        if (authException != null) {
            callback.onError(authException);
            return;
        }

        @StripeIntentResult.Status final int authStatus = data.getIntExtra(
                StripeIntentResultExtras.AUTH_STATUS, StripeIntentResult.Status.UNKNOWN);

        new RetrieveIntentTask(mApiHandler, getClientSecret(data), requestOptions,
                new ApiResultCallback<StripeIntent>() {
                    @Override
                    public void onSuccess(@NonNull StripeIntent stripeIntent) {
                        if (stripeIntent instanceof SetupIntent) {
                            callback.onSuccess(new SetupIntentResult.Builder()
                                    .setSetupIntent((SetupIntent) stripeIntent)
                                    .setStatus(authStatus)
                                    .build());
                        }
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        callback.onError(e);
                    }
                })
                .execute();
    }

    @VisibleForTesting
    @NonNull
    String getClientSecret(@NonNull Intent data) {
        return data.getStringExtra(StripeIntentResultExtras.CLIENT_SECRET);
    }

    /**
     * Determine which authentication mechanism should be used, or bypass authentication
     * if it is not needed.
     */
    @VisibleForTesting
    void handleNextAction(@NonNull Activity activity,
                          @NonNull StripeIntent stripeIntent,
                          @NonNull ApiRequest.Options requestOptions) {
        if (stripeIntent.requiresAction()) {
            final StripeIntent.NextActionType nextActionType = stripeIntent.getNextActionType();
            if (StripeIntent.NextActionType.UseStripeSdk == nextActionType) {
                final StripeIntent.SdkData sdkData =
                        Objects.requireNonNull(stripeIntent.getStripeSdkData());
                if (sdkData.is3ds2()) {
                    mAnalyticsRequestExecutor.executeAsync(
                            ApiRequest.createAnalyticsRequest(
                                    mAnalyticsDataFactory.createAuthParams(
                                            AnalyticsDataFactory.EventName.AUTH_3DS2_FINGERPRINT,
                                            StripeTextUtils.emptyIfNull(stripeIntent.getId()),
                                            requestOptions.apiKey
                                    ),
                                    requestOptions,
                                    null
                            )
                    );
                    try {
                        begin3ds2Auth(activity, stripeIntent,
                                Stripe3ds2Fingerprint.create(sdkData),
                                requestOptions);
                    } catch (CertificateException e) {
                        handleError(activity, getRequestCode(stripeIntent), e);
                    }
                } else if (sdkData.is3ds1()) {
                    beginWebAuth(activity, getRequestCode(stripeIntent),
                            Objects.requireNonNull(stripeIntent.getClientSecret()),
                            Stripe3dsRedirect.create(sdkData).getUrl(), null);
                } else {
                    // authentication type is not supported
                    bypassAuth(activity, stripeIntent);
                }
            } else if (StripeIntent.NextActionType.RedirectToUrl == nextActionType) {
                mAnalyticsRequestExecutor.executeAsync(
                        ApiRequest.createAnalyticsRequest(
                                mAnalyticsDataFactory.createAuthParams(
                                        AnalyticsDataFactory.EventName.AUTH_REDIRECT,
                                        StripeTextUtils.emptyIfNull(stripeIntent.getId()),
                                        requestOptions.apiKey),
                                requestOptions,
                                null
                        )
                );

                final StripeIntent.RedirectData redirectData =
                        Objects.requireNonNull(stripeIntent.getRedirectData());
                beginWebAuth(activity, getRequestCode(stripeIntent),
                        Objects.requireNonNull(stripeIntent.getClientSecret()),
                        redirectData.url.toString(),
                        redirectData.returnUrl);
            } else {
                // next action type is not supported, so bypass authentication
                bypassAuth(activity, stripeIntent);
            }
        } else {
            // no action required, so bypass authentication
            bypassAuth(activity, stripeIntent);
        }
    }

    /**
     * Get the appropriate request code for the given stripe intent type
     *
     * @param intent the {@link StripeIntent} to get the request code for
     * @return PAYMENT_REQUEST_CODE or SETUP_REQUEST_CODE
     */
    static int getRequestCode(@NonNull StripeIntent intent) {
        if (intent instanceof PaymentIntent) {
            return PAYMENT_REQUEST_CODE;
        }
        return SETUP_REQUEST_CODE;
    }

    /**
     * Get the appropriate request code for the given stripe intent params type
     *
     * @param params the {@link ConfirmStripeIntentParams} to get the request code for
     * @return PAYMENT_REQUEST_CODE or SETUP_REQUEST_CODE
     */
    static int getRequestCode(@NonNull ConfirmStripeIntentParams params) {
        if (params instanceof ConfirmPaymentIntentParams) {
            return PAYMENT_REQUEST_CODE;
        }
        return SETUP_REQUEST_CODE;
    }

    private void bypassAuth(@NonNull Activity activity, @NonNull StripeIntent stripeIntent) {
        new PaymentRelayStarter(activity, getRequestCode(stripeIntent))
                .start(new PaymentRelayStarter.Data(stripeIntent));
    }

    private void begin3ds2Auth(@NonNull Activity activity,
                               @NonNull StripeIntent stripeIntent,
                               @NonNull Stripe3ds2Fingerprint stripe3ds2Fingerprint,
                               @NonNull ApiRequest.Options requestOptions) {
        final Transaction transaction =
                mThreeDs2Service.createTransaction(stripe3ds2Fingerprint.directoryServer.id,
                        mMessageVersionRegistry.getCurrent(), stripeIntent.isLiveMode(),
                        stripe3ds2Fingerprint.directoryServer.name,
                        stripe3ds2Fingerprint.directoryServerEncryption.rootCerts,
                        stripe3ds2Fingerprint.directoryServerEncryption.directoryServerPublicKey,
                        stripe3ds2Fingerprint.directoryServerEncryption.keyId);

        ChallengeProgressDialogActivity.show(activity, stripe3ds2Fingerprint.directoryServer.name);

        final StripeIntent.RedirectData redirectData = stripeIntent.getRedirectData();
        final String returnUrl = redirectData != null ? redirectData.returnUrl : null;

        final AuthenticationRequestParameters areqParams =
                transaction.getAuthenticationRequestParameters();
        final int timeout = mConfig.stripe3ds2Config.timeout;
        final Stripe3ds2AuthParams authParams = new Stripe3ds2AuthParams(
                stripe3ds2Fingerprint.source,
                areqParams.getSDKAppID(),
                areqParams.getSDKReferenceNumber(),
                areqParams.getSDKTransactionID(),
                areqParams.getDeviceData(),
                areqParams.getSDKEphemeralPublicKey(),
                areqParams.getMessageVersion(),
                timeout,
                returnUrl
        );
        mApiHandler.start3ds2Auth(authParams, StripeTextUtils.emptyIfNull(stripeIntent.getId()),
                requestOptions,
                new Stripe3ds2AuthCallback(activity, mApiHandler, transaction, timeout,
                        stripeIntent, stripe3ds2Fingerprint.source, requestOptions,
                        mAnalyticsRequestExecutor, mAnalyticsDataFactory));
    }

    /**
     * Start in-app WebView activity.
     *
     * @param activity the payment authentication result will be returned as a result to this
     *         {@link Activity}
     */
    private static void beginWebAuth(@NonNull Activity activity,
                                     int requestCode,
                                     @NonNull String clientSecret,
                                     @NonNull String authUrl,
                                     @Nullable String returnUrl) {
        new PaymentAuthWebViewStarter(activity, requestCode).start(
                new PaymentAuthWebViewStarter.Data(clientSecret, authUrl, returnUrl));
    }

    private static void handleError(@NonNull Activity activity,
                                    int requestCode,
                                    @NonNull Exception exception) {
        new PaymentRelayStarter(activity, requestCode)
                .start(new PaymentRelayStarter.Data(exception));
    }

    private static final class RetrieveIntentTask extends ApiOperation<StripeIntent> {
        @NonNull private final StripeApiHandler mApiHandler;
        @NonNull private final String mClientSecret;
        @NonNull private final ApiRequest.Options mRequestOptions;

        private RetrieveIntentTask(@NonNull StripeApiHandler apiHandler,
                                   @NonNull String clientSecret,
                                   @NonNull ApiRequest.Options requestOptions,
                                   @NonNull ApiResultCallback<StripeIntent> callback) {
            super(callback);
            mApiHandler = apiHandler;
            mClientSecret = clientSecret;
            mRequestOptions = requestOptions;
        }

        @Nullable
        @Override
        StripeIntent getResult() throws StripeException {
            if (mClientSecret.startsWith("pi_")) {
                return mApiHandler.retrievePaymentIntent(mClientSecret, mRequestOptions);
            } else if (mClientSecret.startsWith("seti_")) {
                return mApiHandler.retrieveSetupIntent(mClientSecret, mRequestOptions);
            }
            return null;
        }
    }

    private static final class ConfirmStripeIntentTask extends ApiOperation<StripeIntent> {
        @NonNull private final StripeApiHandler mApiHandler;
        @NonNull private final ConfirmStripeIntentParams mParams;
        @NonNull private final ApiRequest.Options mRequestOptions;

        private ConfirmStripeIntentTask(@NonNull StripeApiHandler apiHandler,
                                        @NonNull ConfirmStripeIntentParams params,
                                        @NonNull ApiRequest.Options requestOptions,
                                        @NonNull ApiResultCallback<StripeIntent> callback) {
            super(callback);
            mApiHandler = apiHandler;
            mRequestOptions = requestOptions;

            // mark this request as `use_stripe_sdk=true`
            mParams = params.withShouldUseStripeSdk(true);
        }

        @Nullable
        @Override
        StripeIntent getResult() throws StripeException {
            if (mParams instanceof ConfirmPaymentIntentParams) {
                return mApiHandler.confirmPaymentIntent(
                        (ConfirmPaymentIntentParams) mParams,
                        mRequestOptions
                );
            } else if (mParams instanceof ConfirmSetupIntentParams) {
                return mApiHandler.confirmSetupIntent(
                        (ConfirmSetupIntentParams) mParams,
                        mRequestOptions
                );
            }
            return null;
        }
    }

    private static final class ConfirmStripeIntentCallback
            implements ApiResultCallback<StripeIntent> {
        @NonNull private final WeakReference<Activity> mActivityRef;
        @NonNull private final ApiRequest.Options mRequestOptions;
        @NonNull private final PaymentController mPaymentController;
        private final int mRequestCode;

        private ConfirmStripeIntentCallback(
                @NonNull Activity activity,
                @NonNull ApiRequest.Options requestOptions,
                @NonNull PaymentController paymentController,
                int requestCode) {
            mActivityRef = new WeakReference<>(activity);
            mRequestOptions = requestOptions;
            mPaymentController = paymentController;
            mRequestCode = requestCode;
        }

        @Override
        public void onSuccess(@NonNull StripeIntent stripeIntent) {
            final Activity activity = mActivityRef.get();
            if (activity != null) {
                mPaymentController.handleNextAction(activity, stripeIntent, mRequestOptions);
            }
        }

        @Override
        public void onError(@NonNull Exception e) {
            final Activity activity = mActivityRef.get();
            if (activity != null) {
                handleError(activity, mRequestCode, e);
            }
        }
    }

    static final class Stripe3ds2AuthCallback
            implements ApiResultCallback<Stripe3ds2AuthResult> {
        @NonNull private final WeakReference<Activity> mActivityRef;
        @NonNull private final StripeApiHandler mApiHandler;
        @NonNull private final Transaction mTransaction;
        private final int mMaxTimeout;
        @NonNull private final StripeIntent mStripeIntent;
        @NonNull private final String mSourceId;
        @NonNull private final ApiRequest.Options mRequestOptions;
        @NonNull private final PaymentRelayStarter mPaymentRelayStarter;
        @NonNull private final Handler mBackgroundHandler;
        @NonNull private final FireAndForgetRequestExecutor mAnalyticsRequestExecutor;
        @NonNull private final AnalyticsDataFactory mAnalyticsDataFactory;

        private Stripe3ds2AuthCallback(
                @NonNull Activity activity,
                @NonNull StripeApiHandler apiHandler,
                @NonNull Transaction transaction,
                int maxTimeout,
                @NonNull StripeIntent stripeIntent,
                @NonNull String sourceId,
                @NonNull ApiRequest.Options requestOptions,
                @NonNull FireAndForgetRequestExecutor analyticsRequestExecutor,
                @NonNull AnalyticsDataFactory analyticsDataFactory) {
            this(activity, apiHandler, transaction, maxTimeout, stripeIntent,
                    sourceId, requestOptions,
                    new PaymentRelayStarter(activity, getRequestCode(stripeIntent)),
                    analyticsRequestExecutor,
                    analyticsDataFactory);
        }

        @VisibleForTesting
        Stripe3ds2AuthCallback(
                @NonNull Activity activity,
                @NonNull StripeApiHandler apiHandler,
                @NonNull Transaction transaction,
                int maxTimeout,
                @NonNull StripeIntent stripeIntent,
                @NonNull String sourceId,
                @NonNull ApiRequest.Options requestOptions,
                @NonNull PaymentRelayStarter paymentRelayStarter,
                @NonNull FireAndForgetRequestExecutor analyticsRequestExecutor,
                @NonNull AnalyticsDataFactory analyticsDataFactory) {
            mActivityRef = new WeakReference<>(activity);
            mApiHandler = apiHandler;
            mTransaction = transaction;
            mMaxTimeout = maxTimeout;
            mStripeIntent = stripeIntent;
            mSourceId = sourceId;
            mRequestOptions = requestOptions;
            mPaymentRelayStarter = paymentRelayStarter;
            mAnalyticsRequestExecutor = analyticsRequestExecutor;
            mAnalyticsDataFactory = analyticsDataFactory;

            // create Handler to notifyCompletion challenge flow on background thread
            final HandlerThread handlerThread =
                    new HandlerThread(Stripe3ds2AuthCallback.class.getSimpleName());
            handlerThread.start();
            mBackgroundHandler = new Handler(handlerThread.getLooper());
        }

        @Override
        public void onSuccess(@NonNull Stripe3ds2AuthResult result) {
            final Activity activity = mActivityRef.get();
            if (activity == null) {
                return;
            }

            final Stripe3ds2AuthResult.Ares ares = result.ares;
            if (ares != null) {
                if (ares.shouldChallenge()) {
                    startChallengeFlow(activity, ares);
                } else {
                    startFrictionlessFlow();
                }
            } else if (result.fallbackRedirectUrl != null) {
                beginWebAuth(activity, getRequestCode(mStripeIntent),
                        Objects.requireNonNull(mStripeIntent.getClientSecret()),
                        result.fallbackRedirectUrl, null);
            } else {
                final Stripe3ds2AuthResult.ThreeDS2Error error = result.error;
                final String errorMessage;
                if (error != null) {
                    errorMessage = "Code: " + error.errorCode +
                            ", Detail: " + error.errorDetail +
                            ", Description: " + error.errorDescription +
                            ", Component: " + error.errorComponent;
                } else {
                    errorMessage = "Invalid 3DS2 authentication response";
                }

                onError(new RuntimeException(
                        "Error encountered during 3DS2 authentication request. " + errorMessage));
            }
        }

        @Override
        public void onError(@NonNull Exception e) {
            final Activity activity = mActivityRef.get();
            if (activity != null) {
                mPaymentRelayStarter.start(new PaymentRelayStarter.Data(e));
            }
        }

        private void startFrictionlessFlow() {
            mAnalyticsRequestExecutor.executeAsync(
                    ApiRequest.createAnalyticsRequest(
                            mAnalyticsDataFactory.createAuthParams(
                                    AnalyticsDataFactory.EventName.AUTH_3DS2_FRICTIONLESS,
                                    StripeTextUtils.emptyIfNull(mStripeIntent.getId()),
                                    mRequestOptions.apiKey),
                            mRequestOptions,
                            null
                    )
            );
            mPaymentRelayStarter.start(new PaymentRelayStarter.Data(mStripeIntent));
        }

        private void startChallengeFlow(@NonNull final Activity activity,
                                        @NonNull Stripe3ds2AuthResult.Ares ares) {
            final StripeChallengeParameters challengeParameters =
                    new StripeChallengeParameters();
            challengeParameters.setAcsSignedContent(ares.acsSignedContent);
            challengeParameters.set3DSServerTransactionID(ares.threeDSServerTransId);
            challengeParameters.setAcsTransactionID(ares.acsTransId);

            mBackgroundHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mTransaction.doChallenge(activity,
                            challengeParameters,
                            PaymentAuth3ds2ChallengeStatusReceiver.create(activity, mApiHandler,
                                    mStripeIntent, mSourceId, mRequestOptions,
                                    mAnalyticsRequestExecutor, mAnalyticsDataFactory,
                                    mTransaction),
                            mMaxTimeout);
                }
            }, TimeUnit.SECONDS.toMillis(2));
        }
    }

    static final class PaymentAuth3ds2ChallengeStatusReceiver
            extends StripeChallengeStatusReceiver {
        private static final String VALUE_YES = "Y";

        @NonNull private final WeakReference<Activity> mActivityRef;
        @NonNull private final AuthActivityStarter<Stripe3ds2CompletionStarter.StartData> mStarter;
        @NonNull private final StripeApiHandler mApiHandler;
        @NonNull private final StripeIntent mStripeIntent;
        @NonNull private final String mSourceId;
        @NonNull private final ApiRequest.Options mRequestOptions;
        @NonNull private final FireAndForgetRequestExecutor mAnalyticsRequestExecutor;
        @NonNull private final AnalyticsDataFactory mAnalyticsDataFactory;
        @NonNull private final Transaction mTransaction;

        @NonNull
        static PaymentAuth3ds2ChallengeStatusReceiver create(
                @NonNull Activity activity,
                @NonNull StripeApiHandler apiHandler,
                @NonNull StripeIntent stripeIntent,
                @NonNull String sourceId,
                @NonNull ApiRequest.Options requestOptions,
                @NonNull FireAndForgetRequestExecutor analyticsRequestExecutor,
                @NonNull AnalyticsDataFactory analyticsDataFactory,
                @NonNull Transaction transaction) {
            return new PaymentAuth3ds2ChallengeStatusReceiver(
                    activity,
                    new Stripe3ds2CompletionStarter(activity, getRequestCode(stripeIntent)),
                    apiHandler,
                    stripeIntent,
                    sourceId,
                    requestOptions,
                    analyticsRequestExecutor,
                    analyticsDataFactory,
                    transaction);
        }

        PaymentAuth3ds2ChallengeStatusReceiver(
                @NonNull Activity activity,
                @NonNull AuthActivityStarter<Stripe3ds2CompletionStarter.StartData> starter,
                @NonNull StripeApiHandler apiHandler,
                @NonNull StripeIntent stripeIntent,
                @NonNull String sourceId,
                @NonNull ApiRequest.Options requestOptions,
                @NonNull FireAndForgetRequestExecutor analyticsRequestExecutor,
                @NonNull AnalyticsDataFactory analyticsDataFactory,
                @NonNull Transaction transaction) {
            mActivityRef = new WeakReference<>(activity);
            mStarter = starter;
            mApiHandler = apiHandler;
            mStripeIntent = stripeIntent;
            mSourceId = sourceId;
            mRequestOptions = requestOptions;
            mAnalyticsRequestExecutor = analyticsRequestExecutor;
            mAnalyticsDataFactory = analyticsDataFactory;
            mTransaction = transaction;
        }

        @Override
        public void completed(@NonNull CompletionEvent completionEvent,
                              @NonNull String uiTypeCode) {
            super.completed(completionEvent, uiTypeCode);
            mAnalyticsRequestExecutor.executeAsync(
                    ApiRequest.createAnalyticsRequest(
                            mAnalyticsDataFactory.create3ds2ChallengeParams(
                                    AnalyticsDataFactory.EventName.AUTH_3DS2_CHALLENGE_COMPLETED,
                                    StripeTextUtils.emptyIfNull(mStripeIntent.getId()),
                                    uiTypeCode,
                                    mRequestOptions.apiKey
                            ),
                            mRequestOptions,
                            null
                    )
            );
            notifyCompletion(new Stripe3ds2CompletionStarter.StartData(mStripeIntent,
                    VALUE_YES.equals(completionEvent.getTransactionStatus()) ?
                            Stripe3ds2CompletionStarter.ChallengeFlowOutcome.COMPLETE_SUCCESSFUL :
                            Stripe3ds2CompletionStarter.ChallengeFlowOutcome.COMPLETE_UNSUCCESSFUL
            ));
        }

        @Override
        public void cancelled(@NonNull String uiTypeCode) {
            super.cancelled(uiTypeCode);
            mAnalyticsRequestExecutor.executeAsync(
                    ApiRequest.createAnalyticsRequest(
                            mAnalyticsDataFactory.create3ds2ChallengeParams(
                                    AnalyticsDataFactory.EventName.AUTH_3DS2_CHALLENGE_CANCELED,
                                    StripeTextUtils.emptyIfNull(mStripeIntent.getId()),
                                    uiTypeCode,
                                    mRequestOptions.apiKey
                            ),
                            mRequestOptions,
                            null
                    )
            );
            notifyCompletion(new Stripe3ds2CompletionStarter.StartData(mStripeIntent,
                    Stripe3ds2CompletionStarter.ChallengeFlowOutcome.CANCEL));
        }

        @Override
        public void timedout(@NonNull String uiTypeCode) {
            super.timedout(uiTypeCode);
            mAnalyticsRequestExecutor.executeAsync(
                    ApiRequest.createAnalyticsRequest(
                            mAnalyticsDataFactory.create3ds2ChallengeParams(
                                    AnalyticsDataFactory.EventName.AUTH_3DS2_CHALLENGE_TIMEDOUT,
                                    StripeTextUtils.emptyIfNull(mStripeIntent.getId()),
                                    uiTypeCode,
                                    mRequestOptions.apiKey
                            ),
                            mRequestOptions,
                            null
                    )
            );
            notifyCompletion(new Stripe3ds2CompletionStarter.StartData(mStripeIntent,
                    Stripe3ds2CompletionStarter.ChallengeFlowOutcome.TIMEOUT));
        }

        @Override
        public void protocolError(@NonNull ProtocolErrorEvent protocolErrorEvent) {
            super.protocolError(protocolErrorEvent);
            mAnalyticsRequestExecutor.executeAsync(
                    ApiRequest.createAnalyticsRequest(
                            mAnalyticsDataFactory.create3ds2ChallengeErrorParams(
                                    StripeTextUtils.emptyIfNull(mStripeIntent.getId()),
                                    protocolErrorEvent,
                                    mRequestOptions.apiKey
                            ),
                            mRequestOptions,
                            null
                    )
            );
            notifyCompletion(new Stripe3ds2CompletionStarter.StartData(mStripeIntent,
                    Stripe3ds2CompletionStarter.ChallengeFlowOutcome.PROTOCOL_ERROR));
        }

        @Override
        public void runtimeError(@NonNull RuntimeErrorEvent runtimeErrorEvent) {
            super.runtimeError(runtimeErrorEvent);
            mAnalyticsRequestExecutor.executeAsync(
                    ApiRequest.createAnalyticsRequest(
                            mAnalyticsDataFactory.create3ds2ChallengeErrorParams(
                                    StripeTextUtils.emptyIfNull(mStripeIntent.getId()),
                                    runtimeErrorEvent,
                                    mRequestOptions.apiKey
                            ),
                            mRequestOptions,
                            null
                    )
            );
            notifyCompletion(new Stripe3ds2CompletionStarter.StartData(mStripeIntent,
                    Stripe3ds2CompletionStarter.ChallengeFlowOutcome.RUNTIME_ERROR));
        }

        private void notifyCompletion(
                @NonNull final Stripe3ds2CompletionStarter.StartData startData) {
            mAnalyticsRequestExecutor.executeAsync(
                    ApiRequest.createAnalyticsRequest(
                            mAnalyticsDataFactory.create3ds2ChallengeParams(
                                    AnalyticsDataFactory.EventName.AUTH_3DS2_CHALLENGE_PRESENTED,
                                    StripeTextUtils.emptyIfNull(mStripeIntent.getId()),
                                    StripeTextUtils.emptyIfNull(
                                            mTransaction.getInitialChallengeUiType()),
                                    mRequestOptions.apiKey
                            ),
                            mRequestOptions,
                            null
                    )
            );

            mApiHandler.complete3ds2Auth(mSourceId, mRequestOptions,
                    new ApiResultCallback<Boolean>() {
                        @Override
                        public void onSuccess(@NonNull Boolean result) {
                            mStarter.start(startData);
                        }

                        @Override
                        public void onError(@NonNull Exception e) {
                            final Activity activity = mActivityRef.get();
                            if (activity != null) {
                                handleError(activity, getRequestCode(mStripeIntent), e);
                            }
                        }
                    });
        }
    }
}
