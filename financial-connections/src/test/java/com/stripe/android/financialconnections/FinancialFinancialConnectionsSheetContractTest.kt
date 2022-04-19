package com.stripe.android.financialconnections

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.InvalidParameterException
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class FinancialFinancialConnectionsSheetContractTest {

    @Test
    fun `parseResult() with missing data should return failed result`() {
        assertThat(FinancialConnectionsSheetContract().parseResult(0, Intent()))
            .isInstanceOf(FinancialConnectionsSheetResult.Failed::class.java)
    }

    @Test
    fun `validate() valid args`() {
        val configuration = FinancialConnectionsSheet.Configuration(
            ApiKeyFixtures.DEFAULT_LINK_ACCOUNT_SESSION_SECRET,
            ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
        )
        val args = FinancialConnectionsSheetContract.Args(configuration)
        args.validate()
    }

    @Test
    fun `validate() missing link account session client secret`() {
        val configuration = FinancialConnectionsSheet.Configuration(
            " ",
            ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
        )
        val args = FinancialConnectionsSheetContract.Args(configuration)
        assertFailsWith<InvalidParameterException>(
            "The link account session client secret cannot be an empty string."
        ) {
            args.validate()
        }
    }

    @Test
    fun `validate() missing publishable key`() {
        val configuration = FinancialConnectionsSheet.Configuration(
            ApiKeyFixtures.DEFAULT_LINK_ACCOUNT_SESSION_SECRET,
            " "
        )
        val args = FinancialConnectionsSheetContract.Args(configuration)
        assertFailsWith<InvalidParameterException>(
            "The publishable key cannot be an empty string."
        ) {
            args.validate()
        }
    }
}
