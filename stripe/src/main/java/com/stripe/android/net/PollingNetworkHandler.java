package com.stripe.android.net;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.APIException;
import com.stripe.android.exception.AuthenticationException;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.exception.PollingFailedException;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceRedirect;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Class to handle polling on a background thread.
 */
public class PollingNetworkHandler {

    private static final int INITIAL_DELAY_MS = 100;

    private final PollingResponseHandler mCallback;
    private final String mClientSecret;
    private final String mPublishableKey;
    private final String mSourceId;

    private final long mTimeoutMs;

    private Handler mNetworkHandler;
    private Handler mUiHandler;

    private boolean mIsPaused;
    private Queue<Message> mMessageQueue = new LinkedBlockingQueue<>();

    final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            Message message = null;
            try {
                Source source = StripeApiHandler.retrieveSource(
                        mSourceId, mClientSecret, mPublishableKey);
                if (source.getRedirect() != null) {
                    switch (source.getRedirect().getStatus()) {
                        case SourceRedirect.SUCCEEDED:
                            message = mUiHandler.obtainMessage(1);
                            break;
                        case SourceRedirect.PENDING:
                            message = mUiHandler.obtainMessage(2);
                            break;
                        case SourceRedirect.FAILED:
                            message = mUiHandler.obtainMessage(3);
                            break;
                    }
                }
            } catch (AuthenticationException authEx) {
                message = mUiHandler.obtainMessage(-1, authEx);
            } catch (InvalidRequestException invalidEx) {
                message = mUiHandler.obtainMessage(-2, invalidEx);
            } catch (APIConnectionException apiConnectionException) {
                message = mUiHandler.obtainMessage(-3, apiConnectionException);
            } catch (APIException apiException) {
                message = mUiHandler.obtainMessage(-4, apiException);
            } finally {
                if (message != null) {
                    if (mIsPaused) {
                        mMessageQueue.add(message);
                    } else {
                        mUiHandler.sendMessage(message);
                    }
                }
            }
        }
    };

    PollingNetworkHandler(@NonNull final String sourceId,
                          @NonNull final String clientSecret,
                          @NonNull final String publishableKey,
                          @NonNull final PollingResponseHandler callback,
                          @Nullable Integer timeOutMs) {
        mSourceId = sourceId;
        mClientSecret = clientSecret;
        mPublishableKey = publishableKey;
        mCallback = callback;

        mTimeoutMs = timeOutMs == null ? 10000L : timeOutMs.longValue();

        HandlerThread handlerThread = new HandlerThread("Network Handler Thread");
        handlerThread.start();

        mUiHandler = new Handler(Looper.getMainLooper()) {
            int delayMs = 100;
            int multiplier = 2;

            int retryCount = 0;
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 0:
                        mNetworkHandler.sendEmptyMessage(0);
                        break;
                    case 1:
                        callback.onSuccess();
                        break;
                    case 2:
                        delayMs *= multiplier;
                        callback.onRetry(++retryCount);
                        mNetworkHandler.sendEmptyMessage(delayMs);
                        break;
                    case 3:
                        callback.onError(new PollingFailedException("failed", false));
                        break;
                    case -55:
                        callback.onError(new PollingFailedException("expired", true));
                        break;
                    default:
                        break;
                }
            }
        };

        mNetworkHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what >= 0) {
                    postDelayed(pollRunnable, msg.what);
                } else {
                    removeCallbacks(pollRunnable);
                }
            }
        };

    }

    public void start() {
        mNetworkHandler.post(pollRunnable);
    }

    public void pause() {
        mIsPaused = true;
    }

    public void resume() {
        mIsPaused = false;
        if (!mMessageQueue.isEmpty()) {
            mUiHandler.sendMessage(mMessageQueue.poll());
        }
    }
}
