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
import static org.junit.Assert.assertNotNull;
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

    @Test
    public void fromJson_createsKeyWithExpectedValues() {
        CustomerEphemeralKey ephemeralKey = CustomerEphemeralKey.fromString(SAMPLE_KEY_RAW);
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
            CustomerEphemeralKey key = CustomerEphemeralKey.fromJson(originalObject);
            assertNotNull(key);
            JsonTestUtils.assertJsonEquals(originalObject, key.toJson());
        } catch (JSONException unexpected) {
            fail("Failure to parse test JSON");
        }
    }

    @Test
    public void toMap_createsMapWithExpectedValues() {
        CustomerEphemeralKey ephemeralKey = CustomerEphemeralKey.fromString(SAMPLE_KEY_RAW);
        assertNotNull(ephemeralKey);
        Map<String, Object> expectedMap = new HashMap<String, Object>() {{
            put(CustomerEphemeralKey.FIELD_ID, "ephkey_123");
            put(CustomerEphemeralKey.FIELD_OBJECT, "ephemeral_key");
            put(CustomerEphemeralKey.FIELD_SECRET, "ek_test_123");
            put(CustomerEphemeralKey.FIELD_LIVEMODE, false);
            put(CustomerEphemeralKey.FIELD_CREATED, 1483575790L);
            put(CustomerEphemeralKey.FIELD_EXPIRES, 1483579790L);
        }};

        Map<String, String> subMap = new HashMap<>();
        subMap.put(CustomerEphemeralKey.FIELD_ID, "cus_123");
        subMap.put(CustomerEphemeralKey.FIELD_TYPE, "customer");
        List<Object> list = new ArrayList<>();
        list.add(subMap);
        expectedMap.put(CustomerEphemeralKey.FIELD_ASSOCIATED_OBJECTS, list);

        JsonTestUtils.assertMapEquals(expectedMap, ephemeralKey.toMap());
    }

    @Test
    public void toParcel_fromParcel_createsExpectedObject() {
        CustomerEphemeralKey ephemeralKey = CustomerEphemeralKey.fromString(SAMPLE_KEY_RAW);
        assertNotNull(ephemeralKey);
        Parcel parcel = Parcel.obtain();
        ephemeralKey.writeToParcel(parcel, 0);
        // We need to reset the data position of the parcel or else we'll continue reading
        // null values off the end.
        parcel.setDataPosition(0);

        CustomerEphemeralKey createdKey = CustomerEphemeralKey.CREATOR.createFromParcel(parcel);

        assertEquals(ephemeralKey.getId(), createdKey.getId());
        assertEquals(ephemeralKey.getCreated(), createdKey.getCreated());
        assertEquals(ephemeralKey.getExpires(), createdKey.getExpires());
        assertEquals(ephemeralKey.getCustomerId(), createdKey.getCustomerId());
        assertEquals(ephemeralKey.getType(), createdKey.getType());
        assertEquals(ephemeralKey.getSecret(), createdKey.getSecret());
        assertEquals(ephemeralKey.isLiveMode(), createdKey.isLiveMode());
        assertEquals(ephemeralKey.getObject(), createdKey.getObject());
    }
}
