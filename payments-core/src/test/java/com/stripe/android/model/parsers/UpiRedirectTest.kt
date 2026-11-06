package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentIntentResult
import com.stripe.android.StripeIntentResult
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class UpiRedirectTest {
    @Test
    fun `parses the mobile auth URL without changing its encoding`() {
        val url = "upi://pay?pa=merchant%40upi&pn=Merchant%20Name&am=100.00&cu=INR&tr=123"
        val action = parse(JSONObject().put("mobile_auth_url", url))

        assertThat(action.mobileAuthUrl).isEqualTo(url)
    }

    @Test
    fun `missing mobile auth URL does not use hosted instructions or QR code`() {
        val action = parse(
            JSONObject()
                .put("hosted_instructions_url", "https://payments.stripe.com/upi/instructions/test")
                .put("qr_code", JSONObject().put("data", "upi://pay?pa=merchant@upi"))
        )

        assertThat(action.mobileAuthUrl).isNull()
    }

    @Test
    fun `empty mobile auth URL is absent`() {
        assertThat(parse(JSONObject().put("mobile_auth_url", "")).mobileAuthUrl).isNull()
    }

    @Test
    fun `recognizes UPI payment method without a VPA`() {
        val paymentMethod = PaymentMethodJsonParser().parse(
            JSONObject().put("id", "pm_upi").put("type", "upi").put("upi", JSONObject())
        )

        assertThat(paymentMethod.type).isEqualTo(PaymentMethod.Type.Upi)
        assertThat(paymentMethod.code).isEqualTo("upi")
    }

    @Test
    fun `pending UPI redirect is not a successful payment`() {
        val intent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
            status = StripeIntent.Status.RequiresAction,
            nextActionData = parse(JSONObject().put("mobile_auth_url", "upi://pay?pa=merchant@upi")),
        )

        assertThat(intent.nextActionType).isEqualTo(StripeIntent.NextActionType.UpiRedirect)
        assertThat(PaymentIntentResult(intent, StripeIntentResult.Outcome.UNKNOWN).outcome)
            .isEqualTo(StripeIntentResult.Outcome.CANCELED)
    }

    private fun parse(details: JSONObject): StripeIntent.NextActionData.UpiRedirect {
        val json = JSONObject()
            .put("type", "upi_handle_redirect_or_display_qr_code")
            .put("upi_handle_redirect_or_display_qr_code", details)
        return NextActionDataParser().parse(json) as StripeIntent.NextActionData.UpiRedirect
    }
}
