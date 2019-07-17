package com.stripe.android;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.InvalidRequestException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class StripeFireAndForgetRequestExecutorTest {

    // Test to verify fingerprint endpoint's success
    @Test
    public void execute_withFingerprintRequest_shouldReturnSuccessfully()
            throws InvalidRequestException, APIConnectionException {
        final TelemetryClientUtil telemetryClientUtil =
                new TelemetryClientUtil(ApplicationProvider.getApplicationContext());
        final int responseCode = new StripeFireAndForgetRequestExecutor().execute(
                new FingerprintRequest(telemetryClientUtil.createTelemetryMap(),
                        UUID.randomUUID().toString()));
        assertEquals(200, responseCode);
    }
}
