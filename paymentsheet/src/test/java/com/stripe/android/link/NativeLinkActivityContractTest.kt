package com.stripe.android.link

import android.app.Activity
import android.content.Intent
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.checkout.checkoutConfirmationRequestId
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.RequestSurface
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NativeLinkActivityContractTest {
    private val savedStateHandle = SavedStateHandle()

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
        val contract = NativeLinkActivityContract(
            savedStateHandle = savedStateHandle,
            paymentElementCallbackIdentifier = LINK_CALLBACK_TEST_IDENTIFIER,
            requestSurface = REQUEST_SURFACE
        )
        val args = LinkActivityContract.Args(
            configuration = TestFactory.LINK_CONFIGURATION,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            linkExpressMode = LinkExpressMode.DISABLED,
            linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
            launchMode = LinkLaunchMode.Full(showSecondaryButton = true),
            statusBarColor = 0x00FF00,
        )

        val intent = contract.createIntent(ApplicationProvider.getApplicationContext(), args)

        assertThat(intent.component?.className).isEqualTo(LinkActivity::class.java.name)

        val actualArg = intent.extras?.let {
            BundleCompat.getParcelable(it, LinkActivity.EXTRA_ARGS, NativeLinkArgs::class.java)
        }
        assertThat(actualArg).isEqualTo(
            NativeLinkArgs(
                configuration = TestFactory.LINK_CONFIGURATION,
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
                requestSurface = REQUEST_SURFACE,
                apiConfiguration = PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG,
                linkExpressMode = LinkExpressMode.DISABLED,
                linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
                paymentElementCallbackIdentifier = LINK_CALLBACK_TEST_IDENTIFIER,
                launchMode = LinkLaunchMode.Full(showSecondaryButton = true),
                statusBarColor = 0x00FF00,
            )
        )
    }

    @Test
    fun `complete with result from native link`() {
        val expectedResult = LinkActivityResult.PaymentMethodObtained(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )

        val contract = createContract()

        val result = contract.parseResult(
            resultCode = LinkActivity.RESULT_COMPLETE,
            intent = intent(expectedResult)
        )

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `complete result without request ID preserves caller request ID`() {
        val expectedResult = LinkActivityResult.PaymentMethodObtained(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )
        val contract = createContract()
        savedStateHandle.checkoutConfirmationRequestId = "req_existing"
        val resultWithoutRequestId = contract.parseResult(
            resultCode = LinkActivity.RESULT_COMPLETE,
            intent = intent(expectedResult)
        )
        assertThat(resultWithoutRequestId).isEqualTo(expectedResult)
        assertThat(savedStateHandle.checkoutConfirmationRequestId).isEqualTo("req_existing")
    }

    @Test
    fun `complete with canceled result when result not found`() {
        val contract = createContract()
        savedStateHandle.checkoutConfirmationRequestId = "req_existing"

        val result = contract.parseResult(
            resultCode = LinkActivity.RESULT_COMPLETE,
            intent = Intent().putExtra(LinkActivity.EXTRA_CONFIRMATION_REQUEST_ID, "req_returned")
        )

        assertThat(result)
            .isEqualTo(
                LinkActivityResult.Canceled(
                    linkAccountUpdate = LinkAccountUpdate.None
                )
            )
        assertThat(savedStateHandle.checkoutConfirmationRequestId).isEqualTo("req_existing")
    }

    @Test
    fun `unknown result code results in canceled`() {
        val contract = createContract()
        savedStateHandle.checkoutConfirmationRequestId = "req_existing"

        val result = contract.parseResult(
            42,
            Intent().putExtra(LinkActivity.EXTRA_CONFIRMATION_REQUEST_ID, "req_returned")
        )

        assertThat(result)
            .isEqualTo(
                LinkActivityResult.Canceled(
                    linkAccountUpdate = LinkAccountUpdate.None
                )
            )
        assertThat(savedStateHandle.checkoutConfirmationRequestId).isEqualTo("req_existing")
    }

    @Test
    fun `canceled result code is handled correctly`() {
        val contract = createContract()
        savedStateHandle.checkoutConfirmationRequestId = "req_existing"

        val result = contract.parseResult(
            Activity.RESULT_CANCELED,
            Intent().putExtra(LinkActivity.EXTRA_CONFIRMATION_REQUEST_ID, "req_returned")
        )

        assertThat(result)
            .isEqualTo(
                LinkActivityResult.Canceled(
                    linkAccountUpdate = LinkAccountUpdate.None
                )
            )
        assertThat(savedStateHandle.checkoutConfirmationRequestId).isEqualTo("req_existing")
    }

    @Test
    fun `complete canceled result transfers request ID`() {
        verifyRequestIdTransferred(
            LinkActivityResult.Canceled(linkAccountUpdate = LinkAccountUpdate.None)
        )
    }

    @Test
    fun `complete success result transfers request ID`() {
        verifyRequestIdTransferred(
            LinkActivityResult.Completed(linkAccountUpdate = LinkAccountUpdate.None)
        )
    }

    @Test
    fun `complete failure result transfers request ID`() {
        verifyRequestIdTransferred(
            LinkActivityResult.Failed(
                error = Throwable("failure"),
                linkAccountUpdate = LinkAccountUpdate.None,
            )
        )
    }

    private fun verifyRequestIdTransferred(expectedResult: LinkActivityResult) {
        val requestId = "req_native_link"
        val contract = createContract()

        val actualResult = contract.parseResult(
            resultCode = LinkActivity.RESULT_COMPLETE,
            intent = intent(expectedResult, requestId)
        )

        assertThat(actualResult).isEqualTo(expectedResult)
        assertThat(savedStateHandle.checkoutConfirmationRequestId).isEqualTo(requestId)
    }

    private fun createContract(): NativeLinkActivityContract = NativeLinkActivityContract(
        savedStateHandle = savedStateHandle,
        paymentElementCallbackIdentifier = LINK_CALLBACK_TEST_IDENTIFIER,
        requestSurface = REQUEST_SURFACE,
    )

    private fun intent(result: LinkActivityResult): Intent {
        val bundle = bundleOf(
            LinkActivityContract.EXTRA_RESULT to result
        )
        return Intent().apply {
            putExtras(bundle)
        }
    }

    private fun intent(result: LinkActivityResult, requestId: String): Intent =
        intent(result).putExtra(LinkActivity.EXTRA_CONFIRMATION_REQUEST_ID, requestId)

    private companion object {
        const val LINK_CALLBACK_TEST_IDENTIFIER = "LinkTestIdentifier"
        val REQUEST_SURFACE = RequestSurface.PaymentElement
    }
}
