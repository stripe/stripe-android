package com.stripe.android.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.stripe.android.model.StripeSourceTypeModel.jsonObjectToMapWithoutKeys;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test class for {@link StripeSourceTypeModel}.
 */
public class StripeSourceTypeModelTest {

    @Test
    public void jsonObjectToMapWithoutKeys_whenHasKeyInput_returnsMapOmittingKeys()
            throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("a_key", "a_value");
        jsonObject.put("b_key", "b_value");
        jsonObject.put("c_key", "c_value");
        jsonObject.put("d_key", "d_value");

        Set<String> omitKeys = new HashSet<String>() {{
            add("a_key");
            add("d_key");
        }};
        Map<String, Object> resultMap = jsonObjectToMapWithoutKeys(jsonObject, omitKeys);
        assertNotNull(resultMap);
        assertEquals(2, resultMap.size());
        assertEquals("b_value", resultMap.get("b_key"));
        assertEquals("c_value", resultMap.get("c_key"));
        assertFalse(resultMap.containsKey("a_key"));
        assertFalse(resultMap.containsKey("d_key"));
    }

    @Test
    public void jsonObjectToMapWithoutKeys_whenAllKeysGiven_returnsNull() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("a_key", "a_value");
        jsonObject.put("b_key", "b_value");

        Set<String> omitKeys = new HashSet<String>() {{
            add("a_key");
            add("b_key");
        }};
        Map<String, Object> resultMap = jsonObjectToMapWithoutKeys(jsonObject, omitKeys);
        assertNull(resultMap);
    }

    @Test
    public void jsonObjectToMapWithoutKeys_whenOtherKeysGiven_returnsFullMap() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("a_key", "a_value");
        jsonObject.put("b_key", "b_value");

        Set<String> omitKeys = new HashSet<String>() {{
            add("c_key");
            add("d_key");
        }};
        Map<String, Object> resultMap = jsonObjectToMapWithoutKeys(jsonObject, omitKeys);
        assertNotNull(resultMap);
        assertEquals("a_value", resultMap.get("a_key"));
        assertEquals("b_value", resultMap.get("b_key"));
    }
}
