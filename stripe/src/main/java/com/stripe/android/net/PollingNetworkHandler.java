package com.stripe.android.net;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Source;

/**
 * Class to handle polling on a background thread.
 */
class PollingNetworkHandler {

    private static final int SUCCESS = 1;
    private static final int PENDING = 2;
    private static final int FAILURE = 3;
    private static final int ERROR = -1;
    private static final int EXPIRED = -2;

    private final String mClientSecret;
    private final String mPublishableKey;
    private final String mSourceId;

    private final long mTimeoutMs;
    private final boolean mIsInSingleThreadMode;
    private final PollingParameters mPollingParameters;

    private Handler mNetworkHandler;
    private Handler mUiHandler;

    @Nullable private Source mLatestRetrievedSource;
    private int mRetryCount;
    @NonNull private SourceRetriever mSourceRetriever;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            Message message = null;
            try {
                Source source = mSourceRetriever.retrieveSource(
                        mSourceId,
                        mClientSecret,
                        mPublishableKey);
                mLatestRetrievedSource = source;
                if (source.getStatus() != null) {
                    switch (source.getStatus()) {
                        case Source.PENDING:
                            message = mUiHandler.obtainMessage(PENDING);
                            break;
                        case Source.CHARGEABLE:
                            message = mUiHandler.obtainMessage(SUCCESS, source);
                            break;
                        case Source.CONSUMED:
                            message = mUiHandler.obtainMessage(SUCCESS, source);
                            break;
                        case Source.CANCELED:
                            message = mUiHandler.obtainMessage(FAILURE, source);
                            break;
                        case Source.FAILED:
                            message = mUiHandler.obtainMessage(FAILURE, source);
                            break;
                    }
                }
            } catch (StripeException stripeEx) {
                message = mUiHandler.obtainMessage(ERROR, stripeEx);
            } finally {
                if (message != null) {
                    mUiHandler.sendMessage(message);
                }
            }
        }
    };

    PollingNetworkHandler(@NonNull final String sourceId,
                          @NonNull final String clientSecret,
                          @NonNull final String publishableKey,
                          @NonNull final PollingResponseHandler callback,
                          @Nullable Integer timeOutMs,
                          @Nullable SourceRetriever sourceRetriever,
                          @NonNull final PollingParameters pollingParameters) {

        mSourceId = sourceId;
        mClientSecret = clientSecret;
        mPublishableKey = publishableKey;
        mIsInSingleThreadMode = sourceRetriever != null;
        mPollingParameters = pollingParameters;
        mSourceRetriever = sourceRetriever == null
                ? new SourceRetriever() {
                    @Override
                    public Source retrieveSource(
                            @NonNull String sourceId,
                            @NonNull String clientSecret,
                            @NonNull String publishableKey) throws StripeException {
                        return StripeApiHandler.retrieveSource(
                                sourceId,
                                clientSecret,
                                publishableKey);
                    }
                }
                : sourceRetriever;

        mTimeoutMs = timeOutMs == null
                ? mPollingParameters.getDefaultTimeoutMs()
                : Math.min(timeOutMs.longValue(), mPollingParameters.getMaxTimeoutMs());

        mRetryCount = 0;

        mUiHandler = new Handler(Looper.getMainLooper()) {
            int delayMs = mPollingParameters.getInitialDelayMsInt();
            boolean terminated = false;

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (terminated) {
                    return;
                }

                switch (msg.what) {
                    case SUCCESS:
                        terminated = true;
                        callback.onPollingResponse(
                                new PollingResponse((Source) msg.obj, true, false));
                        removeCallbacksAndMessages(null);
                        break;
                    case PENDING:
                        mRetryCount = 0;
                        delayMs = mPollingParameters.getInitialDelayMsInt();
                        mNetworkHandler.sendEmptyMessage(delayMs);
                        break;
                    case FAILURE:
                        terminated = true;
                        callback.onPollingResponse(
                                new PollingResponse((Source) msg.obj, false, false));
                        removeCallbacksAndMessages(null);
                        break;
                    case EXPIRED:
                        terminated = true;
                        callback.onPollingResponse(
                                new PollingResponse(mLatestRetrievedSource, false, true));
                        removeCallbacksAndMessages(null);
                        break;
                    case ERROR:
                        mRetryCount++;
                        if (mRetryCount >= mPollingParameters.getMaxRetryCount()) {
                            terminated = true;
                            callback.onPollingResponse(
                                    new PollingResponse(mLatestRetrievedSource,
                                            (StripeException) msg.obj));
                            removeCallbacksAndMessages(null);
                        } else {
                            // We get this case for 500-errors
                            delayMs = Math.min(
                                    delayMs * mPollingParameters.getPollingMultiplier(),
                                    (int) mPollingParameters.getMaxDelayMs());
                            mNetworkHandler.sendEmptyMessage(delayMs);
                        }
                        break;
                    default:
                        break;
                }
            }
        };

        HandlerThread handlerThread = null;
        if (!mIsInSingleThreadMode) {
            handlerThread = new HandlerThread("Network Handler Thread");
            handlerThread.start();
        }

        final Looper looper = mIsInSingleThreadMode
                ? Looper.getMainLooper()
                : handlerThread.getLooper();
        mNetworkHandler = new Handler(looper) {
            boolean terminated = false;

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (terminated) {
                    return;
                }

                if (msg.what >= 0) {
                    postDelayed(pollRunnable, msg.what);
                } else {
                    terminated = true;
                    if (!mIsInSingleThreadMode) {
                        looper.quit();
                    }
                    mUiHandler.removeMessages(SUCCESS);
                    mUiHandler.removeMessages(PENDING);
                    mUiHandler.removeMessages(FAILURE);
                    removeCallbacks(pollRunnable);
                }
            }
        };
    }

    @VisibleForTesting
    long getTimeoutMs() {
        return mTimeoutMs;
    }

    @VisibleForTesting
    int getRetryCount() {
        return mRetryCount;
    }

    @VisibleForTesting
    void setSourceRetriever(@NonNull SourceRetriever sourceRetriever) {
        mSourceRetriever = sourceRetriever;
    }

    void start() {
        mNetworkHandler.post(pollRunnable);
        mNetworkHandler.sendEmptyMessageDelayed(EXPIRED, mTimeoutMs);
        mUiHandler.sendEmptyMessageDelayed(EXPIRED, mTimeoutMs);
    }
}
