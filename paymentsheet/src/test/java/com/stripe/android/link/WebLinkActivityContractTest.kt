package com.stripe.android.link

import android.app.Activity
import android.content.Intent
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.customersheet.FakeStripeRepository
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.FakeErrorReporter
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WebLinkActivityContractTest {

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
        val stripeRepository = object : AbsFakeStripeRepository() {
            override fun buildPaymentUserAgent(attribution: Set<String>) = "test"
        }
        val contract = contract(stripeRepository)
        val args = LinkActivityContract.Args(TestFactory.LINK_CONFIGURATION, null)

        val intent = contract.createIntent(ApplicationProvider.getApplicationContext(), args)

        assertThat(intent.component?.className).isEqualTo(LinkForegroundActivity::class.java.name)
        assertThat(intent.extras?.getString(LinkForegroundActivity.EXTRA_POPUP_URL)).startsWith(
            "https://checkout.link.com/#"
        )
    }

    @Test
    fun `parse paymentMethodObtained result correctly`() {
        val redirectUrl =
            "link-popup://complete?link_status=complete&pm=eyJpZCI6InBtXzFOSmVFckx1NW8zUDE4WnBtWHBDdElyUiIsIm9iamVjdC" +
                "I6InBheW1lbnRfbWV0aG9kIiwiYmlsbGluZ19kZXRhaWxzIjp7ImFkZHJlc3MiOnsiY2l0eSI6bnVsbCwiY291bnRyeSI6bnV" +
                "sbCwibGluZTEiOm51bGwsImxpbmUyIjpudWxsLCJwb3N0YWxfY29kZSI6bnVsbCwic3RhdGUiOm51bGx9LCJlbWFpbCI6bnVs" +
                "bCwibmFtZSI6bnVsbCwicGhvbmUiOm51bGx9LCJjYXJkIjp7ImJyYW5kIjoidmlzYSIsImNoZWNrcyI6eyJhZGRyZXNzX2xpb" +
                "mUxX2NoZWNrIjpudWxsLCJhZGRyZXNzX3Bvc3RhbF9jb2RlX2NoZWNrIjpudWxsLCJjdmNfY2hlY2siOm51bGx9LCJjb3VudHJ" +
                "5IjpudWxsLCJleHBfbW9udGgiOjEyLCJleHBfeWVhciI6MjAzNCwiZnVuZGluZyI6ImNyZWRpdCIsImdlbmVyYXRlZF9mcm9t" +
                "IjpudWxsLCJsYXN0NCI6IjAwMDAiLCJuZXR3b3JrcyI6eyJhdmFpbGFibGUiOlsidmlzYSJdLCJwcmVmZXJyZWQiOm51bGx9LC" +
                "J0aHJlZV9kX3NlY3VyZV91c2FnZSI6eyJzdXBwb3J0ZWQiOnRydWV9LCJ3YWxsZXQiOnsiZHluYW1pY19sYXN0NCI6bnVsbCw" +
                "ibGluayI6e30sInR5cGUiOiJsaW5rIn19LCJjcmVhdGVkIjoxNjg2OTI4MDIxLCJjdXN0b21lciI6bnVsbCwibGl2ZW1vZGU" +
                "iOmZhbHNlLCJ0eXBlIjoiY2FyZCJ9ICAg"
        val intent = Intent()
        intent.data = redirectUrl.toUri()

        val contract = contract()

        val result = contract.parseResult(LinkForegroundActivity.RESULT_COMPLETE, intent)

        assertThat(result).isInstanceOf(LinkActivityResult.PaymentMethodObtained::class.java)
        val paymentMethodObtained = result as LinkActivityResult.PaymentMethodObtained
        assertThat(paymentMethodObtained.paymentMethod.type?.code).isEqualTo("card")
        assertThat(paymentMethodObtained.paymentMethod.card?.last4).isEqualTo("0000")
        assertThat(paymentMethodObtained.paymentMethod.id).isEqualTo("pm_1NJeErLu5o3P18ZpmXpCtIrR")
    }

    @Test
    fun `parse logout result correctly`() {
        val redirectUrl = "link-popup://complete?link_status=logout"
        val intent = Intent()
        intent.data = redirectUrl.toUri()

        val contract = contract()

        val result = contract.parseResult(LinkForegroundActivity.RESULT_COMPLETE, intent)

        assertThat(result).isInstanceOf(LinkActivityResult.Canceled::class.java)
        val canceled = result as LinkActivityResult.Canceled
        assertThat(canceled.reason).isEqualTo(LinkActivityResult.Canceled.Reason.LoggedOut)
    }

    @Test
    fun `parse unknown status result correctly`() {
        val redirectUrl = "link-popup://complete?link_status=shrug"
        val intent = Intent()
        intent.data = redirectUrl.toUri()

        val contract = contract()

        val result = contract.parseResult(LinkForegroundActivity.RESULT_COMPLETE, intent)

        assertThat(result).isInstanceOf(LinkActivityResult.Canceled::class.java)
        val canceled = result as LinkActivityResult.Canceled
        assertThat(canceled.reason).isEqualTo(LinkActivityResult.Canceled.Reason.BackPressed)
    }

    @Test
    fun `canceled result code`() {
        val contract = contract()

        val result = contract.parseResult(Activity.RESULT_CANCELED, null)

        assertThat(result).isInstanceOf(LinkActivityResult.Canceled::class.java)
        val canceled = result as LinkActivityResult.Canceled
        assertThat(canceled.reason).isEqualTo(LinkActivityResult.Canceled.Reason.BackPressed)
    }

    @Test
    fun `failure with data`() {
        val intent = Intent()
        intent.putExtra(LinkForegroundActivity.EXTRA_FAILURE, IllegalStateException("Foobar!"))

        val contract = contract()

        val result = contract.parseResult(LinkForegroundActivity.RESULT_FAILURE, intent)

        assertThat(result).isInstanceOf(LinkActivityResult.Failed::class.java)
        val failed = result as LinkActivityResult.Failed
        assertThat(failed.error).hasMessageThat().isEqualTo("Foobar!")
    }

    @Test
    fun `failure without data results in canceled`() {
        val contract = contract()

        val result = contract.parseResult(LinkForegroundActivity.RESULT_FAILURE, null)

        assertThat(result).isInstanceOf(LinkActivityResult.Canceled::class.java)
        val canceled = result as LinkActivityResult.Canceled
        assertThat(canceled.reason).isEqualTo(LinkActivityResult.Canceled.Reason.BackPressed)
    }

    @Test
    fun `unknown result code results in canceled`() {
        val contract = contract()

        val result = contract.parseResult(42, null)

        assertThat(result).isInstanceOf(LinkActivityResult.Canceled::class.java)
        val canceled = result as LinkActivityResult.Canceled
        assertThat(canceled.reason).isEqualTo(LinkActivityResult.Canceled.Reason.BackPressed)
    }

    @Test
    fun `complete with malformed payment method results in canceled`() {
        val redirectUrl =
            "link-popup://complete?link_status=complete&pm=🤷‍"
        val intent = Intent()
        intent.data = redirectUrl.toUri()

        val errorReporter = FakeErrorReporter()

        val contract = contract(errorReporter = errorReporter)

        val result = contract.parseResult(LinkForegroundActivity.RESULT_COMPLETE, intent)

        assertThat(result).isInstanceOf(LinkActivityResult.Canceled::class.java)
        val canceled = result as LinkActivityResult.Canceled
        assertThat(canceled.reason).isEqualTo(LinkActivityResult.Canceled.Reason.BackPressed)
        assertThat(errorReporter.getLoggedErrors())
            .containsExactly(ErrorReporter.UnexpectedErrorEvent.LINK_WEB_FAILED_TO_PARSE_RESULT_URI.eventName)
    }

    private fun contract(
        stripeRepository: StripeRepository = FakeStripeRepository(),
        errorReporter: FakeErrorReporter = FakeErrorReporter()
    ): WebLinkActivityContract {
        return WebLinkActivityContract(stripeRepository, errorReporter)
    }
}
