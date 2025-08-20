package com.stripe.android.financialconnections

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForDataContract
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.InvalidParameterException
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class FinancialConnectionsSheetForDataContractTest {

    private fun intentBuilder(args: FinancialConnectionsSheetActivityArgs): Intent {
        return Intent()
    }

    @Test
    fun `parseResult() with RESULT_CANCELED should return canceled result`() {
        val contract = FinancialConnectionsSheetForDataContract(::intentBuilder)
        val result = contract.parseResult(android.app.Activity.RESULT_CANCELED, Intent())

        assertThat(result).isInstanceOf(FinancialConnectionsSheetResult.Canceled::class.java)
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
