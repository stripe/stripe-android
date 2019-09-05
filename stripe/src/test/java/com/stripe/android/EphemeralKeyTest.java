package com.stripe.android;

import android.os.Parcel;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
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

    @NonNull
    private CustomerEphemeralKey getCustomerEphemeralKey() throws JSONException {
        return CustomerEphemeralKey.fromJson(new JSONObject(EphemeralKeyTest.SAMPLE_KEY_RAW));
    }
    
    @Test
    public void fromJson_createsKeyWithExpectedValues() throws JSONException {
        CustomerEphemeralKey ephemeralKey = getCustomerEphemeralKey();
        assertNotNull(ephemeralKey);
        assertEquals("ephkey_123", ephemeralKey.getId());
        assertEquals("ephemeral_key", ephemeralKey.getObject());
        assertEquals("ek_test_123", ephemeralKey.getSecret());
        assertFalse(ephemeralKey.isLiveMode());
        assertEquals(1483575790L, ephemeralKey.getCreated());
        assertEquals(1483579790L, ephemeralKey.getExpires());
        assertEquals("customer", ephemeralKey.getType());
        assertEquals("cus_123", ephemeralKey.getCustomerId());
    }

    @Test
    public void fromJson_createsObject() throws JSONException {
        assertNotNull(CustomerEphemeralKey.fromJson(new JSONObject(SAMPLE_KEY_RAW)));
    }

    @Test
    public void toParcel_fromParcel_createsExpectedObject() throws JSONException {
        CustomerEphemeralKey ephemeralKey = getCustomerEphemeralKey();
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
