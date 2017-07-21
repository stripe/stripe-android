package com.stripe.android;

import android.os.Parcel;

import com.stripe.android.testharness.JsonTestUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class EphemeralKeyTest {

    private static final String SAMPLE_KEY_RAW = "{\n" +
            "    \"id\": \"ephkey_123\",\n" +
            "    \"object\": \"ephemeral_key\",\n" +
            "    \"secret\": \"ek_test_123\",\n" +
            "    \"created\": 1483575790,\n" +
            "    \"livemode\": false,\n" +
            "    \"expires\": 1483579790,\n" +
            "    \"associated_objects\": [\n" +
            "    \t{\n" +
            "    \t\t\"type\": \"customer\",\n" +
            "    \t\t\"id\": \"cus_123\"\n" +
            "    \t}\n" +
            "    ]\n" +
            "}";

    private static final String SAMPLE_KEY_RAW_EMPTY_OBJECT = "{\n" +
            "    \"id\": \"ephkey_123\",\n" +
            "    \"object\": \"\",\n" +
            "    \"secret\": \"ek_test_123\",\n" +
            "    \"created\": 1483575790,\n" +
            "    \"livemode\": false,\n" +
            "    \"expires\": 1483579790,\n" +
            "    \"associated_objects\": [\n" +
            "    \t{\n" +
            "    \t\t\"type\": \"customer\",\n" +
            "    \t\t\"id\": \"cus_123\"\n" +
            "    \t}\n" +
            "    ]\n" +
            "}";

    private static final String SAMPLE_KEY_RAW_NULL_OBJECT = "{\n" +
            "    \"id\": \"ephkey_123\",\n" +
            "    \"object\": \"null\",\n" +
            "    \"secret\": \"ek_test_123\",\n" +
            "    \"created\": 1483575790,\n" +
            "    \"livemode\": false,\n" +
            "    \"expires\": 1483579790,\n" +
            "    \"associated_objects\": [\n" +
            "    \t{\n" +
            "    \t\t\"type\": \"customer\",\n" +
            "    \t\t\"id\": \"cus_123\"\n" +
            "    \t}\n" +
            "    ]\n" +
            "}";

    private static final String SAMPLE_KEY_RAW_NO_OBJECT = "{\n" +
            "    \"id\": \"ephkey_123\",\n" +
            "    \"secret\": \"ek_test_123\",\n" +
            "    \"created\": 1483575790,\n" +
            "    \"livemode\": false,\n" +
            "    \"expires\": 1483579790,\n" +
            "    \"associated_objects\": [\n" +
            "    \t{\n" +
            "    \t\t\"type\": \"customer\",\n" +
            "    \t\t\"id\": \"cus_123\"\n" +
            "    \t}\n" +
            "    ]\n" +
            "}";

    @Test
    public void fromJson_createsKeyWithExpectedValues() {
        EphemeralKey ephemeralKey = EphemeralKey.fromString(SAMPLE_KEY_RAW);
        assertNotNull(ephemeralKey);
        assertEquals("ephkey_123", ephemeralKey.getId());
        assertEquals("ephemeral_key", ephemeralKey.getObject());
        assertEquals("ek_test_123", ephemeralKey.getSecret());
        assertEquals(false, ephemeralKey.isLiveMode());
        assertEquals(1483575790L, ephemeralKey.getCreated());
        assertEquals(1483579790L, ephemeralKey.getExpires());
        assertEquals("customer", ephemeralKey.getType());
        assertEquals("cus_123", ephemeralKey.getCustomerId());
    }

    @Test
    public void fromJson_toJson_createsEqualObject() {
        try {
            JSONObject originalObject = new JSONObject(SAMPLE_KEY_RAW);
            EphemeralKey key = EphemeralKey.fromJson(originalObject);
            assertNotNull(key);
            JsonTestUtils.assertJsonEquals(originalObject, key.toJson());
        } catch (JSONException unexpected) {
            fail("Failure to parse test JSON");
        }
    }

    @Test
    public void toMap_createsMapWithExpectedValues() {
        EphemeralKey ephemeralKey = EphemeralKey.fromString(SAMPLE_KEY_RAW);
        assertNotNull(ephemeralKey);
        Map<String, Object> expectedMap = new HashMap<String, Object>() {{
            put(EphemeralKey.FIELD_ID, "ephkey_123");
            put(EphemeralKey.FIELD_OBJECT, "ephemeral_key");
            put(EphemeralKey.FIELD_SECRET, "ek_test_123");
            put(EphemeralKey.FIELD_LIVEMODE, false);
            put(EphemeralKey.FIELD_CREATED, 1483575790L);
            put(EphemeralKey.FIELD_EXPIRES, 1483579790L);
        }};

        Map<String, String> subMap = new HashMap<>();
        subMap.put(EphemeralKey.FIELD_ID, "cus_123");
        subMap.put(EphemeralKey.FIELD_TYPE, "customer");
        List<Object> list = new ArrayList<>();
        list.add(subMap);
        expectedMap.put(EphemeralKey.FIELD_ASSOCIATED_OBJECTS, list);

        JsonTestUtils.assertMapEquals(expectedMap, ephemeralKey.toMap());
    }

    @Test
    public void toParcel_fromParcel_createsExpectedObject() {
        EphemeralKey ephemeralKey = EphemeralKey.fromString(SAMPLE_KEY_RAW);
        assertNotNull(ephemeralKey);
        Parcel parcel = Parcel.obtain();
        ephemeralKey.writeToParcel(parcel, 0);
        // We need to reset the data position of the parcel or else we'll continue reading
        // null values off the end.
        parcel.setDataPosition(0);

        EphemeralKey createdKey = EphemeralKey.CREATOR.createFromParcel(parcel);

        assertEquals(ephemeralKey.getId(), createdKey.getId());
        assertEquals(ephemeralKey.getCreated(), createdKey.getCreated());
        assertEquals(ephemeralKey.getExpires(), createdKey.getExpires());
        assertEquals(ephemeralKey.getCustomerId(), createdKey.getCustomerId());
        assertEquals(ephemeralKey.getType(), createdKey.getType());
        assertEquals(ephemeralKey.getSecret(), createdKey.getSecret());
        assertEquals(ephemeralKey.isLiveMode(), createdKey.isLiveMode());
        assertEquals(ephemeralKey.getObject(), createdKey.getObject());
    }

    @Test
    public void fromJson_whenObjectIsNull_createsExpectedObject() {
        EphemeralKey ephemeralKey = EphemeralKey.fromString(SAMPLE_KEY_RAW_EMPTY_OBJECT);
        assertNotNull(ephemeralKey);
        assertEquals("ephkey_123", ephemeralKey.getId());
        assertNull(ephemeralKey.getObject());
        assertEquals("ek_test_123", ephemeralKey.getSecret());
        assertEquals(false, ephemeralKey.isLiveMode());
        assertEquals(1483575790L, ephemeralKey.getCreated());
        assertEquals(1483579790L, ephemeralKey.getExpires());
        assertEquals("customer", ephemeralKey.getType());
        assertEquals("cus_123", ephemeralKey.getCustomerId());
    }

    @Test
    public void toParcelFromParcel_whenObjectIsNull_createsExpectedObject() {
        EphemeralKey ephemeralKey = EphemeralKey.fromString(SAMPLE_KEY_RAW_EMPTY_OBJECT);
        assertNotNull(ephemeralKey);
        Parcel parcel = Parcel.obtain();
        ephemeralKey.writeToParcel(parcel, 0);
        // We need to reset the data position of the parcel or else we'll continue reading
        // null values off the end.
        parcel.setDataPosition(0);

        EphemeralKey createdKey = EphemeralKey.CREATOR.createFromParcel(parcel);

        assertEquals(ephemeralKey.getId(), createdKey.getId());
        assertEquals(ephemeralKey.getCreated(), createdKey.getCreated());
        assertEquals(ephemeralKey.getExpires(), createdKey.getExpires());
        assertEquals(ephemeralKey.getCustomerId(), createdKey.getCustomerId());
        assertEquals(ephemeralKey.getType(), createdKey.getType());
        assertEquals(ephemeralKey.getSecret(), createdKey.getSecret());
        assertEquals(ephemeralKey.isLiveMode(), createdKey.isLiveMode());
        assertNull(ephemeralKey.getObject(), createdKey.getObject());
    }

    @Test
    public void fromJson_whenObjectIsNullString_createsExpectedObject() {
        EphemeralKey ephemeralKey = EphemeralKey.fromString(SAMPLE_KEY_RAW_NULL_OBJECT);
        assertNotNull(ephemeralKey);
        assertEquals("ephkey_123", ephemeralKey.getId());
        assertNull(ephemeralKey.getObject());
        assertEquals("ek_test_123", ephemeralKey.getSecret());
        assertEquals(false, ephemeralKey.isLiveMode());
        assertEquals(1483575790L, ephemeralKey.getCreated());
        assertEquals(1483579790L, ephemeralKey.getExpires());
        assertEquals("customer", ephemeralKey.getType());
        assertEquals("cus_123", ephemeralKey.getCustomerId());
    }

    @Test
    public void fromJson_whenObjectIsNotPresent_createsExpectedObject() {
        EphemeralKey ephemeralKey = EphemeralKey.fromString(SAMPLE_KEY_RAW_NO_OBJECT);
        assertNotNull(ephemeralKey);
        assertEquals("ephkey_123", ephemeralKey.getId());
        assertNull(ephemeralKey.getObject());
        assertEquals("ek_test_123", ephemeralKey.getSecret());
        assertEquals(false, ephemeralKey.isLiveMode());
        assertEquals(1483575790L, ephemeralKey.getCreated());
        assertEquals(1483579790L, ephemeralKey.getExpires());
        assertEquals("customer", ephemeralKey.getType());
        assertEquals("cus_123", ephemeralKey.getCustomerId());
    }

    @Test
    public void toJson_withNullObject_doesNotPutObjectKey() {
        EphemeralKey startingKey = EphemeralKey.fromString(SAMPLE_KEY_RAW_EMPTY_OBJECT);
        assertNotNull(startingKey);
        assertNull(startingKey.getObject());

        JSONObject outputObject = startingKey.toJson();

        assertFalse(outputObject.has(EphemeralKey.FIELD_OBJECT));
        EphemeralKey cycledKey = EphemeralKey.fromJson(outputObject);
        assertNotNull(cycledKey);
    }

    @Test
    public void fromNullOrEmptyStringIfNecessary_forEmptyStringAndNullString_returnsNull() {
        assertNull(EphemeralKey.fromNullOrEmptyStringIfNecessary("null"));
        assertNull(EphemeralKey.fromNullOrEmptyStringIfNecessary(""));
    }

    @Test
    public void fromNullOrEmptyStringIfNecessary_forNonNullString_returnsInputString() {
        assertEquals("Hello", EphemeralKey.fromNullOrEmptyStringIfNecessary("Hello"));
    }

    @Test
    public void toNullStringIfNecessary_forNullOrEmptyValue_returnsNullString() {
        assertEquals("null", EphemeralKey.toNullStringIfNecessary(""));
        assertEquals("null", EphemeralKey.toNullStringIfNecessary(null));
    }
}
