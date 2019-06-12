package com.stripe.android;

import android.support.annotation.NonNull;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.InvalidRequestException;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

@RunWith(RobolectricTestRunner.class)
public class RequestExecutorTest {

    // Test to verify fingerprint endpoint's success
    @Test
    public void executeAndForget_withFingerprintRequest_shouldReturnSuccessfully()
            throws InvalidRequestException, APIConnectionException {
        final TelemetryClientUtil telemetryClientUtil =
                new TelemetryClientUtil(ApplicationProvider.getApplicationContext());
        final int responseCode = new RequestExecutor().executeAndForget(
                new FingerprintRequest(telemetryClientUtil.createTelemetryMap(),
                        UUID.randomUUID().toString()));
        assertEquals(200, responseCode);
    }

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
