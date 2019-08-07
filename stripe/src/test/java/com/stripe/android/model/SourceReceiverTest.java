package com.stripe.android.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Objects;

import static org.junit.Assert.assertEquals;

/**
 * Test class for {@link SourceReceiver}.
 */
public class SourceReceiverTest {

    private static final String EXAMPLE_JSON_RECEIVER = "{" +
            "\"address\": \"test_1MBhWS3uv4ynCfQXF3xQjJkzFPukr4K56N\"," +
            "\"amount_charged\": 10," +
            "\"amount_received\": 20," +
            "\"amount_returned\": 30" +
            "}";

    @Test
    public void fromJson_createsExpectedObject() throws JSONException {
        final SourceReceiver sourceReceiver = Objects.requireNonNull(
                        SourceReceiver.fromJson(new JSONObject(EXAMPLE_JSON_RECEIVER)));
        assertEquals("test_1MBhWS3uv4ynCfQXF3xQjJkzFPukr4K56N",
                sourceReceiver.getAddress());
        assertEquals(10, sourceReceiver.getAmountCharged());
        assertEquals(20, sourceReceiver.getAmountReceived());
        assertEquals(30, sourceReceiver.getAmountReturned());
    }
}
