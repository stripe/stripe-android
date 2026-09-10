package com.stripe.android.financialconnections.utils

import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import org.junit.Test

internal class HostedAuthUrlBuilderTest {

    @Test
    fun `permissioned sessions do not change the hosted auth URL`() {
        val configuration = FinancialConnectionsSheetConfiguration(
            financialConnectionsSessionClientSecret = "fcsess_secret",
            publishableKey = "pk_merchant",
            stripeAccountId = "acct_merchant",
            hasRequestedDataPermissions = false,
        )
        val hostedAuthUrl = "https://connect.stripe.com/hosted_auth"

        val standardUrl = HostedAuthUrlBuilder.create(
            args = FinancialConnectionsSheetActivityArgs.ForData(configuration),
            hostedAuthUrl = hostedAuthUrl,
        )
        val permissionedUrl = HostedAuthUrlBuilder.create(
            args = FinancialConnectionsSheetActivityArgs.ForData(
                configuration.copy(hasRequestedDataPermissions = true)
            ),
            hostedAuthUrl = hostedAuthUrl,
        )

        assertThat(permissionedUrl).isEqualTo(standardUrl)
        assertThat(permissionedUrl).doesNotContain("consumer_session_client_secret")
        assertThat(permissionedUrl).doesNotContain("consumer_publishable_key")
        assertThat(permissionedUrl).doesNotContain("consumer_email")
    }
}
