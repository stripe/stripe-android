package com.stripe.android;

import com.stripe.android.testharness.TestEphemeralKeyProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

    @Mock EphemeralKeyManager.KeyManagerListener mKeyManagerListener;

    private Calendar mProxyCalendar;
    private TestEphemeralKeyProvider mTestEphemeralKeyProvider;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mProxyCalendar = Calendar.getInstance();
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
        EphemeralKey key = EphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
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
        EphemeralKey key = EphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
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
        EphemeralKey key = EphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
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
    public void createKeyManager_updatesEphemeralKey_notifiesListener() {
        EphemeralKey testKey = EphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(testKey);

        mTestEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        EphemeralKeyManager keyManager = new EphemeralKeyManager(
                mTestEphemeralKeyProvider,
                mKeyManagerListener,
                TEST_SECONDS_BUFFER,
                null);

        verify(mKeyManagerListener, times(1)).onKeyUpdate(any(EphemeralKey.class));
        assertNotNull(keyManager.getEphemeralKey());
        assertEquals(testKey.getId(), keyManager.getEphemeralKey().getId());
    }

    @Test
    public void updateKeyIfNecessary_whenReturnsError_setsExistingKeyToNull() {
        EphemeralKey testKey = EphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(testKey);

        Calendar proxyCalendar = Calendar.getInstance();
        long expiryTimeInMillis = TimeUnit.SECONDS.toMillis(testKey.getExpires());
        // The time is one millisecond past the expiration date for this test.
        proxyCalendar.setTimeInMillis(expiryTimeInMillis + 1L);
        // Testing this just to invoke getTime
        assertEquals(expiryTimeInMillis + 1L, proxyCalendar.getTimeInMillis());

        mTestEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        EphemeralKeyManager keyManager = new EphemeralKeyManager(
                mTestEphemeralKeyProvider,
                mKeyManagerListener,
                TEST_SECONDS_BUFFER,
                proxyCalendar);

        // Make sure we're in a good state
        verify(mKeyManagerListener, times(1)).onKeyUpdate(any(EphemeralKey.class));
        assertNotNull(keyManager.getEphemeralKey());

        // Set up the error
        final String errorMessage = "This is an error";
        mTestEphemeralKeyProvider.setNextError(404, errorMessage);

        // It should be necessary to update because the key is expired.
        keyManager.retrieveEphemeralKey();

        verify(mKeyManagerListener, times(1)).onKeyError(404, errorMessage);
        verifyNoMoreInteractions(mKeyManagerListener);
        assertNull(keyManager.getEphemeralKey());
    }
}
