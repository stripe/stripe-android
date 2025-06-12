package com.stripe.android.shoppay.webview

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test

class HandleClickRequestJsonParserTest {
    
    private val parser = HandleClickRequestJsonParser()
    
    @Test
    fun `parse should correctly parse valid handle click request`() {
        val jsonString = """
            {
                "eventData": {
                    "expressPaymentType": "shop_pay"
                },
                "timestamp": 1749737552473,
                "requestId": "req_1749737552473_wh4vdgx"
            }
        """.trimIndent()
        
        val json = JSONObject(jsonString)
        val result = parser.parse(json)
        
        assertThat(result).isNotNull()
        result!!
        
        // Test top-level fields
        assertThat(result.requestId).isEqualTo("req_1749737552473_wh4vdgx")
        assertThat(result.timestamp).isEqualTo(1749737552473L)
        
        // Test event data fields
        val eventData = result.eventData
        assertThat(eventData.expressPaymentType).isEqualTo("shop_pay")
    }
    
    @Test
    fun `parse should handle different express payment types`() {
        val jsonString = """
            {
                "eventData": {
                    "expressPaymentType": "google_pay"
                },
                "timestamp": 1749737552474,
                "requestId": "req_1749737552474_abc123"
            }
        """.trimIndent()
        
        val json = JSONObject(jsonString)
        val result = parser.parse(json)
        
        assertThat(result).isNotNull()
        result!!
        
        assertThat(result.requestId).isEqualTo("req_1749737552474_abc123")
        assertThat(result.timestamp).isEqualTo(1749737552474L)
        assertThat(result.eventData.expressPaymentType).isEqualTo("google_pay")
    }
    
    @Test
    fun `parse should return null when requestId is missing`() {
        val jsonString = """
            {
                "eventData": {
                    "expressPaymentType": "shop_pay"
                },
                "timestamp": 1749737552473
            }
        """.trimIndent()
        
        val json = JSONObject(jsonString)
        val result = parser.parse(json)
        
        assertThat(result).isNull()
    }
    
    @Test
    fun `parse should return null when eventData is missing`() {
        val jsonString = """
            {
                "timestamp": 1749737552473,
                "requestId": "req_1749737552473_wh4vdgx"
            }
        """.trimIndent()
        
        val json = JSONObject(jsonString)
        val result = parser.parse(json)
        
        assertThat(result).isNull()
    }
    
    @Test
    fun `parse should return null when expressPaymentType is missing`() {
        val jsonString = """
            {
                "eventData": {},
                "timestamp": 1749737552473,
                "requestId": "req_1749737552473_wh4vdgx"
            }
        """.trimIndent()
        
        val json = JSONObject(jsonString)
        val result = parser.parse(json)
        
        assertThat(result).isNull()
    }
    
    @Test
    fun `parse should handle zero timestamp`() {
        val jsonString = """
            {
                "eventData": {
                    "expressPaymentType": "shop_pay"
                },
                "requestId": "req_1749737552473_wh4vdgx"
            }
        """.trimIndent()
        
        val json = JSONObject(jsonString)
        val result = parser.parse(json)
        
        assertThat(result).isNotNull()
        result!!
        assertThat(result.timestamp).isEqualTo(0L)
        assertThat(result.requestId).isEqualTo("req_1749737552473_wh4vdgx")
        assertThat(result.eventData.expressPaymentType).isEqualTo("shop_pay")
    }
} 