package com.stripe.android.financialconnections

import android.content.Intent
import android.os.Parcel
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForDataContract
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.InvalidParameterException
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class FinancialConnectionsSheetForDataContractTest {

    @Test
    fun `parseResult() with RESULT_CANCELED should return canceled result`() {
        val contract = FinancialConnectionsSheetForDataContract { Intent() }
        val result = contract.parseResult(android.app.Activity.RESULT_CANCELED, null)

        assertThat(result).isInstanceOf(FinancialConnectionsSheetResult.Canceled::class.java)
    }

    @Test
    fun `configuration preserves connected account credentials through parceling`() {
        assertConfigurationParcelRoundTrip(
            ApiConfiguration.State(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY, "acct_test")
        )
    }

    @Test
    fun `configuration preserves null account credentials through parceling`() {
        assertConfigurationParcelRoundTrip(
            ApiConfiguration.State(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY, null)
        )
    }

    @Test
    fun `validate() valid args`() {
        val configuration = FinancialConnectionsSheetConfiguration(
            ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
            ApiConfiguration.State(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY, null),
            preCollectedConsent = null,
        )
        val args = FinancialConnectionsSheetActivityArgs.ForData(configuration)
        args.validate()
    }

    @Test
    fun `validate() missing session client secret`() {
        val configuration = FinancialConnectionsSheetConfiguration(
            " ",
            ApiConfiguration.State(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY, null),
            preCollectedConsent = null,
        )
        val args = FinancialConnectionsSheetActivityArgs.ForData(configuration)
        assertFailsWith<InvalidParameterException>(
            "The financial connections session client secret cannot be an empty string."
        ) {
            args.validate()
        }
    }

    @Test
    fun `validate() missing publishable key`() {
        val configuration = FinancialConnectionsSheetConfiguration(
            ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
            ApiConfiguration.State(" ", null),
            preCollectedConsent = null,
        )
        val args = FinancialConnectionsSheetActivityArgs.ForData(configuration)
        assertFailsWith<InvalidParameterException>(
            "The publishable key cannot be an empty string."
        ) {
            args.validate()
        }
    }

    @Suppress("DEPRECATION")
    private fun assertConfigurationParcelRoundTrip(apiConfiguration: ApiConfiguration.State) {
        val configuration = FinancialConnectionsSheetConfiguration(
            financialConnectionsSessionClientSecret = ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
            apiConfiguration = apiConfiguration,
            preCollectedConsent = null,
        )
        val parcel = Parcel.obtain()
        try {
            parcel.writeParcelable(configuration, 0)
            parcel.setDataPosition(0)
            val restored = parcel.readParcelable<FinancialConnectionsSheetConfiguration>(
                FinancialConnectionsSheetConfiguration::class.java.classLoader
            )
            assertThat(restored).isEqualTo(configuration)
        } finally {
            parcel.recycle()
        }
    }
}
