package com.stripe.android

import kotlin.test.Test
import kotlin.test.assertEquals

class AppInfoTest {

    @Test
    fun toUserAgent() {
        assertEquals("MyAwesomePlugin/1.2.34 (https://myawesomeplugin.info)",
            APP_INFO.toUserAgent())
    }

    @Test
    fun createClientHeaders() {
        val header = mapOf("application" to
            "{\"name\":\"MyAwesomePlugin\",\"partner_id\":\"pp_partner_1234\"," + "\"version\":\"1.2.34\",\"url\":\"https://myawesomeplugin.info\"}"
        )
        assertEquals(header, APP_INFO.createClientHeaders())
    }

    @Test
    fun equals() {
        assertEquals(APP_INFO,
            AppInfo.create(
                "MyAwesomePlugin",
                "1.2.34",
                "https://myawesomeplugin.info",
                "pp_partner_1234"
            ))
    }

    companion object {
        internal val APP_INFO = AppInfo.create(
            "MyAwesomePlugin",
            "1.2.34",
            "https://myawesomeplugin.info",
            "pp_partner_1234"
        )
    }
}
