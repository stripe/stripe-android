package com.stripe.android.model.parsers

import com.stripe.android.model.SourceFixtures
import com.stripe.android.model.SourceRedirect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SourceRedirectJsonParserTest {
    @Test
    fun asStatus() {
        assertNotNull(SourceFixtures.SOURCE_REDIRECT)

        assertEquals(SourceRedirect.Status.FAILED,
            SourceRedirectJsonParser.asStatus("failed"))
        assertEquals(SourceRedirect.Status.SUCCEEDED,
            SourceRedirectJsonParser.asStatus("succeeded"))
        assertEquals(SourceRedirect.Status.PENDING,
            SourceRedirectJsonParser.asStatus("pending"))
        assertEquals(SourceRedirect.Status.NOT_REQUIRED,
            SourceRedirectJsonParser.asStatus("not_required"))
        assertNull(SourceRedirectJsonParser.asStatus("something_else"))
    }
}
