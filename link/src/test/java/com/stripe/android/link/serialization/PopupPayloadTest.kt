package com.stripe.android.link.serialization

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Suppress("MaxLineLength")
internal class PopupPayloadTest {
    @Test
    fun testJsonSerialization() {
        val json = PopupPayload.PopupPayloadJson.encodeToString(
            serializer = PopupPayload.serializer(),
            value = createPayload(),
        )
        assertThat(json).isEqualTo("""{"publishableKey":"pk_test_abc","stripeAccount":"123","merchantInfo":{"businessName":"Jay's Taco Stand","country":"US"},"customerInfo":{"email":"jaystacostandfake@gmail.com","country":"US"},"paymentInfo":{"currency":"USD","amount":5555},"appId":"example.stripe.unittest","locale":"US","paymentUserAgent":"test","path":"mobile_pay","integrationType":"mobile","paymentObject":"link_payment_method","loggerMetadata":{},"flags":{},"experiments":{}}""")
    }

    @Test
    fun testToUrl() {
        assertThat(createPayload().toUrl()).isEqualTo("https://checkout.link.com/#eyJwdWJsaXNoYWJsZUtleSI6InBrX3Rlc3RfYWJjIiwic3RyaXBlQWNjb3VudCI6IjEyMyIsIm1lcmNoYW50SW5mbyI6eyJidXNpbmVzc05hbWUiOiJKYXkncyBUYWNvIFN0YW5kIiwiY291bnRyeSI6IlVTIn0sImN1c3RvbWVySW5mbyI6eyJlbWFpbCI6ImpheXN0YWNvc3RhbmRmYWtlQGdtYWlsLmNvbSIsImNvdW50cnkiOiJVUyJ9LCJwYXltZW50SW5mbyI6eyJjdXJyZW5jeSI6IlVTRCIsImFtb3VudCI6NTU1NX0sImFwcElkIjoiZXhhbXBsZS5zdHJpcGUudW5pdHRlc3QiLCJsb2NhbGUiOiJVUyIsInBheW1lbnRVc2VyQWdlbnQiOiJ0ZXN0IiwicGF0aCI6Im1vYmlsZV9wYXkiLCJpbnRlZ3JhdGlvblR5cGUiOiJtb2JpbGUiLCJwYXltZW50T2JqZWN0IjoibGlua19wYXltZW50X21ldGhvZCIsImxvZ2dlck1ldGFkYXRhIjp7fSwiZmxhZ3MiOnt9LCJleHBlcmltZW50cyI6e319")
    }

    private fun createPayload(): PopupPayload = PopupPayload(
        publishableKey = "pk_test_abc",
        stripeAccount = "123",
        merchantInfo = PopupPayload.MerchantInfo(
            businessName = "Jay's Taco Stand",
            country = "US",
        ),
        customerInfo = PopupPayload.CustomerInfo(
            email = "jaystacostandfake@gmail.com",
            country = "US",
        ),
        paymentInfo = PopupPayload.PaymentInfo(
            currency = "USD",
            amount = 5555,
        ),
        appId = "example.stripe.unittest",
        locale = "US",
        paymentUserAgent = "test",
    )
}
