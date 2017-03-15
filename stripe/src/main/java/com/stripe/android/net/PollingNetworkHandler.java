package com.stripe.android.net;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.exception.PollingFailedException;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceRedirect;

/**
 * Class to handle polling on a background thread.
 */
public class PollingNetworkHandler {

    private static final String AUTHORIZATION_FAILED = "The redirect authorization request failed.";
    private static final int INITIAL_DELAY_MS = 100;
    private static final String POLLING_EXPIRED = "The polling request has expired.";
    private static final int POLLING_MULTIPLIER = 2;

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
    @NonNull private final SourceRetriever mSourceRetriever;

    private Handler mNetworkHandler;
    private Handler mUiHandler;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            Message message = null;
            try {
                Source source = mSourceRetriever.retrieveSource(
                        mSourceId,
                        mClientSecret,
                        mPublishableKey);
                if (source.getRedirect() != null) {
                    switch (source.getRedirect().getStatus()) {
                        case SourceRedirect.SUCCEEDED:
                            message = mUiHandler.obtainMessage(SUCCESS);
                            break;
                        case SourceRedirect.PENDING:
                            message = mUiHandler.obtainMessage(PENDING);
                            break;
                        case SourceRedirect.FAILED:
                            message = mUiHandler.obtainMessage(FAILURE);
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
                          @Nullable SourceRetriever sourceRetriever) {
        mSourceId = sourceId;
        mClientSecret = clientSecret;
        mPublishableKey = publishableKey;
        mIsInSingleThreadMode = sourceRetriever != null;
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

        mTimeoutMs = timeOutMs == null ? 10000L : timeOutMs.longValue();

        mUiHandler = new Handler(Looper.getMainLooper()) {
            int delayMs = INITIAL_DELAY_MS;
            boolean terminated = false;

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (terminated) {
                    return;
                }

                switch (msg.what) {
                    case SUCCESS:
                        callback.onSuccess();
                        break;
                    case PENDING:
                        delayMs *= POLLING_MULTIPLIER;
                        callback.onRetry(delayMs);
                        mNetworkHandler.sendEmptyMessage(delayMs);
                        break;
                    case FAILURE:
                        terminated = true;
                        callback.onError(new PollingFailedException(AUTHORIZATION_FAILED, false));
                        break;
                    case EXPIRED:
                        terminated = true;
                        callback.onError(new PollingFailedException(POLLING_EXPIRED, true));
                        break;
                    case ERROR:
                        terminated = true;
                        callback.onError((StripeException) msg.obj);
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

    public void start() {
        mNetworkHandler.post(pollRunnable);
        mNetworkHandler.sendEmptyMessageDelayed(EXPIRED, mTimeoutMs);
        mUiHandler.sendEmptyMessageDelayed(EXPIRED, mTimeoutMs);
    }

    interface SourceRetriever {
        Source retrieveSource(
                @NonNull String sourceId,
                @NonNull String clientSecret,
                @NonNull String publishableKey)
                throws StripeException;
    }
}
