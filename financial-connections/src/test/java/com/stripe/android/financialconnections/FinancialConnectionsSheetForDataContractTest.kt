package com.stripe.android.financialconnections

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForDataContract
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import java.security.InvalidParameterException
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class FinancialConnectionsSheetForDataContractTest {

    @Test
    fun `parseResult() with missing data should return failed result`() {
        assertThat(FinancialConnectionsSheetForDataContract(intentBuilder(mock())).parseResult(0, Intent()))
            .isInstanceOf(FinancialConnectionsSheetResult.Failed::class.java)
    }

    @Test
    fun `validate() valid args`() {
        val configuration = FinancialConnectionsSheetConfiguration(
            ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
            ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
        )
        val args = FinancialConnectionsSheetActivityArgs.ForData(configuration)
        args.validate()
    }

    @Test
    fun `validate() missing session client secret`() {
        val configuration = FinancialConnectionsSheetConfiguration(
            " ",
            ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
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
            " "
        )
        val args = FinancialConnectionsSheetActivityArgs.ForData(configuration)
        assertFailsWith<InvalidParameterException>(
            "The publishable key cannot be an empty string."
        ) {
            args.validate()
        }
    }
}
