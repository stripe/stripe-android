package com.stripe.android.repository

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ConsumerFixtures
import com.stripe.android.core.ApiVersion
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.header
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.verify
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Locale

class ConsumersApiServiceImplTest {
    @get:Rule
    val networkRule = NetworkRule()

    private val consumersApiService = ConsumersApiServiceImpl(
        stripeNetworkClient = DefaultStripeNetworkClient(),
        sdkVersion = StripeSdkVersion.VERSION,
        apiVersion = ApiVersion(betas = emptySet()).code,
        appInfo = null
    )

    @Test
    fun `lookupConsumerSession() sends all parameters`() = runTest {
        val email = "email@example.com"
        val cookie = "cookie1"

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/lookup"),
            header("Authorization", "Bearer ${DEFAULT_OPTIONS.apiKey}"),
            header("User-Agent", "Stripe/v1 ${StripeSdkVersion.VERSION}"),
            bodyPart("email_address", "email%40example.com"),
            bodyPart("cookies%5Bverification_session_client_secrets%5D%5B%5D", cookie),
            bodyPart("request_surface", "android_payment_element"),
        ) { response ->
            response.setBody(ConsumerFixtures.EXISTING_CONSUMER_JSON.toString())
        }

        val lookup = consumersApiService.lookupConsumerSession(
            email,
            cookie,
            DEFAULT_OPTIONS
        )

        assertThat(lookup.exists).isTrue()
        assertThat(lookup.errorMessage).isNull()
        assertThat(lookup.consumerSession?.authSessionClientSecret).isNull()
        assertThat(lookup.consumerSession?.publishableKey).isNull()
        assertThat(lookup.consumerSession?.verificationSessions).isEmpty()
        assertThat(lookup.consumerSession?.emailAddress).isEqualTo(email)
        assertThat(lookup.consumerSession?.redactedPhoneNumber).isEqualTo("+1********68")
        assertThat(lookup.consumerSession?.clientSecret).isEqualTo("secret")
    }

    @Test
    fun `lookupConsumerSession() returns error message with invalid json`() = runTest {
        val email = "email@example.com"
        val cookie = "cookie1"

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/lookup"),
        ) { response ->
            response.setResponseCode(400)
            response.setBody("""{"invalid-json"}""")
        }

        assertFailsWith<APIException> {
            consumersApiService.lookupConsumerSession(
                email,
                cookie,
                DEFAULT_OPTIONS
            )
        }
    }

    @Test
    fun `startConsumerVerification() sends all parameters`() = runTest {
        val stripeResponse = StripeResponse(
            200,
            ConsumerFixtures.CONSUMER_VERIFICATION_STARTED_JSON.toString(),
            emptyMap()
        )
        whenever(stripeNetworkClient.executeRequest(any<ApiRequest>()))
            .thenReturn(stripeResponse)

        val clientSecret = "secret"
        val locale = Locale.US
        val cookie = "cookie2"
        consumersApiService.startConsumerVerification(
            clientSecret,
            locale,
            cookie,
            DEFAULT_OPTIONS
        )

        org.mockito.kotlin.verify(stripeNetworkClient)
            .executeRequest(apiRequestArgumentCaptor.capture())
        val params = requireNotNull(apiRequestArgumentCaptor.firstValue.params)

        with(params) {
            assertEquals(this["request_surface"], "android_payment_element")
            assertEquals(this["type"], "SMS")
            assertEquals(this["locale"], locale.toLanguageTag())
            assertEquals(
                this["credentials"],
                mapOf("consumer_session_client_secret" to clientSecret)
            )
            assertEquals(
                this["cookies"],
                mapOf("verification_session_client_secrets" to listOf(cookie))
            )
        }
    }

    @Test
    fun testConsumerSessionLookupUrl() {
        ApiRequest.apiTestHost = null
        assertEquals(
            "https://api.stripe.com/v1/consumers/sessions/lookup",
            ConsumersApiServiceImpl.consumerSessionLookupUrl
        )
    }

    @Test
    fun testStartConsumerVerificationUrl() {
        kotlin.test.assertEquals(
            "https://api.stripe.com/v1/consumers/sessions/start_verification",
            ConsumersApiServiceImpl.startConsumerVerificationUrl
        )
    }

    private companion object {
        private val DEFAULT_OPTIONS = ApiRequest.Options("pk_test_vOo1umqsYxSrP5UXfOeL3ecm")
    }
}
