package com.stripe.android.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SourceRedirectTest {
    @Test
    fun asStatus() {
        assertNotNull(SourceFixtures.SOURCE_REDIRECT)

        assertEquals(SourceRedirect.Status.FAILED,
            SourceRedirect.asStatus("failed"))
        assertEquals(SourceRedirect.Status.SUCCEEDED,
            SourceRedirect.asStatus("succeeded"))
        assertEquals(SourceRedirect.Status.PENDING,
            SourceRedirect.asStatus("pending"))
        assertEquals(SourceRedirect.Status.NOT_REQUIRED,
            SourceRedirect.asStatus("not_required"))
        assertNull(SourceRedirect.asStatus("something_else"))
    }
}
