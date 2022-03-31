package com.stripe.android.connections

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.InvalidParameterException
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class ConnectionsSheetContractTest {

    @Test
    fun `parseResult() with missing data should return failed result`() {
        assertThat(ConnectionsSheetContract().parseResult(0, Intent()))
            .isInstanceOf(ConnectionsSheetResult.Failed::class.java)
    }

    @Test
    fun `validate() valid args`() {
        val configuration = ConnectionsSheet.Configuration(
            ApiKeyFixtures.DEFAULT_LINK_ACCOUNT_SESSION_SECRET,
            ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
        )
        val args = ConnectionsSheetContract.Args(configuration)
        args.validate()
    }

    @Test
    fun `validate() missing link account session client secret`() {
        val configuration = ConnectionsSheet.Configuration(
            " ",
            ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
        )
        val args = ConnectionsSheetContract.Args(configuration)
        assertFailsWith<InvalidParameterException>(
            "The link account session client secret cannot be an empty string."
        ) {
            args.validate()
        }
    }

    @Test
    fun `validate() missing publishable key`() {
        val configuration = ConnectionsSheet.Configuration(
            ApiKeyFixtures.DEFAULT_LINK_ACCOUNT_SESSION_SECRET,
            " "
        )
        val args = ConnectionsSheetContract.Args(configuration)
        assertFailsWith<InvalidParameterException>(
            "The publishable key cannot be an empty string."
        ) {
            args.validate()
        }
    }
}
