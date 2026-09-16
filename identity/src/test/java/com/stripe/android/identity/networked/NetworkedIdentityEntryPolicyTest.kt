package com.stripe.android.identity.networked

import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.SUCCESS_VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE
import com.stripe.android.identity.VERIFICATION_PAGE_DATA_HAS_ERROR
import com.stripe.android.identity.networking.models.Requirement
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.networking.models.VerificationPageDataRequirements
import com.stripe.android.identity.networking.models.VerificationPageNetworkedIdentity
import com.stripe.android.identity.networking.models.VerificationPageRequirements
import org.junit.Test

internal class NetworkedIdentityEntryPolicyTest {
    @Test
    fun `preview disabled leaves eligible sessions ordinary`() = runScenario(preview = false) {
        assertThat(policy.enter(page, null)).isNull()
    }

    @Test
    fun `reuse requires the disclosure first`() = runScenario {
        assertThat(policy.enter(page.withMissing(Requirement.BIOMETRICCONSENT, Requirement.IDDOCUMENTFRONT), null))
            .isNull()
        assertThat(policy.enter(page, update())).isEqualTo(NetworkedIdentityEntry.Reuse)
    }

    @Test
    fun `reuse is attempted only once per presentation`() = runScenario {
        assertThat(policy.enter(page, null)).isEqualTo(NetworkedIdentityEntry.Reuse)
        assertThat(policy.enter(page, update())).isNull()
    }

    @Test
    fun `fresh presentation can offer the same session again`() = runScenario {
        assertThat(policy.enter(page, null)).isEqualTo(NetworkedIdentityEntry.Reuse)
        assertThat(NetworkedIdentityEntryPolicy(true).enter(page, null)).isEqualTo(NetworkedIdentityEntry.Reuse)
    }

    @Test
    fun `blank merchant key leaves ordinary capture`() = runScenario {
        assertThat(policy.enter(page.copy(merchantPublishableKey = "  "), null)).isNull()
    }

    @Test
    fun `missing merchant key leaves ordinary capture`() = runScenario {
        assertThat(policy.enter(page.copy(merchantPublishableKey = null), null)).isNull()
    }

    @Test
    fun `remaining individual fields do not restart Link`() = runScenario {
        assertThat(policy.enter(page, update().withMissing(Requirement.ADDRESS))).isNull()
    }

    @Test
    fun `latest canceled session cannot enter`() = runScenario {
        assertThat(policy.enter(page, update().copy(status = VerificationPageData.Status.CANCELED))).isNull()
    }

    @Test
    fun `latest processing session cannot enter`() = runScenario {
        assertThat(policy.enter(page, update().copy(status = VerificationPageData.Status.PROCESSING))).isNull()
    }

    @Test
    fun `latest verified session cannot enter`() = runScenario {
        assertThat(policy.enter(page, update().copy(status = VerificationPageData.Status.VERIFIED))).isNull()
    }

    @Test
    fun `latest closed session cannot enter`() = runScenario {
        assertThat(policy.enter(page, update().copy(closed = true))).isNull()
    }

    @Test
    fun `writable document fallback after submission can enter`() = runScenario {
        assertThat(policy.enter(page, update().copy(submitted = true))).isEqualTo(NetworkedIdentityEntry.Reuse)
    }

    @Test
    fun `submitted bootstrap does not restart Link`() = runScenario {
        assertThat(policy.enter(page.copy(submitted = true), null)).isNull()
    }

    @Test
    fun `updated validation error does not enter`() = runScenario {
        assertThat(policy.enter(page, VERIFICATION_PAGE_DATA_HAS_ERROR.copy(id = page.id))).isNull()
    }

    @Test
    fun `response for another session cannot enter`() = runScenario {
        assertThat(policy.enter(page, update().copy(id = "another-session"))).isNull()
    }

    @Test
    fun `missing updated requirements do not use stale bootstrap eligibility`() = runScenario {
        val incomplete = update().copy(requirements = VerificationPageDataRequirements(emptyList(), null))
        assertThat(policy.enter(page, incomplete)).isNull()
    }

    @Test
    fun `resumed reuse without missing fields submits`() = runScenario {
        val resumed = page.resuming(VerificationPageNetworkedIdentity.Direction.ConsumerToMerchant).withMissing()
        assertThat(policy.enter(resumed, null)).isEqualTo(NetworkedIdentityEntry.Submit)
        assertThat(policy.enter(resumed, null)).isNull()
    }

    @Test
    fun `resumed save without missing fields submits`() = runScenario {
        val resumed = page.resuming(VerificationPageNetworkedIdentity.Direction.MerchantToConsumer).withMissing()
        assertThat(policy.enter(resumed, null)).isEqualTo(NetworkedIdentityEntry.Submit)
    }

    @Test
    fun `resumed reuse with missing fields follows ordinary capture`() = runScenario {
        assertThat(policy.enter(page.resuming(VerificationPageNetworkedIdentity.Direction.ConsumerToMerchant), null))
            .isNull()
    }

    @Test
    fun `resumed save with missing fields follows ordinary capture`() = runScenario {
        assertThat(policy.enter(page.resuming(VerificationPageNetworkedIdentity.Direction.MerchantToConsumer), null))
            .isNull()
    }

    @Test
    fun `save offer remains ordinary until its design is implemented`() = runScenario {
        val savePage = page.copy(networkedIdentity = page.networkedIdentity?.copy(reuseAvailable = false))
        assertThat(policy.enter(savePage, null)).isNull()
    }

    @Test
    fun `persisted skip does not restart Link`() = runScenario {
        val skipped = page.copy(
            networkedIdentity = page.networkedIdentity?.copy(
                state = VerificationPageNetworkedIdentity.State(consented = false, skipped = true, direction = null)
            )
        )
        assertThat(policy.enter(skipped, null)).isNull()
    }

    private fun runScenario(preview: Boolean = true, block: Scenario.() -> Unit) {
        Scenario(NetworkedIdentityEntryPolicy(preview), eligiblePage()).block()
    }

    private data class Scenario(val policy: NetworkedIdentityEntryPolicy, val page: VerificationPage) {
        fun update() = VerificationPageData(
            id = page.id,
            objectType = "identity.verification_page",
            requirements = VerificationPageDataRequirements(emptyList(), listOf(Requirement.IDDOCUMENTFRONT)),
            status = VerificationPageData.Status.REQUIRESINPUT,
            submitted = false,
            closed = false,
        )
    }

    private fun eligiblePage() = SUCCESS_VERIFICATION_PAGE_NOT_REQUIRE_LIVE_CAPTURE.copy(
        status = VerificationPage.Status.REQUIRESINPUT,
        submitted = false,
        merchantPublishableKey = "pk_test_merchant",
        requirements = VerificationPageRequirements(listOf(Requirement.IDDOCUMENTFRONT)),
        networkedIdentity = VerificationPageNetworkedIdentity(
            saveAvailable = true,
            reuseAvailable = true,
            email = null,
            phoneNumber = null,
            state = VerificationPageNetworkedIdentity.State(consented = false, skipped = false, direction = null),
        ),
    )

    private fun VerificationPage.resuming(direction: VerificationPageNetworkedIdentity.Direction) = copy(
        networkedIdentity = networkedIdentity?.copy(
            state = VerificationPageNetworkedIdentity.State(consented = true, skipped = false, direction = direction)
        )
    )

    private fun VerificationPage.withMissing(vararg missing: Requirement) =
        copy(requirements = VerificationPageRequirements(missing.toList()))

    private fun VerificationPageData.withMissing(vararg missing: Requirement) =
        copy(requirements = VerificationPageDataRequirements(emptyList(), missing.toList()))
}
