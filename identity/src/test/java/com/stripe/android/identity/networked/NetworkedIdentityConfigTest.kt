package com.stripe.android.identity.networked

import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.SUCCESS_VERIFICATION_PAGE_REQUIRE_LIVE_CAPTURE
import com.stripe.android.identity.networking.models.NetworkedIdentityRoute
import com.stripe.android.identity.networking.models.VerificationPageNetworkedIdentity
import org.junit.Test

class NetworkedIdentityConfigTest {

    @Test
    fun `without overrides everything comes from the page`() {
        val config = NetworkedIdentityConfig.from(page(), NO_OVERRIDES)

        assertThat(config.route).isEqualTo(NetworkedIdentityRoute.Reuse)
        assertThat(config.merchantPublishableKey).isEqualTo("pk_page")
        assertThat(config.merchantEmail).isEqualTo("networked@example.com")
        assertThat(config.seedSavedDocuments).isFalse()
        assertThat(config.saveEnabled).isTrue()
        assertThat(config.savedDocumentAvailable).isTrue()
        assertThat(config.phoneNumberAutofillHint).isEqualTo("+15555551234")
    }

    @Test
    fun `without networked identity fields nothing is offered`() {
        val config = NetworkedIdentityConfig.from(
            page().copy(merchantPublishableKey = null, networkedIdentity = null),
            NO_OVERRIDES,
        )

        assertThat(config.route).isEqualTo(NetworkedIdentityRoute.OrdinaryIdentity)
        assertThat(config.merchantPublishableKey).isNull()
    }

    @Test
    fun `overrides win over the page`() {
        val config = NetworkedIdentityConfig.from(page(), overrides(route = "save"))

        assertThat(config.route).isEqualTo(NetworkedIdentityRoute.Save)
        assertThat(config.merchantPublishableKey).isEqualTo("pk_override")
        assertThat(config.merchantEmail).isEqualTo("person@example.com")
        assertThat(config.seedSavedDocuments).isTrue()
        assertThat(config.saveEnabled).isTrue()
        assertThat(config.savedDocumentAvailable).isFalse()
    }

    @Test
    fun `an ordinary override route doesn't offer save`() {
        val config = NetworkedIdentityConfig.from(page(), overrides(route = "none"))

        assertThat(config.saveEnabled).isFalse()
        assertThat(config.savedDocumentAvailable).isFalse()
    }

    @Test
    fun `null override route keeps the page route`() {
        val config = NetworkedIdentityConfig.from(page(), overrides(route = null))

        assertThat(config.route).isEqualTo(NetworkedIdentityRoute.Reuse)
    }

    private fun page() =
        SUCCESS_VERIFICATION_PAGE_REQUIRE_LIVE_CAPTURE.copy(
            merchantPublishableKey = "pk_page",
            networkedIdentity = VerificationPageNetworkedIdentity(
                saveEnabled = true,
                reuseEnabled = true,
                reuseAvailable = true,
                lookupEmail = "networked@example.com",
                phoneNumberAutofillHint = "+15555551234",
                state = VerificationPageNetworkedIdentity.State(consented = false, skipped = false, direction = null),
            ),
        )

    private companion object {
        val NO_OVERRIDES = IdentityVerificationSheet.Configuration.NetworkedIdentityOptions(
            linkSessionHandoff = null,
            debugMerchantPublishableKey = null,
            debugProvidedEmail = null,
            debugRoute = null,
            debugSeedSavedDocuments = false,
        )
    }

    private fun overrides(route: String?) = IdentityVerificationSheet.Configuration.NetworkedIdentityOptions(
        linkSessionHandoff = null,
        debugMerchantPublishableKey = "pk_override",
        debugProvidedEmail = "person@example.com",
        debugRoute = route,
        debugSeedSavedDocuments = true,
    )
}
