package com.stripe.android.model

import com.stripe.android.model.StripeSourceTypeModel.Companion.jsonObjectToMapWithoutKeys
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Test class for [StripeSourceTypeModel].
 */
class StripeSourceTypeModelTest {

    @Test
    @Throws(JSONException::class)
    fun jsonObjectToMapWithoutKeys_whenHasKeyInput_returnsMapOmittingKeys() {
        val jsonObject = JSONObject()
            .put("a_key", "a_value")
            .put("b_key", "b_value")
            .put("c_key", "c_value")
            .put("d_key", "d_value")

        val omitKeys = setOf("a_key", "d_key")

        val resultMap = jsonObjectToMapWithoutKeys(jsonObject, omitKeys)!!
        assertEquals(2, resultMap.size)
        assertEquals("b_value", resultMap["b_key"])
        assertEquals("c_value", resultMap["c_key"])
        assertFalse(resultMap.containsKey("a_key"))
        assertFalse(resultMap.containsKey("d_key"))
    }

    @Test
    @Throws(JSONException::class)
    fun jsonObjectToMapWithoutKeys_whenAllKeysGiven_returnsNull() {
        val jsonObject = JSONObject()
            .put("a_key", "a_value")
            .put("b_key", "b_value")

        val omitKeys = setOf("a_key", "b_key")
        val resultMap = jsonObjectToMapWithoutKeys(jsonObject, omitKeys)
        assertNull(resultMap)
    }

    @Test
    @Throws(JSONException::class)
    fun jsonObjectToMapWithoutKeys_whenOtherKeysGiven_returnsFullMap() {
        val jsonObject = JSONObject()
            .put("a_key", "a_value")
            .put("b_key", "b_value")

        val omitKeys = setOf("c_key", "d_key")
        val resultMap = jsonObjectToMapWithoutKeys(jsonObject, omitKeys)!!
        assertEquals("a_value", resultMap["a_key"])
        assertEquals("b_value", resultMap["b_key"])
    }
}
