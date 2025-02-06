package com.stripe.android.link.express

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.TestFactory
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkExpressContractTest {

    @Before
    fun setup() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            publishableKey = PUBLISHABLE_KEY,
            stripeAccountId = STRIPE_ACCOUNT_ID
        )
    }

    @After
    fun teardown() {
        PaymentConfiguration.clearInstance()
    }

    @Test
    fun `createIntent populates the correct extras`() {
        val contract = LinkExpressContract()
        val testArgs = LinkExpressContract.Args(
            configuration = TestFactory.LINK_CONFIGURATION,
            linkAccount = TestFactory.LINK_ACCOUNT
        )

        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = contract.createIntent(context, testArgs)

        // Verify that the Intent targets LinkExpressActivity
        assertThat(intent.component?.className).isEqualTo(LinkExpressActivity::class.java.name)

        // Extract the LinkExpressArgs from the Intent extras
        val actualArgs = intent.extras?.let {
            BundleCompat.getParcelable(it, LinkExpressActivity.EXTRA_ARGS, LinkExpressArgs::class.java)
        }

        // Construct the expected LinkExpressArgs based on the given test args
        val expectedArgs = LinkExpressArgs(
            configuration = testArgs.configuration,
            stripeAccountId = STRIPE_ACCOUNT_ID,
            publishableKey = PUBLISHABLE_KEY,
            linkAccount = testArgs.linkAccount
        )

        // Compare the actual and expected LinkExpressArgs
        assertThat(actualArgs).isEqualTo(expectedArgs)
    }

    @Test
    fun `parseResult returns valid result if result code is RESULT_COMPLETE`() {
        val contract = LinkExpressContract()
        val expectedResult = LinkExpressResult.Authenticated(LinkAccountUpdate.None) // Example result type

        val actualResult = contract.parseResult(
            LinkExpressActivity.RESULT_COMPLETE,
            createIntentWithResult(expectedResult)
        )
        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun `parseResult returns Canceled when RESULT_COMPLETE with no data`() {
        val contract = LinkExpressContract()

        val actualResult = contract.parseResult(
            LinkExpressActivity.RESULT_COMPLETE,
            Intent() // no extras
        )
        assertThat(actualResult).isEqualTo(
            LinkExpressResult.Canceled(linkAccountUpdate = LinkAccountUpdate.None)
        )
    }

    @Test
    fun `parseResult returns Canceled when Activity RESULT_CANCELED`() {
        val contract = LinkExpressContract()

        val actualResult = contract.parseResult(Activity.RESULT_CANCELED, Intent())
        assertThat(actualResult).isEqualTo(
            LinkExpressResult.Canceled(linkAccountUpdate = LinkAccountUpdate.None)
        )
    }

    @Test
    fun `parseResult returns Canceled for an unknown result code`() {
        val contract = LinkExpressContract()

        val actualResult = contract.parseResult(
            resultCode = 998,
            intent = Intent()
        )
        assertThat(actualResult).isEqualTo(
            LinkExpressResult.Canceled(linkAccountUpdate = LinkAccountUpdate.None)
        )
    }

    /**
     * Helper to build an Intent with a given LinkExpressResult.
     */
    private fun createIntentWithResult(result: LinkExpressResult): Intent {
        return Intent().apply {
            putExtras(
                bundleOf(
                    LinkExpressContract.EXTRA_RESULT to result
                )
            )
        }
    }

    companion object {
        private const val STRIPE_ACCOUNT_ID = "stp_1234"
        private const val PUBLISHABLE_KEY = "pk_test_1234"
    }
}
