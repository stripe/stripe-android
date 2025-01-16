package com.stripe.android.link

import android.app.Activity
import android.content.Intent
import androidx.core.os.BundleCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NativeLinkActivityContractTest {

    @Before
    fun before() {
        PaymentConfiguration.init(
            context = ApplicationProvider.getApplicationContext(),
            publishableKey = "pk_test_abcdefg",
        )
    }

    @After
    fun after() {
        PaymentConfiguration.clearInstance()
    }

    @Test
    fun `intent is created correctly`() {
        val contract = NativeLinkActivityContract()
        val args = LinkActivityContract.Args(TestFactory.LINK_CONFIGURATION)

        val intent = contract.createIntent(ApplicationProvider.getApplicationContext(), args)

        assertThat(intent.component?.className).isEqualTo(LinkActivity::class.java.name)

        val actualArg = intent.extras?.let {
            BundleCompat.getParcelable(it, LinkActivity.EXTRA_ARGS, NativeLinkArgs::class.java)
        }
        assertThat(actualArg).isEqualTo(
            NativeLinkArgs(
                configuration = TestFactory.LINK_CONFIGURATION,
                publishableKey = "pk_test_abcdefg",
                stripeAccountId = null
            )
        )
    }

    @Test
    fun `canceled result code is handled correctly`() {
        val contract = NativeLinkActivityContract()

        val result = contract.parseResult(Activity.RESULT_CANCELED, Intent())

        assertThat(result).isEqualTo(LinkActivityResult.Canceled())
    }
}