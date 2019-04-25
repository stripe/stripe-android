package com.stripe.android;

import android.support.annotation.NonNull;

import com.stripe.android.testharness.TestEphemeralKeyProvider;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.net.HttpURLConnection;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test class for {@link EphemeralKeyManager}.
 */
@RunWith(RobolectricTestRunner.class)
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
    private static final long DEFAULT_EXPIRES = 1501199335L;

    @Mock private EphemeralKeyManager.KeyManagerListener<CustomerEphemeralKey> mKeyManagerListener;
    @Captor private ArgumentCaptor<Map<String, Object>> mArgumentCaptor;

    private CustomerEphemeralKey mCustomerEphemeralKey;
    private TestEphemeralKeyProvider mTestEphemeralKeyProvider;
    
    @Before
    public void setup() throws JSONException {
        MockitoAnnotations.initMocks(this);
        mCustomerEphemeralKey = CustomerEphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
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
        final Calendar fixedCalendar = Calendar.getInstance();
        final long expires = TimeUnit.SECONDS.toMillis(DEFAULT_EXPIRES + 2 * TEST_SECONDS_BUFFER);
        final CustomerEphemeralKey key = createEphemeralKey(expires);
        fixedCalendar.setTimeInMillis(expires);

        // If you don't call getTime or getTimeInMillis on a Calendar, none of the updates happen.
        assertEquals(expires, fixedCalendar.getTimeInMillis());
        assertFalse(EphemeralKeyManager.shouldRefreshKey(key,
                TEST_SECONDS_BUFFER,
                fixedCalendar));
    }

    @Test
    public void shouldRefreshKey_whenKeyExpiryIsInThePast_returnsTrue() {
        final Calendar fixedCalendar = Calendar.getInstance();
        final long timeAgoInMillis = fixedCalendar.getTimeInMillis() - 100L;
        final CustomerEphemeralKey key = createEphemeralKey(
                TimeUnit.MILLISECONDS.toSeconds(timeAgoInMillis));
        assertTrue(EphemeralKeyManager.shouldRefreshKey(key,
                TEST_SECONDS_BUFFER,
                fixedCalendar));
    }

    @Test
    public void shouldRefreshKey_whenKeyExpiryIsInFutureButWithinBuffer_returnsTrue() {
        Calendar fixedCalendar = Calendar.getInstance();
        assertNotNull(mCustomerEphemeralKey);

        long parsedExpiryTimeInMillis = TimeUnit.SECONDS
                .toMillis(mCustomerEphemeralKey.getExpires());
        long bufferTimeInMillis = TimeUnit.SECONDS.toMillis(TEST_SECONDS_BUFFER);

        long notFarEnoughInTheFuture = parsedExpiryTimeInMillis + bufferTimeInMillis / 2;
        fixedCalendar.setTimeInMillis(notFarEnoughInTheFuture);
        assertEquals(notFarEnoughInTheFuture, fixedCalendar.getTimeInMillis());

        assertTrue(EphemeralKeyManager.shouldRefreshKey(mCustomerEphemeralKey,
                TEST_SECONDS_BUFFER,
                fixedCalendar));
    }

    @Test
    public void createKeyManager_updatesEphemeralKey_notifiesListener() {
        assertNotNull(mCustomerEphemeralKey);

        mTestEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        EphemeralKeyManager<CustomerEphemeralKey> keyManager = new EphemeralKeyManager<>(
                mTestEphemeralKeyProvider,
                mKeyManagerListener,
                TEST_SECONDS_BUFFER,
                null,
                CustomerEphemeralKey.class);

        verify(mKeyManagerListener).onKeyUpdate(
                ArgumentMatchers.<CustomerEphemeralKey>any(),
                ArgumentMatchers.<String>isNull(),
                ArgumentMatchers.<String>isNull(),
                ArgumentMatchers.<Map<String, Object>>isNull());
        assertNotNull(keyManager.getEphemeralKey());
        assertEquals(mCustomerEphemeralKey.getId(), keyManager.getEphemeralKey().getId());
    }

    @Test
    public void retrieveEphemeralKey_whenUpdateNecessary_returnsUpdateAndArguments() {
        final Calendar fixedCalendar = Calendar.getInstance();
        mTestEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);

        EphemeralKeyManager<CustomerEphemeralKey> keyManager = new EphemeralKeyManager<>(
                mTestEphemeralKeyProvider,
                mKeyManagerListener,
                TEST_SECONDS_BUFFER,
                fixedCalendar,
                CustomerEphemeralKey.class);

        final String operationId = UUID.randomUUID().toString();
        final String actionString = "action";
        final Map<String, Object> actionArgs = new HashMap<>();
        actionArgs.put("key", "value");
        keyManager.retrieveEphemeralKey(operationId, actionString, actionArgs);

        ArgumentCaptor<CustomerEphemeralKey> keyArgumentCaptor =
                ArgumentCaptor.forClass(CustomerEphemeralKey.class);
        ArgumentCaptor<String> stringArgumentCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(mKeyManagerListener).onKeyUpdate(
                keyArgumentCaptor.capture(),
                eq(operationId),
                stringArgumentCaptor.capture(),
                mArgumentCaptor.capture());

        final Map<String, Object> capturedMap = mArgumentCaptor.getValue();
        assertNotNull(capturedMap);
        assertNotNull(keyArgumentCaptor.getValue());
        assertEquals(1, capturedMap.size());
        assertEquals("value", capturedMap.get("key"));
        assertEquals(actionString, stringArgumentCaptor.getValue());
    }

    @Test
    public void updateKeyIfNecessary_whenReturnsError_setsExistingKeyToNull() {
        assertNotNull(mCustomerEphemeralKey);

        Calendar proxyCalendar = Calendar.getInstance();
        long expiryTimeInMillis = TimeUnit.SECONDS.toMillis(mCustomerEphemeralKey.getExpires());
        // The time is one millisecond past the expiration date for this test.
        proxyCalendar.setTimeInMillis(expiryTimeInMillis + 1L);
        // Testing this just to invoke getTime
        assertEquals(expiryTimeInMillis + 1L, proxyCalendar.getTimeInMillis());

        mTestEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        EphemeralKeyManager<CustomerEphemeralKey> keyManager = new EphemeralKeyManager<>(
                mTestEphemeralKeyProvider,
                mKeyManagerListener,
                TEST_SECONDS_BUFFER,
                proxyCalendar,
                CustomerEphemeralKey.class);

        // Make sure we're in a good state
        verify(mKeyManagerListener).onKeyUpdate(
                ArgumentMatchers.<CustomerEphemeralKey>any(),
                ArgumentMatchers.<String>isNull(),
                ArgumentMatchers.<String>isNull(),
                ArgumentMatchers.<Map<String, Object>>isNull());
        assertNotNull(keyManager.getEphemeralKey());

        // Set up the error
        final String errorMessage = "This is an error";
        mTestEphemeralKeyProvider.setNextError(404, errorMessage);

        // It should be necessary to update because the key is expired.
        final String operationId = UUID.randomUUID().toString();
        keyManager.retrieveEphemeralKey(operationId, null, null);

        verify(mKeyManagerListener).onKeyError(operationId, 404, errorMessage);
        verifyNoMoreInteractions(mKeyManagerListener);
        assertNull(keyManager.getEphemeralKey());
    }

    @Test
    public void triggerCorrectErrorOnInvalidRawKey() {

        mTestEphemeralKeyProvider.setNextRawEphemeralKey("Not_a_JSON");
        EphemeralKeyManager<CustomerEphemeralKey> keyManager = new EphemeralKeyManager<>(
                mTestEphemeralKeyProvider,
                mKeyManagerListener,
                TEST_SECONDS_BUFFER,
                null,
                CustomerEphemeralKey.class);

        verify(mKeyManagerListener, never()).onKeyUpdate(
                ArgumentMatchers.<CustomerEphemeralKey>isNull(),
                ArgumentMatchers.<String>isNull(),
                ArgumentMatchers.<String>isNull(),
                ArgumentMatchers.<Map<String, Object>>isNull());
        verify(mKeyManagerListener).onKeyError(null,
                HttpURLConnection.HTTP_INTERNAL_ERROR,
                "EphemeralKeyUpdateListener.onKeyUpdate was passed a value that " +
                        "could not be JSON parsed: [Value Not_a_JSON of type java.lang.String " +
                        "cannot be converted to JSONObject]. The raw body from Stripe's " +
                        "response should be passed");
        assertNull(keyManager.getEphemeralKey());
    }

    @Test
    public void triggerCorrectErrorOnInvalidJsonKey() {
        mTestEphemeralKeyProvider.setNextRawEphemeralKey("{}");
        EphemeralKeyManager<CustomerEphemeralKey> keyManager = new EphemeralKeyManager<>(
                mTestEphemeralKeyProvider,
                mKeyManagerListener,
                TEST_SECONDS_BUFFER,
                null,
                CustomerEphemeralKey.class);

        verify(mKeyManagerListener, never()).onKeyUpdate(
                ArgumentMatchers.<CustomerEphemeralKey>isNull(),
                ArgumentMatchers.<String>isNull(),
                ArgumentMatchers.<String>isNull(),
                ArgumentMatchers.<Map<String, Object>>isNull());
        verify(mKeyManagerListener).onKeyError(null,
                HttpURLConnection.HTTP_INTERNAL_ERROR,
                "EphemeralKeyUpdateListener.onKeyUpdate was passed a JSON String " +
                        "that was invalid: [Improperly formatted JSON for ephemeral " +
                        "key CustomerEphemeralKey - No value for created]. The raw body " +
                        "from Stripe's response should be passed");
        assertNull(keyManager.getEphemeralKey());
    }

    @Test
    public void triggerCorrectErrorOnNullKey() {
        mTestEphemeralKeyProvider.setNextRawEphemeralKey(null);
        EphemeralKeyManager<CustomerEphemeralKey> keyManager = new EphemeralKeyManager<>(
                mTestEphemeralKeyProvider,
                mKeyManagerListener,
                TEST_SECONDS_BUFFER,
                null,
                CustomerEphemeralKey.class);

        verify(mKeyManagerListener, never()).onKeyUpdate(
                ArgumentMatchers.<CustomerEphemeralKey>isNull(),
                ArgumentMatchers.<String>isNull(),
                ArgumentMatchers.<String>isNull(),
                ArgumentMatchers.<Map<String, Object>>isNull());
        verify(mKeyManagerListener).onKeyError(null,
                HttpURLConnection.HTTP_INTERNAL_ERROR,
                "EphemeralKeyUpdateListener.onKeyUpdate was called with a null value");
        assertNull(keyManager.getEphemeralKey());
    }

    @NonNull
    private CustomerEphemeralKey createEphemeralKey(long expires) {
        return new CustomerEphemeralKey(1501199335L, "cus_AQsHpvKfKwJDrF",
                expires, "ephkey_123", false, "customer", "", "");
    }
}
