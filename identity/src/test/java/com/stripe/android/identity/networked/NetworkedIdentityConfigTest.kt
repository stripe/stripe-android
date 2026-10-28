package com.stripe.android.identity.networked

import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.SUCCESS_VERIFICATION_PAGE_REQUIRE_LIVE_CAPTURE
import com.stripe.android.identity.networking.models.NetworkedIdentityRoute
import com.stripe.android.identity.networking.models.VerificationPageNetworkedIdentity
import org.junit.Test

class NetworkedIdentityConfigTest {

    @Test
    fun `the page drives routing and the host supplies the key`() {
        val config = NetworkedIdentityConfig.from(page(), OPTIONS)

        assertThat(config.route).isEqualTo(NetworkedIdentityRoute.Reuse)
        assertThat(config.merchantPublishableKey).isEqualTo("pk_host")
        assertThat(config.merchantEmail).isEqualTo("networked@example.com")
        assertThat(config.saveEnabled).isTrue()
        assertThat(config.savedDocumentAvailable).isTrue()
        assertThat(config.phoneNumberAutofillHint).isEqualTo("+15555551234")
    }

    @Test
    fun `without networked identity fields nothing is offered`() {
        val page = page().copy(networkedIdentity = null)

        assertThat(page.networkedIdentityConfig(OPTIONS)).isNull()
    }

    @Test
    fun `without the host's key nothing is offered`() {
        val options = OPTIONS.copy(merchantPublishableKey = null)

        assertThat(page().networkedIdentityConfig(options)).isNull()
    }

    private fun page() =
        SUCCESS_VERIFICATION_PAGE_REQUIRE_LIVE_CAPTURE.copy(
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
        val OPTIONS = IdentityVerificationSheet.Configuration.NetworkedIdentityOptions(
            linkSessionHandoff = null,
            merchantPublishableKey = "pk_host",
        )
    }
}
