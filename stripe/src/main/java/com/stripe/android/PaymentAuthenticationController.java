package com.stripe.android;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.stripe.android.exception.StripeException;
import com.stripe.android.model.PaymentIntent;
import com.stripe.android.model.PaymentIntentParams;
import com.stripe.android.view.PaymentAuthenticationExtras;

import java.lang.ref.WeakReference;

/**
 * A controller responsible for authenticating payment (typically through resolving any required
 * customer action). The payment authentication mechanism (e.g. 3DS) will be determined by the
 * {@link PaymentIntent} object.
 */
class PaymentAuthenticationController {
    static final int REQUEST_CODE = 50000;
    private static final String TAG = "PaymentAuthentication";

    /**
     * Confirm the PaymentIntent and resolve any next actions
     */
    void confirmAndAuth(@NonNull Stripe stripe,
                        @NonNull Activity activity,
                        @NonNull PaymentIntentParams paymentIntentParams,
                        @NonNull String publishableKey) {
        final WeakReference<Activity> activityRef = new WeakReference<>(activity);
        new ConfirmPaymentIntentTask(stripe, activity, paymentIntentParams, publishableKey,
                new ApiResultCallback<PaymentIntent>() {
                    @Override
                    public void onSuccess(@NonNull PaymentIntent paymentIntent) {
                        handleNextAction(activityRef.get(), paymentIntent);
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
                          @NonNull PaymentIntent paymentIntent) {
        if (paymentIntent.requiresAction()) {
            begin3ds1Auth(activity, paymentIntent.getRedirectData());
        } else {
            new PaymentAuthBypassStarter(activity, REQUEST_CODE).start(paymentIntent);
        }
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
}
