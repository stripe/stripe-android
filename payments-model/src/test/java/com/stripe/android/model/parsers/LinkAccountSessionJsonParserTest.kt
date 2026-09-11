package com.stripe.android.model.parsers

import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class LinkAccountSessionJsonParserTest {
    @Test
    fun `parses correctly`() {
        val id = "id_xyz"
        val secret = "secret_xyz"
        val json = """{"id": "$id", "client_secret": "$secret", "permissions": ["balances", "ownership"]}"""
        val obj = LinkAccountSessionJsonParser.parse(JSONObject(json))
        assertEquals(id, obj.id)
        assertEquals(secret, obj.clientSecret)
        assertEquals(listOf("balances", "ownership"), obj.permissions)
    }

    @Test
    fun `missing permissions defaults to empty list`() {
        val json = """{"id": "id_xyz", "client_secret": "secret_xyz"}"""

        val obj = LinkAccountSessionJsonParser.parse(JSONObject(json))

        assertEquals(emptyList<String>(), obj.permissions)
    }

    @Test(expected = JSONException::class)
    fun `id is required`() {
        val json = """{"client_secret": "secret"}"""
        LinkAccountSessionJsonParser.parse(JSONObject(json))
    }

    @Test(expected = JSONException::class)
    fun `secret is required`() {
        val json = """{"id": "id"}"""
        LinkAccountSessionJsonParser.parse(JSONObject(json))
    }
}
