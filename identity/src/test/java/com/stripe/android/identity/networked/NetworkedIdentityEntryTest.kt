package com.stripe.android.identity.networked

import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.networking.models.NetworkedIdentityRoute
import org.junit.Test

internal class NetworkedIdentityEntryTest {

    @Test
    fun `a found saved document shows the chip with the provided email`() {
        val entry = entry(niConfig(merchantEmail = "person@example.com", savedDocumentAvailable = true))

        assertThat(entry).isEqualTo(
            NetworkedIdentityEntry(offersReuse = true, offersSave = true, savedIdEmail = "person@example.com")
        )
    }

    @Test
    fun `a handed-in session's email is used for the chip`() {
        val entry = entry(
            niConfig(merchantEmail = "merchant@example.com", savedDocumentAvailable = true),
            handoff = niHandoff(),
        )

        assertThat(entry.savedIdEmail).isEqualTo("person@example.com")
    }

    @Test
    fun `reuse can be tried without a found document, without the chip`() {
        val entry = entry(niConfig(merchantEmail = "person@example.com", savedDocumentAvailable = false))

        assertThat(entry).isEqualTo(
            NetworkedIdentityEntry(offersReuse = true, offersSave = true, savedIdEmail = null)
        )
    }

    @Test
    fun `without an email Link is offered without the chip`() {
        val entry = entry(niConfig(savedDocumentAvailable = true))

        assertThat(entry.offersReuse).isTrue()
        assertThat(entry.savedIdEmail).isNull()
    }

    @Test
    fun `without a merchant publishable key nothing is offered`() {
        val entry = entry(
            niConfig(
                merchantPublishableKey = null,
                merchantEmail = "person@example.com",
                savedDocumentAvailable = true,
            )
        )

        assertThat(entry).isEqualTo(
            NetworkedIdentityEntry(offersReuse = false, offersSave = false, savedIdEmail = null)
        )
    }

    @Test
    fun `save route offers save but not reuse`() {
        val entry = entry(niConfig(route = NetworkedIdentityRoute.Save, savedDocumentAvailable = true))

        assertThat(entry.offersReuse).isFalse()
        assertThat(entry.offersSave).isTrue()
        assertThat(entry.savedIdEmail).isNull()
    }

    @Test
    fun `a prepared save isn't offered again`() {
        val entry = entry(niConfig(route = NetworkedIdentityRoute.ResumeSave))

        assertThat(entry.offersSave).isFalse()
    }

    @Test
    fun `reuse route without save enabled doesn't offer save`() {
        val entry = entry(niConfig(saveEnabled = false))

        assertThat(entry.offersSave).isFalse()
    }

    @Test
    fun `skipped sessions don't offer Networked Identity`() {
        val entry = entry(
            niConfig(route = NetworkedIdentityRoute.OrdinaryIdentity, savedDocumentAvailable = true),
            handoff = niHandoff(),
        )

        assertThat(entry).isEqualTo(
            NetworkedIdentityEntry(offersReuse = false, offersSave = false, savedIdEmail = null)
        )
    }

    @Test
    fun `a shared document isn't offered for reuse or saving again`() {
        val entry = entry(
            niConfig(route = NetworkedIdentityRoute.ResumeReuse, savedDocumentAvailable = true),
            handoff = niHandoff(),
        )

        assertThat(entry).isEqualTo(
            NetworkedIdentityEntry(offersReuse = false, offersSave = false, savedIdEmail = null)
        )
    }

    private fun entry(
        config: NetworkedIdentityConfig,
        handoff: IdentityVerificationSheet.Configuration.LinkSessionHandoff? = null,
    ) = NetworkedIdentityEntry(config = config, handoff = handoff, route = config.route)
}
