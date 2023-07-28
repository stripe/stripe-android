package com.stripe.android.financialconnections.features.common

import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.ApiKeyFixtures.visual
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class ManifestExtensionsKtTest {

    @Test
    fun `showManualEntryInErrors true when allowManualEntry true and reducedManualEntry false`() {
        val syncObj = syncResponse(
            sessionManifest().copy(allowManualEntry = true),
            visual().copy(reducedManualEntryProminenceForErrors = false)
        )
        assertTrue(syncObj.showManualEntryInErrors())
    }

    @Test
    fun `showManualEntryInErrors false when allowManualEntry true and reducedManualEntry true`() {
        val syncObj = syncResponse(
            sessionManifest().copy(allowManualEntry = true),
            visual().copy(reducedManualEntryProminenceForErrors = true)
        )
        assertFalse(syncObj.showManualEntryInErrors())
    }

    @Test
    fun `showManualEntryInErrors false when allowManualEntry false and reducedManualEntry false`() {
        val syncObj = syncResponse(
            sessionManifest().copy(allowManualEntry = false),
            visual().copy(reducedManualEntryProminenceForErrors = false)
        )
        assertFalse(syncObj.showManualEntryInErrors())
    }

    @Test
    fun `showManualEntryInErrors false when allowManualEntry false and reducedManualEntry true`() {
        val syncObj = syncResponse(
            sessionManifest().copy(allowManualEntry = false),
            visual().copy(reducedManualEntryProminenceForErrors = true)
        )
        assertFalse(syncObj.showManualEntryInErrors())
    }
}
