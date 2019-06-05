package com.stripe.android;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.exception.InvalidRequestException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class FingerprintRequestTest {
    @Test
    public void getContentType() {
        assertEquals("application/json; charset=UTF-8",
                new FingerprintRequest(new HashMap<String, Object>(), "guid")
                        .getContentType());
    }

    @Test
    public void getHeaders() {
        final Map<String, String> headers =
                new FingerprintRequest(new HashMap<String, Object>(), "guid")
                    .getHeaders();
        assertEquals("m=guid", headers.get("Cookie"));
    }

    @Test
    public void getOutputBytes() throws UnsupportedEncodingException, InvalidRequestException {
        final TelemetryClientUtil telemetryClientUtil =
                new TelemetryClientUtil(ApplicationProvider.getApplicationContext());
        final byte[] output = new FingerprintRequest(telemetryClientUtil.createTelemetryMap(),
                "guid")
                .getOutputBytes();
        assertTrue(output.length > 0);
    }
}
