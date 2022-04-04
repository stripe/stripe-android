package com.stripe.android.core

import kotlin.test.Test
import kotlin.test.assertEquals

class AppInfoTest {

    @Test
    fun toUserAgent() {
        assertEquals(
            "MyAwesomePlugin/1.2.34 (https://myawesomeplugin.info)",
            APP_INFO.toUserAgent()
        )
    }

    @Test
    fun createClientHeaders() {
        val header = mapOf(
            "application" to
                mapOf(
                    "name" to "MyAwesomePlugin",
                    "partner_id" to "pp_partner_1234",
                    "version" to "1.2.34",
                    "url" to "https://myawesomeplugin.info"
                )
        )
        assertEquals(header, APP_INFO.createClientHeaders())
    }

    @Test
    fun createClientHeaders_withoutVersion() {
        val appInfo = AppInfo.create(
            "MyAwesomePlugin",
            url = "https://myawesomeplugin.info"
        )

        assertEquals(
            "MyAwesomePlugin (https://myawesomeplugin.info)",
            appInfo.toUserAgent()
        )
    }

    @Test
    fun createClientHeaders_withoutVersionOrUrl() {
        val appInfo = AppInfo.create(
            "MyAwesomePlugin"
        )

        assertEquals(
            "MyAwesomePlugin",
            appInfo.toUserAgent()
        )
    }

    @Test
    fun equals() {
        assertEquals(
            APP_INFO,
            AppInfo.create(
                "MyAwesomePlugin",
                "1.2.34",
                "https://myawesomeplugin.info",
                "pp_partner_1234"
            )
        )
    }

    internal companion object {
        internal val APP_INFO = AppInfoFixtures.DEFAULT
    }
}
