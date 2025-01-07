package com.stripe.android.link

import android.app.Activity
import android.content.Intent
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodFixtures
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkActivityResultTest {

    @Test
    fun `complete with payment method`() {
        val redirectUrl =
            "link-popup://complete?link_status=complete&pm=eyJpZCI6InBtXzFOSmVFckx1NW8zUDE4WnBtWHBDdElyUiIsIm9iamVjdCI6InBheW1lbnRfbWV0aG9kIiwiYmlsbGluZ19kZXRhaWxzIjp7ImFkZHJlc3MiOnsiY2l0eSI6bnVsbCwiY291bnRyeSI6bnVsbCwibGluZTEiOm51bGwsImxpbmUyIjpudWxsLCJwb3N0YWxfY29kZSI6bnVsbCwic3RhdGUiOm51bGx9LCJlbWFpbCI6bnVsbCwibmFtZSI6bnVsbCwicGhvbmUiOm51bGx9LCJjYXJkIjp7ImJyYW5kIjoidmlzYSIsImNoZWNrcyI6eyJhZGRyZXNzX2xpbmUxX2NoZWNrIjpudWxsLCJhZGRyZXNzX3Bvc3RhbF9jb2RlX2NoZWNrIjpudWxsLCJjdmNfY2hlY2siOm51bGx9LCJjb3VudHJ5IjpudWxsLCJleHBfbW9udGgiOjEyLCJleHBfeWVhciI6MjAzNCwiZnVuZGluZyI6ImNyZWRpdCIsImdlbmVyYXRlZF9mcm9tIjpudWxsLCJsYXN0NCI6IjAwMDAiLCJuZXR3b3JrcyI6eyJhdmFpbGFibGUiOlsidmlzYSJdLCJwcmVmZXJyZWQiOm51bGx9LCJ0aHJlZV9kX3NlY3VyZV91c2FnZSI6eyJzdXBwb3J0ZWQiOnRydWV9LCJ3YWxsZXQiOnsiZHluYW1pY19sYXN0NCI6bnVsbCwibGluayI6e30sInR5cGUiOiJsaW5rIn19LCJjcmVhdGVkIjoxNjg2OTI4MDIxLCJjdXN0b21lciI6bnVsbCwibGl2ZW1vZGUiOmZhbHNlLCJ0eXBlIjoiY2FyZCJ9ICAg"
        val intent = Intent()
        intent.data = redirectUrl.toUri()
        val result = createLinkActivityResult(LinkForegroundActivity.RESULT_COMPLETE, intent)
        assertThat(result).isInstanceOf(LinkActivityResult.PaymentMethodObtained::class.java)
        val paymentMethodObtained = result as LinkActivityResult.PaymentMethodObtained
        assertThat(paymentMethodObtained.paymentMethod.type?.code).isEqualTo("card")
        assertThat(paymentMethodObtained.paymentMethod.card?.last4).isEqualTo("0000")
        assertThat(paymentMethodObtained.paymentMethod.id).isEqualTo("pm_1NJeErLu5o3P18ZpmXpCtIrR")
    }

    @Test
    fun `complete with logout`() {
        val redirectUrl = "link-popup://complete?link_status=logout"
        val intent = Intent()
        intent.data = redirectUrl.toUri()
        val result = createLinkActivityResult(LinkForegroundActivity.RESULT_COMPLETE, intent)
        assertThat(result).isInstanceOf(LinkActivityResult.Canceled::class.java)
        val canceled = result as LinkActivityResult.Canceled
        assertThat(canceled.reason).isEqualTo(LinkActivityResult.Canceled.Reason.LoggedOut)
    }

    @Test
    fun `complete with unknown link_status results in canceled`() {
        val redirectUrl = "link-popup://complete?link_status=shrug"
        val intent = Intent()
        intent.data = redirectUrl.toUri()
        val result = createLinkActivityResult(LinkForegroundActivity.RESULT_COMPLETE, intent)
        assertThat(result).isInstanceOf(LinkActivityResult.Canceled::class.java)
        val canceled = result as LinkActivityResult.Canceled
        assertThat(canceled.reason).isEqualTo(LinkActivityResult.Canceled.Reason.BackPressed)
    }

    @Test
    fun `canceled result code`() {
        val result = createLinkActivityResult(Activity.RESULT_CANCELED, null)
        assertThat(result).isInstanceOf(LinkActivityResult.Canceled::class.java)
        val canceled = result as LinkActivityResult.Canceled
        assertThat(canceled.reason).isEqualTo(LinkActivityResult.Canceled.Reason.BackPressed)
    }

    @Test
    fun `failure with data`() {
        val intent = Intent()
        intent.putExtra(LinkForegroundActivity.EXTRA_FAILURE, IllegalStateException("Foobar!"))
        val result = createLinkActivityResult(LinkForegroundActivity.RESULT_FAILURE, intent)
        assertThat(result).isInstanceOf(LinkActivityResult.Failed::class.java)
        val failed = result as LinkActivityResult.Failed
        assertThat(failed.error).hasMessageThat().isEqualTo("Foobar!")
    }

    @Test
    fun `failure without data results in canceled`() {
        val result = createLinkActivityResult(LinkForegroundActivity.RESULT_FAILURE, null)
        assertThat(result).isInstanceOf(LinkActivityResult.Canceled::class.java)
        val canceled = result as LinkActivityResult.Canceled
        assertThat(canceled.reason).isEqualTo(LinkActivityResult.Canceled.Reason.BackPressed)
    }

    @Test
    fun `unknown result code results in canceled`() {
        val result = createLinkActivityResult(42, null)
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
        val result = createLinkActivityResult(LinkForegroundActivity.RESULT_COMPLETE, intent)
        assertThat(result).isInstanceOf(LinkActivityResult.Canceled::class.java)
        val canceled = result as LinkActivityResult.Canceled
        assertThat(canceled.reason).isEqualTo(LinkActivityResult.Canceled.Reason.BackPressed)
    }

    @Test
    fun `complete with result from native link`() {
        val bundle = bundleOf(
            LinkActivityContract.EXTRA_RESULT to LinkActivityResult.PaymentMethodObtained(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
            )
        )
        val intent = Intent()
        intent.putExtras(bundle)
        val result = createLinkActivityResult(LinkActivity.RESULT_COMPLETE, intent)
        assertThat(result)
            .isEqualTo(LinkActivityResult.PaymentMethodObtained(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
    }

    @Test
    fun `complete with canceled result when native link result not found`() {
        val intent = Intent()
        val result = createLinkActivityResult(LinkActivity.RESULT_COMPLETE, intent)
        assertThat(result).isEqualTo(LinkActivityResult.Canceled())
    }
}
