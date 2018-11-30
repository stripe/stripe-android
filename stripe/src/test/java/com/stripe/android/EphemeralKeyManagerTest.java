package com.stripe.android;

import com.stripe.android.testharness.TestEphemeralKeyProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test class for {@link EphemeralKeyManager}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class EphemeralKeyManagerTest {

    private static final String FIRST_SAMPLE_KEY_RAW = "{\n" +
            "  \"id\": \"ephkey_123\",\n" +
            "  \"object\": \"ephemeral_key\",\n" +
            "  \"secret\": \"ek_test_123\",\n" +
            "  \"created\": 1501199335,\n" +
            "  \"livemode\": false,\n" +
            "  \"expires\": 1501199335,\n" +
            "  \"associated_objects\": [{\n" +
            "            \"type\": \"customer\",\n" +
            "            \"id\": \"cus_AQsHpvKfKwJDrF\"\n" +
            "            }]\n" +
            "}";

    private static final long TEST_SECONDS_BUFFER = 10L;

    @Mock EphemeralKeyManager.KeyManagerListener<CustomerEphemeralKey> mKeyManagerListener;

    private TestEphemeralKeyProvider mTestEphemeralKeyProvider;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mTestEphemeralKeyProvider = new TestEphemeralKeyProvider();
    }

    @Test
    public void shouldRefreshKey_whenKeyIsNullAndTimeIsInFuture_returnsTrue() {
        Calendar futureCalendar = Calendar.getInstance();
        futureCalendar.add(Calendar.YEAR, 1);
        // If you don't call getTime or getTimeInMillis on a Calendar, none of the updates happen.
        futureCalendar.getTimeInMillis();
        assertTrue(EphemeralKeyManager.shouldRefreshKey(null,
                TEST_SECONDS_BUFFER,
                futureCalendar));
    }

    @Test
    public void shouldRefreshKey_whenKeyIsNullAndTimeIsInPast_returnsTrue() {
        Calendar pastCalendar = Calendar.getInstance();
        pastCalendar.add(Calendar.YEAR, -1);
        // If you don't call getTime or getTimeInMillis on a Calendar, none of the updates happen.
        pastCalendar.getTimeInMillis();
        assertTrue(EphemeralKeyManager.shouldRefreshKey(null,
                TEST_SECONDS_BUFFER,
                pastCalendar));
    }

    @Test
    public void shouldRefreshKey_whenKeyExpiryIsAfterBufferFromPresent_returnsFalse() {
        Calendar fixedCalendar = Calendar.getInstance();
        CustomerEphemeralKey key = CustomerEphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(key);

        long expiryTimeInSeconds = key.getExpires();
        long sufficientBufferTime = 2*TEST_SECONDS_BUFFER;
        key.setExpires(expiryTimeInSeconds + sufficientBufferTime);

        long expiryTimeInMilliseconds = TimeUnit.SECONDS.toMillis(expiryTimeInSeconds);
        fixedCalendar.setTimeInMillis(expiryTimeInMilliseconds);
        // If you don't call getTime or getTimeInMillis on a Calendar, none of the updates happen.
        assertEquals(expiryTimeInMilliseconds, fixedCalendar.getTimeInMillis());
        assertFalse(EphemeralKeyManager.shouldRefreshKey(key,
                TEST_SECONDS_BUFFER,
                fixedCalendar));
    }

    @Test
    public void shouldRefreshKey_whenKeyExpiryIsInThePast_returnsTrue() {
        Calendar fixedCalendar = Calendar.getInstance();
        CustomerEphemeralKey key = CustomerEphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(key);

        long currentTimeInMillis = fixedCalendar.getTimeInMillis();
        long timeAgoInMillis = currentTimeInMillis - 100L;

        key.setExpires(TimeUnit.MILLISECONDS.toSeconds(timeAgoInMillis));
        assertTrue(EphemeralKeyManager.shouldRefreshKey(key,
                TEST_SECONDS_BUFFER,
                fixedCalendar));
    }

    @Test
    public void shouldRefreshKey_whenKeyExpiryIsInFutureButWithinBuffer_returnsTrue() {
        Calendar fixedCalendar = Calendar.getInstance();
        CustomerEphemeralKey key = CustomerEphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(key);

        long parsedExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(key.getExpires());
        long bufferTimeInMillis = TimeUnit.SECONDS.toMillis(TEST_SECONDS_BUFFER);

        long notFarEnoughInTheFuture = parsedExpiryTimeInMillis + bufferTimeInMillis / 2;
        fixedCalendar.setTimeInMillis(notFarEnoughInTheFuture);
        assertEquals(notFarEnoughInTheFuture, fixedCalendar.getTimeInMillis());

        assertTrue(EphemeralKeyManager.shouldRefreshKey(key,
                TEST_SECONDS_BUFFER,
                fixedCalendar));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void createKeyManager_updatesEphemeralKey_notifiesListener() {
        CustomerEphemeralKey testKey = CustomerEphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(testKey);

        mTestEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        EphemeralKeyManager<CustomerEphemeralKey> keyManager = new EphemeralKeyManager(
                mTestEphemeralKeyProvider,
                mKeyManagerListener,
                TEST_SECONDS_BUFFER,
                null,
                CustomerEphemeralKey.class);

        verify(mKeyManagerListener, times(1)).onKeyUpdate(
                any(CustomerEphemeralKey.class), (String) isNull(), (Map<String, Object>) isNull());
        assertNotNull(keyManager.getEphemeralKey());
        assertEquals(testKey.getId(), keyManager.getEphemeralKey().getId());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void retrieveEphemeralKey_whenUpdateNecessary_returnsUpdateAndArguments() {
        CustomerEphemeralKey testKey = CustomerEphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(testKey);

        Calendar fixedCalendar = Calendar.getInstance();

        long currentTimeInMillis = fixedCalendar.getTimeInMillis();
        long timeAgoInMillis = currentTimeInMillis - 100L;

        testKey.setExpires(TimeUnit.MILLISECONDS.toSeconds(timeAgoInMillis));
        mTestEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);

        EphemeralKeyManager<CustomerEphemeralKey> keyManager = new EphemeralKeyManager(
                mTestEphemeralKeyProvider,
                mKeyManagerListener,
                TEST_SECONDS_BUFFER,
                fixedCalendar,
                CustomerEphemeralKey.class);

        // We already tested this setup, so let's reset the mock to avoid confusion.
        reset(mKeyManagerListener);

        final String ACTION_STRING = "action";
        final Map<String, Object> ACTION_ARGS = new HashMap<>();
        ACTION_ARGS.put("key", "value");
        keyManager.retrieveEphemeralKey(ACTION_STRING, ACTION_ARGS);

        ArgumentCaptor<CustomerEphemeralKey> keyArgumentCaptor =
                ArgumentCaptor.forClass(CustomerEphemeralKey.class);
        ArgumentCaptor<String> stringArgumentCaptor =
                ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> mapArgumentCaptor =
                ArgumentCaptor.forClass(Map.class);

        verify(mKeyManagerListener, times(1)).onKeyUpdate(
                keyArgumentCaptor.capture(),
                stringArgumentCaptor.capture(),
                mapArgumentCaptor.capture());

        Map<String, Object> capturedMap = mapArgumentCaptor.getValue();
        assertNotNull(capturedMap);
        assertNotNull(keyArgumentCaptor.getValue());
        assertEquals(1, capturedMap.size());
        assertEquals("value", capturedMap.get("key"));
        assertEquals(ACTION_STRING, stringArgumentCaptor.getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void updateKeyIfNecessary_whenReturnsError_setsExistingKeyToNull() {
        CustomerEphemeralKey testKey = CustomerEphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(testKey);

        Calendar proxyCalendar = Calendar.getInstance();
        long expiryTimeInMillis = TimeUnit.SECONDS.toMillis(testKey.getExpires());
        // The time is one millisecond past the expiration date for this test.
        proxyCalendar.setTimeInMillis(expiryTimeInMillis + 1L);
        // Testing this just to invoke getTime
        assertEquals(expiryTimeInMillis + 1L, proxyCalendar.getTimeInMillis());

        mTestEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        EphemeralKeyManager<CustomerEphemeralKey> keyManager = new EphemeralKeyManager(
                mTestEphemeralKeyProvider,
                mKeyManagerListener,
                TEST_SECONDS_BUFFER,
                proxyCalendar,
                CustomerEphemeralKey.class);

        // Make sure we're in a good state
        verify(mKeyManagerListener, times(1)).onKeyUpdate(
                any(CustomerEphemeralKey.class), (String) isNull(), (Map<String, Object>) isNull());
        assertNotNull(keyManager.getEphemeralKey());

        // Set up the error
        final String errorMessage = "This is an error";
        mTestEphemeralKeyProvider.setNextError(404, errorMessage);

        // It should be necessary to update because the key is expired.
        keyManager.retrieveEphemeralKey(null, null);

        verify(mKeyManagerListener, times(1)).onKeyError(404, errorMessage);
        verifyNoMoreInteractions(mKeyManagerListener);
        assertNull(keyManager.getEphemeralKey());
    }
}
