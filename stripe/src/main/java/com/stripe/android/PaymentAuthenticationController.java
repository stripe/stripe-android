package com.stripe.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.stripe.android.exception.StripeException;
import com.stripe.android.model.PaymentIntent;
import com.stripe.android.model.PaymentIntentParams;
import com.stripe.android.model.Stripe3ds2Fingerprint;
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
import com.stripe.android.view.ActivityStarter;
import com.stripe.android.view.PaymentAuthenticationExtras;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * A controller responsible for authenticating payment (typically through resolving any required
 * customer action). The payment authentication mechanism (e.g. 3DS) will be determined by the
 * {@link PaymentIntent} object.
 */
class PaymentAuthenticationController {
    static final int REQUEST_CODE = 50000;
    private static final String TAG = "PaymentAuthentication";
    private static final String DIRECTORY_SERVER_ID = "F000000000";

    @NonNull private final StripeThreeDs2Service mThreeDs2Service;
    @NonNull private final StripeApiHandler mApiHandler;
    @NonNull private final MessageVersionRegistry mMessageVersionRegistry;
    @NonNull private final String mDirectoryServerId;
    @NonNull private final PaymentAuthConfig mConfig;

    PaymentAuthenticationController(@NonNull Context context,
                                    @NonNull StripeApiHandler apiHandler) {
        this(context, new StripeThreeDs2ServiceImpl(context), apiHandler,
                new MessageVersionRegistry(), DIRECTORY_SERVER_ID,
                PaymentAuthConfig.get());
    }

    @VisibleForTesting
    PaymentAuthenticationController(@NonNull Context context,
                                    @NonNull StripeThreeDs2Service threeDs2Service,
                                    @NonNull StripeApiHandler apiHandler,
                                    @NonNull MessageVersionRegistry messageVersionRegistry,
                                    @NonNull String directoryServerId,
                                    @NonNull PaymentAuthConfig config) {

        mConfig = config;
        mThreeDs2Service = threeDs2Service;
        mThreeDs2Service.initialize(context, new StripeConfigParameters(), null,
                config.stripe3ds2Config.uiCustomization);
        mApiHandler = apiHandler;
        mMessageVersionRegistry = messageVersionRegistry;
        mDirectoryServerId = directoryServerId;
    }

    /**
     * Confirm the PaymentIntent and resolve any next actions
     */
    void confirmAndAuth(@NonNull Stripe stripe,
                        @NonNull Activity activity,
                        @NonNull PaymentIntentParams paymentIntentParams,
                        @NonNull final String publishableKey) {
        final WeakReference<Activity> activityRef = new WeakReference<>(activity);
        new ConfirmPaymentIntentTask(stripe, activity, paymentIntentParams, publishableKey,
                new ApiResultCallback<PaymentIntent>() {
                    @Override
                    public void onSuccess(@NonNull PaymentIntent paymentIntent) {
                        handleNextAction(activityRef.get(), paymentIntent, publishableKey);
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        Log.e(TAG, "Exception thrown while confirming PaymentIntent", e);
                    }
                })
                .execute();
    }

    /**
     * Decide whether {@link #handleResult(Stripe, Intent, String, ApiResultCallback)} should be
     * called.
     */
    boolean shouldHandleResult(int requestCode, int resultCode, @Nullable Intent data) {
        return requestCode == REQUEST_CODE &&
                resultCode == Activity.RESULT_OK &&
                data != null;
    }

    /**
     * Get the PaymentIntent's client_secret from {@param data} and use to retrieve the
     * PaymentIntent object with updated status
     *
     * @param data the result Intent
     */
    void handleResult(@NonNull Stripe stripe, @NonNull Intent data, @NonNull String publishableKey,
                      @NonNull final ApiResultCallback<PaymentIntent> listener) {
        final String clientSecret = data.getStringExtra(PaymentAuthenticationExtras.CLIENT_SECRET);
        final PaymentIntentParams paymentIntentParams = PaymentIntentParams
                .createRetrievePaymentIntentParams(clientSecret);
        new RetrievePaymentIntentTask(stripe, paymentIntentParams, publishableKey,
                new ApiResultCallback<PaymentIntent>() {
                    @Override
                    public void onSuccess(@NonNull PaymentIntent paymentIntent) {
                        listener.onSuccess(paymentIntent);
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        listener.onError(e);
                    }
                })
                .execute();
    }

    /**
     * Determine which payment authentication mechanism should be used, or bypass authentication
     * if it is not needed.
     */
    @VisibleForTesting
    void handleNextAction(@NonNull Activity activity,
                          @NonNull PaymentIntent paymentIntent,
                          @NonNull String publishableKey) {
        if (paymentIntent.requiresAction()) {
            final PaymentIntent.NextActionType nextActionType = paymentIntent.getNextActionType();
            if (PaymentIntent.NextActionType.UseStripeSdk == nextActionType) {
                final PaymentIntent.SdkData sdkData =
                        Objects.requireNonNull(paymentIntent.getStripeSdkData());
                if (sdkData.is3ds2()) {
                    begin3ds2Auth(activity, paymentIntent,
                            Stripe3ds2Fingerprint.create(sdkData), publishableKey);
                } else {
                    // authentication type is not supported
                    bypassAuth(activity, paymentIntent);
                }
            } else if (PaymentIntent.NextActionType.RedirectToUrl == nextActionType) {
                begin3ds1Auth(activity,
                        Objects.requireNonNull(paymentIntent.getRedirectData()));
            } else {
                // next action type is not supported, so bypass authentication
                bypassAuth(activity, paymentIntent);
            }
        } else {
            // no action required, so bypass authentication
            bypassAuth(activity, paymentIntent);
        }
    }

    private void bypassAuth(@NonNull Activity activity, @NonNull PaymentIntent paymentIntent) {
        new PaymentAuthBypassStarter(activity, REQUEST_CODE).start(paymentIntent);
    }

    private void begin3ds2Auth(@NonNull Activity activity,
                               @NonNull PaymentIntent paymentIntent,
                               @NonNull Stripe3ds2Fingerprint stripe3ds2Fingerprint,
                               @NonNull String publishableKey) {
        final Transaction transaction =
                mThreeDs2Service.createTransaction(mDirectoryServerId,
                        mMessageVersionRegistry.getCurrent(), false);
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
                timeout
        );
        mApiHandler.start3ds2Auth(authParams, publishableKey,
                new Stripe3ds2AuthCallback(activity, transaction, timeout, paymentIntent));
    }

    /**
     * Start in-app WebView activity.
     *
     * @param activity the payment authentication result will be returned as a result to this
     *                 {@link Activity}
     */
    private void begin3ds1Auth(@NonNull Activity activity,
                               @NonNull PaymentIntent.RedirectData redirectData) {
        new PaymentAuthWebViewStarter(activity, REQUEST_CODE).start(redirectData);
    }

    private static final class RetrievePaymentIntentTask
            extends AsyncTask<Void, Void, ResultWrapper<PaymentIntent>> {
        @NonNull private final Stripe mStripe;
        @NonNull private final PaymentIntentParams mParams;
        @NonNull private final String mPublishableKey;
        @NonNull private final ApiResultCallback<PaymentIntent> mListener;

        private RetrievePaymentIntentTask(@NonNull Stripe stripe,
                                          @NonNull PaymentIntentParams params,
                                          @NonNull String publishableKey,
                                          @NonNull ApiResultCallback<PaymentIntent> listener) {
            mStripe = stripe;
            mParams = params;
            mPublishableKey = publishableKey;
            mListener = listener;
        }

        @Override
        protected ResultWrapper<PaymentIntent> doInBackground(Void... voids) {
            try {
                final PaymentIntent paymentIntent =
                        mStripe.retrievePaymentIntentSynchronous(mParams, mPublishableKey);
                return new ResultWrapper<>(paymentIntent);
            } catch (StripeException e) {
                return new ResultWrapper<>(e);
            }
        }

        @Override
        protected void onPostExecute(@NonNull ResultWrapper<PaymentIntent> resultWrapper) {
            final PaymentIntent paymentIntent = resultWrapper.result;
            if (paymentIntent != null) {
                mListener.onSuccess(paymentIntent);
            } else if (resultWrapper.error != null) {
                mListener.onError(resultWrapper.error);
            } else {
                mListener.onError(new RuntimeException(
                        "Somehow got neither a PaymentIntent response or an error response"));
            }
        }
    }

    private static final class ConfirmPaymentIntentTask
            extends AsyncTask<Void, Void, ResultWrapper<PaymentIntent>> {
        @NonNull private final Stripe mStripe;
        @NonNull private final WeakReference<Activity> mActivityRef;
        @NonNull private final PaymentIntentParams mParams;
        @NonNull private final String mPublishableKey;
        @NonNull private final ApiResultCallback<PaymentIntent> mListener;

        private ConfirmPaymentIntentTask(@NonNull Stripe stripe,
                                         @NonNull Activity activity,
                                         @NonNull PaymentIntentParams params,
                                         @NonNull String publishableKey,
                                         @NonNull ApiResultCallback<PaymentIntent> listener) {
            mStripe = stripe;
            mActivityRef = new WeakReference<>(activity);
            mParams = params;
            mPublishableKey = publishableKey;
            mListener = listener;
        }

        @Override
        protected ResultWrapper<PaymentIntent> doInBackground(Void... voids) {
            try {
                final PaymentIntent paymentIntent =
                        mStripe.confirmPaymentIntentSynchronous(mParams, mPublishableKey);
                return new ResultWrapper<>(paymentIntent);
            } catch (StripeException stripeException) {
                return new ResultWrapper<>(stripeException);
            }
        }

        @Override
        protected void onPostExecute(@NonNull ResultWrapper<PaymentIntent> resultWrapper) {
            final PaymentIntent paymentIntent = resultWrapper.result;
            if (paymentIntent != null) {
                final Activity activity = mActivityRef.get();
                if (activity != null) {
                    mListener.onSuccess(paymentIntent);
                } else {
                    mListener.onError(new RuntimeException("Activity has been GCed"));
                }
            } else if (resultWrapper.error != null) {
                mListener.onError(resultWrapper.error);
            } else {
                mListener.onError(new RuntimeException(
                        "Somehow got neither a PaymentIntent response or an error response"));
            }
        }
    }

    private static final class Stripe3ds2AuthCallback
            implements ApiResultCallback<JSONObject> {
        @NonNull private final WeakReference<Activity> mActivityRef;
        @NonNull private final Transaction mTransaction;
        private final int mMaxTimeout;
        @NonNull private final PaymentIntent mPaymentIntent;

        private Stripe3ds2AuthCallback(@NonNull Activity activity,
                                       @NonNull Transaction transaction,
                                       int maxTimeout,
                                       @NonNull PaymentIntent paymentIntent) {
            mActivityRef = new WeakReference<>(activity);
            mTransaction = transaction;
            mMaxTimeout = maxTimeout;
            mPaymentIntent = paymentIntent;
        }

        @Override
        public void onSuccess(@NonNull JSONObject result) {
            final Activity activity = mActivityRef.get();
            if (activity == null) {
                return;
            }

            final JSONObject ares = result.optJSONObject("ares");
            final StripeChallengeParameters challengeParameters = new StripeChallengeParameters();
            challengeParameters.setAcsSignedContent(ares.optString("acsSignedContent"));
            challengeParameters.set3DSServerTransactionID(ares.optString("threeDSServerTransID"));
            challengeParameters.setAcsTransactionID(ares.optString("acsTransID"));
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    mTransaction.doChallenge(activity,
                            challengeParameters,
                            PaymentAuth3ds2ChallengeStatusReceiver.create(activity, mPaymentIntent),
                            mMaxTimeout);
                }
            });
        }

        @Override
        public void onError(@NonNull Exception e) {
        }
    }

    static final class PaymentAuth3ds2ChallengeStatusReceiver
            extends StripeChallengeStatusReceiver {
        @NonNull private final ActivityStarter<Stripe3ds2CompletionStarter.StartData> mStarter;
        @NonNull private final PaymentIntent mPaymentIntent;

        @NonNull
        static PaymentAuth3ds2ChallengeStatusReceiver create(
                @NonNull Activity activity,
                @NonNull PaymentIntent paymentIntent) {
            return new PaymentAuth3ds2ChallengeStatusReceiver(
                    new Stripe3ds2CompletionStarter(activity, REQUEST_CODE),
                    paymentIntent
            );
        }

        PaymentAuth3ds2ChallengeStatusReceiver(
                @NonNull ActivityStarter<Stripe3ds2CompletionStarter.StartData> starter,
                @NonNull PaymentIntent paymentIntent) {
            mStarter = starter;
            mPaymentIntent = paymentIntent;
        }

        @Override
        public void completed(@NonNull CompletionEvent completionEvent) {
            super.completed(completionEvent);
            start(Stripe3ds2CompletionStarter.StartData.createForComplete(mPaymentIntent,
                    completionEvent.getTransactionStatus()));
        }

        @Override
        public void cancelled() {
            super.cancelled();
            start(new Stripe3ds2CompletionStarter.StartData(mPaymentIntent,
                    Stripe3ds2CompletionStarter.Status.CANCEL));
        }

        @Override
        public void timedout() {
            super.timedout();
            start(new Stripe3ds2CompletionStarter.StartData(mPaymentIntent,
                    Stripe3ds2CompletionStarter.Status.TIMEOUT));
        }

        @Override
        public void protocolError(@NonNull ProtocolErrorEvent protocolErrorEvent) {
            super.protocolError(protocolErrorEvent);
            start(new Stripe3ds2CompletionStarter.StartData(mPaymentIntent,
                    Stripe3ds2CompletionStarter.Status.PROTOCOL_ERROR));
        }

        @Override
        public void runtimeError(@NonNull RuntimeErrorEvent runtimeErrorEvent) {
            super.runtimeError(runtimeErrorEvent);
            start(new Stripe3ds2CompletionStarter.StartData(mPaymentIntent,
                    Stripe3ds2CompletionStarter.Status.RUNTIME_ERROR));
        }

        private void start(@NonNull Stripe3ds2CompletionStarter.StartData startData) {
            mStarter.start(startData);
        }
    }
}
