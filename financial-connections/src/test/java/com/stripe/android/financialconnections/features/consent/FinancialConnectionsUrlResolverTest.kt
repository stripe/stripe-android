package com.stripe.android.financialconnections.features.consent

import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.features.consent.FinancialConnectionsUrlResolver.getDisconnectUrl
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.AccountDisconnectionMethod
import com.stripe.android.financialconnections.presentation.FinancialConnectionsUrls
import org.junit.Test

@Suppress("MaxLineLength")
class FinancialConnectionsUrlResolverTest {

    @Test
    fun `getDisconnectUrl returns supportEndUser URL when disconnection method is SUPPORT and isEndUserFacing is true`() {
        val manifest = ApiKeyFixtures.sessionManifest().copy(
            accountDisconnectionMethod = AccountDisconnectionMethod.SUPPORT,
            isEndUserFacing = true
        )

        val expectedUrl = FinancialConnectionsUrls.Disconnect.supportEndUser
        assertThat(expectedUrl).isEqualTo(getDisconnectUrl(manifest))
    }

    @Test
    fun `getDisconnectUrl returns supportMerchantUser URL when disconnection method is SUPPORT and isEndUserFacing is false`() {
        val manifest = ApiKeyFixtures.sessionManifest().copy(
            accountDisconnectionMethod = AccountDisconnectionMethod.SUPPORT,
            isEndUserFacing = false
        )

        val expectedUrl = FinancialConnectionsUrls.Disconnect.supportMerchantUser
        assertThat(expectedUrl).isEqualTo(getDisconnectUrl(manifest))
    }

    @Test
    fun `getDisconnectUrl returns dashboard URL when disconnection method is DASHBOARD`() {
        val manifest = ApiKeyFixtures.sessionManifest().copy(
            accountDisconnectionMethod = AccountDisconnectionMethod.DASHBOARD
        )

        val expectedUrl = FinancialConnectionsUrls.Disconnect.dashboard
        assertThat(expectedUrl).isEqualTo(getDisconnectUrl(manifest))
    }

    @Test
    fun `getDisconnectUrl returns link URL when disconnection method is LINK`() {
        val manifest = ApiKeyFixtures.sessionManifest().copy(
            accountDisconnectionMethod = AccountDisconnectionMethod.LINK
        )

        val expectedUrl = FinancialConnectionsUrls.Disconnect.link
        assertThat(expectedUrl).isEqualTo(getDisconnectUrl(manifest))
    }

    @Test
    fun `getDisconnectUrl returns email URL when disconnection method is EMAIL`() {
        val manifest = ApiKeyFixtures.sessionManifest().copy(
            accountDisconnectionMethod = AccountDisconnectionMethod.EMAIL
        )

        val expectedUrl = FinancialConnectionsUrls.Disconnect.email
        assertThat(expectedUrl).isEqualTo(getDisconnectUrl(manifest))
    }

    @Test
    fun `getDisconnectUrl returns email URL when disconnection method is UNKNOWN`() {
        val manifest = ApiKeyFixtures.sessionManifest().copy(
            accountDisconnectionMethod = AccountDisconnectionMethod.UNKNOWN
        )

        val expectedUrl = FinancialConnectionsUrls.Disconnect.email
        assertThat(expectedUrl).isEqualTo(getDisconnectUrl(manifest))
    }
}
