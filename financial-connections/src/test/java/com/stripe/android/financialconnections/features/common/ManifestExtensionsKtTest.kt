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
            visual().copy(reducedManualEntryProminenceInErrors = false)
        )
        assertTrue(syncObj.showManualEntryInErrors())
    }

    @Test
    fun `showManualEntryInErrors false when allowManualEntry true and reducedManualEntry true`() {
        val syncObj = syncResponse(
            sessionManifest().copy(allowManualEntry = true),
            visual().copy(reducedManualEntryProminenceInErrors = true)
        )
        assertFalse(syncObj.showManualEntryInErrors())
    }

    @Test
    fun `showManualEntryInErrors false when allowManualEntry false and reducedManualEntry false`() {
        val syncObj = syncResponse(
            sessionManifest().copy(allowManualEntry = false),
            visual().copy(reducedManualEntryProminenceInErrors = false)
        )
        assertFalse(syncObj.showManualEntryInErrors())
    }

    @Test
    fun `showManualEntryInErrors false when allowManualEntry false and reducedManualEntry true`() {
        val syncObj = syncResponse(
            sessionManifest().copy(allowManualEntry = false),
            visual().copy(reducedManualEntryProminenceInErrors = true)
        )
        assertFalse(syncObj.showManualEntryInErrors())
    }

    @Test
    fun `enableRetrieveAuthSession true when disable feature flag not present`() {
        val syncObj = syncResponse(
            sessionManifest().copy(features = emptyMap()),
        )
        assertTrue(syncObj.manifest.enableRetrieveAuthSession())
    }

    @Test
    fun `enableRetrieveAuthSession true when disable feature flag is false`() {
        val syncObj = syncResponse(
            sessionManifest().copy(
                features = mapOf(
                    "bank_connections_disable_defensive_auth_session_retrieval_on_complete" to false
                )
            ),
        )
        assertTrue(syncObj.manifest.enableRetrieveAuthSession())
    }

    @Test
    fun `enableRetrieveAuthSession false when disable feature flag is true`() {
        val syncObj = syncResponse(
            sessionManifest().copy(
                features = mapOf(
                    "bank_connections_disable_defensive_auth_session_retrieval_on_complete" to true
                )
            ),
        )
        assertFalse(syncObj.manifest.enableRetrieveAuthSession())
    }
}
