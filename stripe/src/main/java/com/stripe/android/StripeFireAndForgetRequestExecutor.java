package com.stripe.android;

import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.InvalidRequestException;

import java.io.IOException;
import java.net.HttpURLConnection;

final class StripeFireAndForgetRequestExecutor implements FireAndForgetRequestExecutor {

    @NonNull private final ConnectionFactory mConnectionFactory;
    @NonNull private final Handler mHandler;

    StripeFireAndForgetRequestExecutor() {
        mConnectionFactory = new ConnectionFactory();

        final HandlerThread handlerThread = new HandlerThread(
                StripeFireAndForgetRequestExecutor.class.getSimpleName());
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
    }

    /**
     * Make the request and ignore the response
     *
     * @return the response status code. Used for testing purposes.
     */
    @VisibleForTesting
    int execute(@NonNull StripeRequest request)
            throws APIConnectionException, InvalidRequestException {
        // HttpURLConnection verifies SSL cert by default
        HttpURLConnection conn = null;
        try {
            conn = mConnectionFactory.create(request);
            // required to trigger the request
            return conn.getResponseCode();
        } catch (IOException e) {
            throw APIConnectionException.create(request.getBaseUrl(), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void executeAsync(@NonNull final StripeRequest request) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    execute(request);
                } catch (Exception ignore) {
                }
            }
        });
    }
}
