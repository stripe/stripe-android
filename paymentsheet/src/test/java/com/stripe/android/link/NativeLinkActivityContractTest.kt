package com.stripe.android.link

import android.app.Activity
import android.content.Intent
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethodFixtures
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
        val contract = NativeLinkActivityContract(paymentElementCallbackIdentifier = LINK_CALLBACK_TEST_IDENTIFIER)
        val args = LinkActivityContract.Args(
            configuration = TestFactory.LINK_CONFIGURATION,
            startWithVerificationDialog = false,
            linkAccount = TestFactory.LINK_ACCOUNT
        )

        val intent = contract.createIntent(ApplicationProvider.getApplicationContext(), args)

        assertThat(intent.component?.className).isEqualTo(LinkActivity::class.java.name)

        val actualArg = intent.extras?.let {
            BundleCompat.getParcelable(it, LinkActivity.EXTRA_ARGS, NativeLinkArgs::class.java)
        }
        assertThat(actualArg).isEqualTo(
            NativeLinkArgs(
                configuration = TestFactory.LINK_CONFIGURATION,
                publishableKey = "pk_test_abcdefg",
                stripeAccountId = null,
                startWithVerificationDialog = false,
                linkAccount = TestFactory.LINK_ACCOUNT,
                paymentElementCallbackIdentifier = LINK_CALLBACK_TEST_IDENTIFIER,
            )
        )
    }

    @Test
    fun `complete with result from native link`() {
        val expectedResult = LinkActivityResult.PaymentMethodObtained(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )

        val contract = NativeLinkActivityContract(paymentElementCallbackIdentifier = LINK_CALLBACK_TEST_IDENTIFIER)

        val result = contract.parseResult(
            resultCode = LinkActivity.RESULT_COMPLETE,
            intent = intent(expectedResult)
        )

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `complete with canceled result when result not found`() {
        val contract = NativeLinkActivityContract(paymentElementCallbackIdentifier = LINK_CALLBACK_TEST_IDENTIFIER)

        val result = contract.parseResult(
            resultCode = LinkActivity.RESULT_COMPLETE,
            intent = Intent()
        )

        assertThat(result)
            .isEqualTo(
                LinkActivityResult.Canceled(
                    linkAccountUpdate = LinkAccountUpdate.None
                )
            )
    }

    @Test
    fun `unknown result code results in canceled`() {
        val contract = NativeLinkActivityContract(paymentElementCallbackIdentifier = LINK_CALLBACK_TEST_IDENTIFIER)

        val result = contract.parseResult(42, Intent())

        assertThat(result)
            .isEqualTo(
                LinkActivityResult.Canceled(
                    linkAccountUpdate = LinkAccountUpdate.None
                )
            )
    }

    @Test
    fun `canceled result code is handled correctly`() {
        val contract = NativeLinkActivityContract(paymentElementCallbackIdentifier = LINK_CALLBACK_TEST_IDENTIFIER)

        val result = contract.parseResult(Activity.RESULT_CANCELED, Intent())

        assertThat(result)
            .isEqualTo(
                LinkActivityResult.Canceled(
                    linkAccountUpdate = LinkAccountUpdate.None
                )
            )
    }

    private fun intent(result: LinkActivityResult): Intent {
        val bundle = bundleOf(
            LinkActivityContract.EXTRA_RESULT to result
        )
        return Intent().apply {
            putExtras(bundle)
        }
    }

    private companion object {
        const val LINK_CALLBACK_TEST_IDENTIFIER = "LinkTestIdentifier"
    }
}
