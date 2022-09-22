package com.stripe.android.financialconnections.features.consent

import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.ui.TextResource
import org.junit.Test

class ConsentTextBuilderTest {

    @Test
    fun `getDataBullets - When account numbers and payment method are present, no duplicated texts are shown`() {
        val manifestWithDuplicatedPermissions = ApiKeyFixtures.sessionManifest().copy(
            permissions = listOf(
                FinancialConnectionsAccount.Permissions.ACCOUNT_NUMBERS,
                FinancialConnectionsAccount.Permissions.PAYMENT_METHOD
            )
        )
        val result: List<Pair<TextResource, TextResource>> =
            ConsentTextBuilder.getRequestedDataBullets(
                manifestWithDuplicatedPermissions
            )

        assertThat(result).containsExactly(
            Pair(
                TextResource.StringId(R.string.stripe_consent_requested_data_accountdetails_title),
                TextResource.StringId(R.string.stripe_consent_requested_data_accountdetails_desc)
            )
        )
    }
}