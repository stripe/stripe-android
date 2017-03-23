package com.stripe.android.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.stripe.android.model.StripeSourceTypeModel.jsonObjectToMapWithoutKeys;
import static com.stripe.android.model.StripeSourceTypeModel.putAdditionalFieldsIntoJsonObject;
import static com.stripe.android.model.StripeSourceTypeModel.putAdditionalFieldsIntoMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Test class for {@link StripeSourceTypeModel}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class StripeSourceTypeModelTest {

    @Test
    public void jsonObjectToMapWithoutKeys_whenHasKeyInput_returnsMapOmittingKeys() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("a_key", "a_value");
            jsonObject.put("b_key", "b_value");
            jsonObject.put("c_key", "c_value");
            jsonObject.put("d_key", "d_value");
        } catch (JSONException unexpected) {
            fail("Unexpected error: " + unexpected.getLocalizedMessage());
        }

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
    public void jsonObjectToMapWithoutKeys_whenAllKeysGiven_returnsNull() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("a_key", "a_value");
            jsonObject.put("b_key", "b_value");
        } catch (JSONException unexpected) {
            fail("Unexpected error: " + unexpected.getLocalizedMessage());
        }

        Set<String> omitKeys = new HashSet<String>() {{
            add("a_key");
            add("b_key");
        }};
        Map<String, Object> resultMap = jsonObjectToMapWithoutKeys(jsonObject, omitKeys);
        assertNull(resultMap);
    }

    @Test
    public void jsonObjectToMapWithoutKeys_whenOtherKeysGiven_returnsFullMap() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("a_key", "a_value");
            jsonObject.put("b_key", "b_value");
        } catch (JSONException unexpected) {
            fail("Unexpected error: " + unexpected.getLocalizedMessage());
        }

        Set<String> omitKeys = new HashSet<String>() {{
            add("c_key");
            add("d_key");
        }};
        Map<String, Object> resultMap = jsonObjectToMapWithoutKeys(jsonObject, omitKeys);
        assertNotNull(resultMap);
        assertEquals("a_value", resultMap.get("a_key"));
        assertEquals("b_value", resultMap.get("b_key"));
    }

    @Test
    public void putAdditionalFieldsIntoJsonObject_whenHasFields_putsThemIntoObject() {
        JSONObject jsonObject = new JSONObject();
        Map<String, Object> additionalFields = new HashMap<>();
        additionalFields.put("a_key", "a_value");
        additionalFields.put("b_key", "b_value");
        putAdditionalFieldsIntoJsonObject(jsonObject, additionalFields);

        assertEquals(2, jsonObject.length());
        assertEquals("a_value", jsonObject.optString("a_key"));
        assertEquals("b_value", jsonObject.optString("b_key"));
    }

    @Test
    public void putAdditionalFieldsIntoJsonObject_whenHasDuplicateFields_putsThemIntoObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("a_key", "original");
        } catch (JSONException unexpected) {
            fail("Unexpected exception: " + unexpected);
        }

        Map<String, Object> additionalFields = new HashMap<>();
        additionalFields.put("a_key", "a_value");
        additionalFields.put("b_key", "b_value");
        putAdditionalFieldsIntoJsonObject(jsonObject, additionalFields);

        assertEquals(2, jsonObject.length());
        assertEquals("a_value", jsonObject.optString("a_key"));
        assertEquals("b_value", jsonObject.optString("b_key"));
    }

    @Test
    public void putAdditionalFieldsIntoJsonObject_whenNoFieldsArePassed_doesNothing() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("a", "a_value");
        } catch (JSONException unexpected) {
            fail("Unexpected error: " + unexpected.getLocalizedMessage());
        }
        Map<String, Object> emptyMap = new HashMap<>();
        putAdditionalFieldsIntoJsonObject(jsonObject, emptyMap);

        assertEquals(1, jsonObject.length());
    }

    @Test
    public void putAdditionalFieldsIntoJsonObject_whenNullPassedInEitherArgument_doesNothing() {
        Map<String, Object> littleMap = new HashMap<String, Object>() {{ put("a", "a_value"); }};
        JSONObject jsonObject =  new JSONObject();

        // Just don't crash for this one
        putAdditionalFieldsIntoJsonObject(null, littleMap);

        putAdditionalFieldsIntoJsonObject(jsonObject, null);
        assertEquals(0, jsonObject.length());
    }

    @Test
    public void putAdditionalFieldsIntoMap_whenGivenFields_putsThemIntoTheMap() {
        Map<String, Object> originalMap = new HashMap<String, Object>() {{
            put("a", "a_val");
            put("b", "b_val");
        }};
        Map<String, Object> extraMap = new HashMap<String, Object>() {{
            put("c", 100);
            put("d", false);
        }};

        putAdditionalFieldsIntoMap(originalMap, extraMap);
        assertEquals(4, originalMap.size());
        assertEquals("a_val", originalMap.get("a"));
        assertEquals("b_val", originalMap.get("b"));
        assertEquals(100, originalMap.get("c"));
        assertEquals(false, originalMap.get("d"));
    }

    @Test
    public void putAdditionalFieldsIntoMap_whenGivenDuplicateFields_putsThemIntoTheMap() {
        Map<String, Object> originalMap = new HashMap<String, Object>() {{
            put("a", "a_val");
            put("b", "b_val");
        }};
        Map<String, Object> extraMap = new HashMap<String, Object>() {{
            // Overwriting this value!
            put("a", 100);
            put("d", false);
        }};

        putAdditionalFieldsIntoMap(originalMap, extraMap);
        assertEquals(3, originalMap.size());
        assertEquals(100, originalMap.get("a"));
        assertEquals("b_val", originalMap.get("b"));
        assertEquals(false, originalMap.get("d"));
    }

    @Test
    public void putAdditionalFieldsIntoMap_whenGivenEmptyMap_doesNothing() {
        Map<String, Object> originalMap = new HashMap<String, Object>() {{
            put("a", "a_val");
            put("b", "b_val");
        }};
        Map<String, Object> emptyMap = new HashMap<>();

        putAdditionalFieldsIntoMap(originalMap, emptyMap);
        assertEquals(2, originalMap.size());
        assertEquals("a_val", originalMap.get("a"));
        assertEquals("b_val", originalMap.get("b"));
    }

    @Test
    public void putAdditionalFieldsIntoMap_whenGivenNullForEitherInput_doesNothing() {
        Map<String, Object> littleMap = new HashMap<String, Object>() {{ put("a", "b"); }};

        // Just don't crash when the first one is null
        putAdditionalFieldsIntoMap(null, littleMap);

        putAdditionalFieldsIntoMap(littleMap, null);
        assertEquals(1, littleMap.size());
        assertEquals("b", littleMap.get("a"));
    }
}
