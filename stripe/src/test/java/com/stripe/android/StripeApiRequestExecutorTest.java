package com.stripe.android;

import android.support.annotation.NonNull;

import com.stripe.android.exception.InvalidRequestException;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertThrows;

@RunWith(RobolectricTestRunner.class)
public class StripeApiRequestExecutorTest {
    @Test
    public void getOutputBytes_shouldHandleUnsupportedEncodingException() {
        final StripeRequest stripeRequest = new StripeRequest(
                StripeRequest.Method.POST,
                ApiRequest.API_HOST, null,
                ApiRequest.MIME_TYPE) {
            @NonNull
            @Override
            Map<String, String> createHeaders() {
                return new HashMap<>();
            }

            @NonNull
            @Override
            String getUserAgent() {
                return StripeRequest.DEFAULT_USER_AGENT;
            }

            @NonNull
            @Override
            byte[] getOutputBytes() throws UnsupportedEncodingException {
                throw new UnsupportedEncodingException();
            }
        };

        final ConnectionFactory connectionFactory = new ConnectionFactory();
        assertThrows(InvalidRequestException.class,
                new ThrowingRunnable() {
                    @Override
                    public void run() throws Throwable {
                        connectionFactory.getRequestOutputBytes(stripeRequest);
                    }
                });
    }
}
