package com.stripe.android.model

import com.stripe.android.model.StripeSourceTypeModel.Companion.jsonObjectToMapWithoutKeys
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import org.json.JSONObject

/**
 * Test class for [StripeSourceTypeModel].
 */
class StripeSourceTypeModelTest {

    @Test
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
    fun jsonObjectToMapWithoutKeys_whenAllKeysGiven_returnsNull() {
        val jsonObject = JSONObject()
            .put("a_key", "a_value")
            .put("b_key", "b_value")

        val omitKeys = setOf("a_key", "b_key")
        val resultMap = jsonObjectToMapWithoutKeys(jsonObject, omitKeys)
        assertNull(resultMap)
    }

    @Test
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
