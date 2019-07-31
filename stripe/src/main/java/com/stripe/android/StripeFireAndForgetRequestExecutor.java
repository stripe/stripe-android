package com.stripe.android;

import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.InvalidRequestException;

import java.io.Closeable;
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
            final int responseCode = conn.getResponseCode();

            closeConnection(conn, responseCode);
            return responseCode;
        } catch (IOException e) {
            throw APIConnectionException.create(request.getBaseUrl(), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void closeConnection(@NonNull HttpURLConnection conn, int responseCode)
            throws IOException {
        if (responseCode >= 200 && responseCode < 300) {
            closeStream(conn.getInputStream());
        } else {
            closeStream(conn.getErrorStream());
        }
    }

    private void closeStream(@Nullable Closeable stream) throws IOException {
        if (stream != null) {
            stream.close();
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
