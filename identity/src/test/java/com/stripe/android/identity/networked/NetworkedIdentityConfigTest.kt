package com.stripe.android.identity.networked

import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.SUCCESS_VERIFICATION_PAGE_REQUIRE_LIVE_CAPTURE
import com.stripe.android.identity.networking.models.NetworkedIdentityRoute
import com.stripe.android.identity.networking.models.VerificationPageNetworkedIdentity
import com.stripe.android.identity.networking.models.VerificationPageProvidedDetails
import org.junit.Test

class NetworkedIdentityConfigTest {

    @Test
    fun `without overrides everything comes from the page`() {
        val config = NetworkedIdentityConfig.from(page(), overrides = null)

        assertThat(config.route).isEqualTo(NetworkedIdentityRoute.Reuse)
        assertThat(config.merchantPublishableKey).isEqualTo("pk_page")
        assertThat(config.merchantEmail).isEqualTo("networked@example.com")
        assertThat(config.seedSavedDocuments).isFalse()
    }

    @Test
    fun `without a networked identity email the provided email is used`() {
        val config = NetworkedIdentityConfig.from(page(networkedEmail = null), overrides = null)

        assertThat(config.merchantEmail).isEqualTo("provided@example.com")
    }

    @Test
    fun `without networked identity fields nothing is offered`() {
        val config = NetworkedIdentityConfig.from(
            page().copy(merchantPublishableKey = null, networkedIdentity = null),
            overrides = null,
        )

        assertThat(config.route).isEqualTo(NetworkedIdentityRoute.OrdinaryIdentity)
        assertThat(config.merchantPublishableKey).isNull()
    }

    @Test
    fun `overrides win over the page`() {
        val config = NetworkedIdentityConfig.from(page(), overrides(route = NetworkedIdentityRoute.Save))

        assertThat(config.route).isEqualTo(NetworkedIdentityRoute.Save)
        assertThat(config.merchantPublishableKey).isEqualTo("pk_override")
        assertThat(config.merchantEmail).isEqualTo("person@example.com")
        assertThat(config.seedSavedDocuments).isTrue()
    }

    @Test
    fun `null override route keeps the page route`() {
        val config = NetworkedIdentityConfig.from(page(), overrides(route = null))

        assertThat(config.route).isEqualTo(NetworkedIdentityRoute.Reuse)
    }

    private fun page(networkedEmail: String? = "networked@example.com") =
        SUCCESS_VERIFICATION_PAGE_REQUIRE_LIVE_CAPTURE.copy(
            merchantPublishableKey = "pk_page",
            providedDetails = VerificationPageProvidedDetails(email = "provided@example.com"),
            networkedIdentity = VerificationPageNetworkedIdentity(
                saveAvailable = true,
                reuseAvailable = true,
                email = networkedEmail,
                phoneNumber = null,
                state = VerificationPageNetworkedIdentity.State(consented = false, skipped = false, direction = null),
            ),
        )

    private fun overrides(route: NetworkedIdentityRoute?) = NetworkedIdentityDebugOverrides(
        route = route,
        merchantPublishableKey = "pk_override",
        merchantEmail = "person@example.com",
        seedSavedDocuments = true,
    )
}
