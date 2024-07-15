package com.stripe.android.financialconnections.features.consent

import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.domain.LookupAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.INSTITUTION_PICKER
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.LINK_LOGIN
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.NETWORKING_LINK_LOGIN_WARMUP
import com.stripe.android.financialconnections.utils.FakeFinancialConnectionsConsumerSessionRepository
import com.stripe.android.model.ConsumerSessionLookup
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DetermineNextPaneFromConsentTest {

    @Test
    fun `Returns Link warmup pane if consumer exists in Instant Debits flow`() = runTest {
        val determineNextPaneFromConsent = makeDetermineNextPaneFromConsent(
            onLookupConsumerSession = { ConsumerSessionLookup(exists = true) },
        )

        val manifest = ApiKeyFixtures.sessionManifest().copy(
            isLinkWithStripe = true,
            accountholderCustomerEmailAddress = "email@email.com",
            nextPane = LINK_LOGIN,
        )

        val nextPane = determineNextPaneFromConsent(manifest)
        assertThat(nextPane).isEqualTo(NETWORKING_LINK_LOGIN_WARMUP)
    }

    @Test
    fun `Returns server-provided pane if no consumer exists in Instant Debits flow`() = runTest {
        val determineNextPaneFromConsent = makeDetermineNextPaneFromConsent(
            onLookupConsumerSession = { ConsumerSessionLookup(exists = false) },
        )

        val manifest = ApiKeyFixtures.sessionManifest().copy(
            isLinkWithStripe = true,
            accountholderCustomerEmailAddress = "email@email.com",
            nextPane = LINK_LOGIN,
        )

        val nextPane = determineNextPaneFromConsent(manifest)
        assertThat(nextPane).isEqualTo(LINK_LOGIN)
    }

    @Test
    fun `Returns server-provided pane if no customer email provided in Instant Debits flow`() = runTest {
        val determineNextPaneFromConsent = makeDetermineNextPaneFromConsent(
            onLookupConsumerSession = { error("Not expected to call lookupConsumerSession() in this test.") },
        )

        val manifest = ApiKeyFixtures.sessionManifest().copy(
            isLinkWithStripe = true,
            accountholderCustomerEmailAddress = null,
            nextPane = LINK_LOGIN,
        )

        val nextPane = determineNextPaneFromConsent(manifest)
        assertThat(nextPane).isEqualTo(LINK_LOGIN)
    }

    @Test
    fun `Returns server-provided pane if not in Instant Debits flow`() = runTest {
        val determineNextPaneFromConsent = makeDetermineNextPaneFromConsent(
            onLookupConsumerSession = { error("Not expected to call lookupConsumerSession() in this test.") },
        )

        val manifest = ApiKeyFixtures.sessionManifest().copy(
            isLinkWithStripe = false,
            accountholderCustomerEmailAddress = "email@email.com",
            nextPane = INSTITUTION_PICKER,
        )

        val nextPane = determineNextPaneFromConsent(manifest)
        assertThat(nextPane).isEqualTo(INSTITUTION_PICKER)
    }

    private fun makeDetermineNextPaneFromConsent(
        onLookupConsumerSession: () -> ConsumerSessionLookup,
    ): DetermineNextPaneFromConsent {
        val repository = object : FakeFinancialConnectionsConsumerSessionRepository() {
            override suspend fun lookupConsumerSession(email: String, clientSecret: String): ConsumerSessionLookup {
                return onLookupConsumerSession()
            }
        }

        return DetermineNextPaneFromConsent(
            lookupAccount = LookupAccount(
                consumerSessionRepository = repository,
                configuration = FinancialConnectionsSheet.Configuration(
                    ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
                    ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
                ),
            )
        )
    }
}
