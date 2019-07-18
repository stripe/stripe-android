package com.stripe.android;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.exception.StripeException;

import java.io.IOException;
import java.net.HttpURLConnection;

final class StripeFireAndForgetRequestExecutor implements FireAndForgetRequestExecutor {

    @NonNull private final ConnectionFactory mConnectionFactory;

    StripeFireAndForgetRequestExecutor() {
        mConnectionFactory = new ConnectionFactory();
    }

    /**
     * Make the request and ignore the response
     *
     * @return the response status code. Used for testing purposes.
     */
    public int execute(@NonNull StripeRequest request)
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
    public void executeAsync(@NonNull final StripeRequest request) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    execute(request);
                } catch (StripeException ignore) {
                }
            }
        });
    }
}
