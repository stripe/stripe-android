package com.stripe.android;

import android.support.annotation.NonNull;

import com.stripe.android.exception.InvalidRequestException;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertThrows;

public class RequestExecutorTest {

    @Test
    public void getOutputBytes_shouldHandleUnsupportedEncodingException() {
        final StripeRequest stripeRequest = new StripeRequest(
                StripeRequest.Method.POST,
                ApiRequest.API_HOST, null,
                ApiRequest.MIME_TYPE) {
            @NonNull
            @Override
            Map<String, String> getHeaders() {
                return new HashMap<>();
            }

            @NonNull
            @Override
            byte[] getOutputBytes() throws UnsupportedEncodingException {
                throw new UnsupportedEncodingException();
            }
        };

        final RequestExecutor.ConnectionFactory connectionFactory =
                new RequestExecutor.ConnectionFactory();
        assertThrows(InvalidRequestException.class,
                new ThrowingRunnable() {
                    @Override
                    public void run() throws Throwable {
                        connectionFactory.getRequestOutputBytes(stripeRequest);
                    }
                });
    }
}
