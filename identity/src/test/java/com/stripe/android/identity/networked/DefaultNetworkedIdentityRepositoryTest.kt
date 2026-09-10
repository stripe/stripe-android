package com.stripe.android.identity.networked

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.URLDecoder
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Suppress("LargeClass")
internal class DefaultNetworkedIdentityRepositoryTest {
    @Test
    fun `lookup sends merchant authentication and decodes account metadata and verification IDs`() = runScenario {
        network.respond(
            sessionResponse().put("exists", true).put("publishable_key", "pk_test_consumer")
                .put("account_id", "acct_link")
                .put("email_otp_requires_additional_info", true)
                .put("email_otp_verify_phone_despite_sms_otp", false)
                .put(
                    "experiments",
                    JSONArray().put(
                        JSONObject().put("experiment_name", "networked_identity")
                            .put("variant", "treatment").put("response_id", "response_123")
                    )
                )
        )

        val result = repository.lookup("person+identity@example.com", listOf("auth_1", "auth_2")).getOrThrow()

        val request = request("consumers/sessions/lookup", consumer = false)
        assertThat(request.body()).containsExactly(
            "email_address" to "person+identity@example.com",
            "request_surface" to "web_identity_product",
            "cookies[verification_session_client_secrets][]" to "auth_1",
            "cookies[verification_session_client_secrets][]" to "auth_2",
        )
        val found = result as NetworkedIdentityLookup.Found
        assertThat(found.publishableKey).isEqualTo("pk_test_consumer")
        assertThat(found.accountId).isEqualTo("acct_link")
        assertThat(found.authSessionClientSecret).isEqualTo("auth_response")
        assertThat(found.emailOtpRequiresAdditionalInfo).isTrue()
        assertThat(found.emailOtpVerifyPhoneDespiteSmsOtp).isFalse()
        assertThat(found.experiments.single().responseId).isEqualTo("response_123")
        assertThat(found.session.clientSecret).isEqualTo("css_response")
        assertThat(found.session.emailAddress).isEqualTo("person@example.com")
        assertThat(found.session.redactedPhoneNumber).isEqualTo("******1234")
        assertThat(found.session.redactedFormattedPhoneNumber).isEqualTo("(***) ***-1234")
        assertThat(found.session.phoneNumberCountry).isEqualTo("US")
        assertThat(found.session.unredactedPhoneNumber).isNull()
        val verification = found.session.verificationSessions.single()
        assertThat(verification.id).isEqualTo("cvs_fresh")
        assertThat(verification.type).isEqualTo(NetworkedIdentityVerificationType.SMS)
        assertThat(verification.state).isEqualTo(NetworkedIdentityVerificationState.STARTED)
        assertThat(verification.verificationToken).isEqualTo("token_response")
    }

    @Test
    fun `not found lookup needs no session or consumer key and omits empty cookies`() = runScenario {
        network.respond(JSONObject().put("exists", false).put("error_message", "No account found"))

        val result = repository.lookup("missing@example.com", emptyList()).getOrThrow()

        assertThat((result as NetworkedIdentityLookup.NotFound).errorMessage).isEqualTo("No account found")
        assertThat(request("consumers/sessions/lookup", consumer = false).body()).containsExactly(
            "email_address" to "missing@example.com",
            "request_surface" to "web_identity_product",
        )
    }

    @Test
    fun `lookup cannot treat a missing discriminator as account not found`() = runScenario {
        network.respond(JSONObject())

        assertThat(repository.lookup("person@example.com", emptyList()).isFailure).isTrue()

        request("consumers/sessions/lookup", consumer = false)
    }

    @Test
    fun `lookup rejects a null publishable key instead of coercing it to a string`() = runScenario {
        network.respond(sessionResponse().put("exists", true).put("publishable_key", JSONObject.NULL))

        assertThat(repository.lookup("person@example.com", emptyList()).isFailure).isTrue()

        request("consumers/sessions/lookup", consumer = false)
    }

    @Test
    fun `lookup rejects a blank publishable key`() = runScenario {
        network.respond(sessionResponse().put("exists", true).put("publishable_key", " "))

        assertThat(repository.lookup("person@example.com", emptyList()).isFailure).isTrue()

        request("consumers/sessions/lookup", consumer = false)
    }

    @Test
    fun `signup encodes all optional values and Link identity signup consent`() = runScenario {
        network.respond(sessionResponse().put("publishable_key", "pk_test_consumer").put("account_id", "acct_link"))

        val result = repository.signUp(
            signUpRequest().copy(
                defaultOptInEnabled = true,
                changedPhoneNumber = false,
                checkedOptInBox = true,
                legalName = "Person Example",
                hcaptchaResponse = "captcha&response",
                hcaptchaKey = "captcha_key",
                sessionId = "session_123",
                authSessionSecrets = listOf("auth_1"),
            )
        ).getOrThrow()

        assertThat(request("consumers/accounts/sign_up", consumer = false).body()).containsExactly(
            "email_address" to "person@example.com",
            "phone_number" to "+15555551234",
            "country" to "US",
            "country_inferring_method" to "PHONE_NUMBER",
            "locale" to "en-US",
            "consent_action" to "entered_phone_number_email_clicked_save_with_link_identity",
            "default_opt_in_enabled" to "true",
            "changed_phone_number" to "false",
            "checked_opt_in_box" to "true",
            "legal_name" to "Person Example",
            "hcaptcha_response" to "captcha&response",
            "hcaptcha_key" to "captcha_key",
            "session_id" to "session_123",
            "cookies[verification_session_client_secrets][]" to "auth_1",
            "request_surface" to "web_identity_product",
        )
        assertThat(result.accountId).isEqualTo("acct_link")
        assertThat(result.publishableKey).isEqualTo("pk_test_consumer")
        assertThat(result.session.clientSecret).isEqualTo("css_response")
        assertThat(result.authSessionClientSecret).isEqualTo("auth_response")
    }

    @Test
    fun `signup omits absent optional fields`() = runScenario {
        network.respond(sessionResponse().put("publishable_key", "pk_test_consumer").put("account_id", "acct_link"))

        repository.signUp(signUpRequest()).getOrThrow()

        assertThat(request("consumers/accounts/sign_up", consumer = false).body()).containsExactly(
            "email_address" to "person@example.com",
            "phone_number" to "+15555551234",
            "country" to "US",
            "country_inferring_method" to "PHONE_NUMBER",
            "locale" to "en-US",
            "consent_action" to "entered_phone_number_email_clicked_save_with_link_identity",
            "request_surface" to "web_identity_product",
        )
    }

    @Test
    fun `signup requires the account ID`() = runScenario {
        network.respond(sessionResponse().put("publishable_key", "pk_test_consumer"))

        assertThat(repository.signUp(signUpRequest()).isFailure).isTrue()

        request("consumers/accounts/sign_up", consumer = false)
    }

    @Test
    fun `start uses consumer credentials SMS language tag and optional account phone`() = runScenario {
        network.respond(sessionResponse())

        val response = repository.startVerification(
            credentials = credentials,
            locale = "fr-CA",
            accountPhoneNumber = "+15555551234",
            authSessionSecrets = listOf("auth_1", "", "auth_1", "auth_2"),
        ).getOrThrow()

        assertThat(request("consumers/sessions/start_verification", consumer = true).body()).containsExactly(
            "credentials[consumer_session_client_secret]" to "css_request",
            "request_surface" to "web_identity_product",
            "type" to "SMS",
            "locale" to "fr-CA",
            "account_phone_number" to "+15555551234",
            "cookies[verification_session_client_secrets][]" to "auth_1",
            "cookies[verification_session_client_secrets][]" to "auth_2",
        )
        assertThat(response.session.clientSecret).isEqualTo("css_response")
        assertThat(response.authSessionClientSecret).isEqualTo("auth_response")
    }

    @Test
    fun `start omits absent phone cookies and resend flag`() = runScenario {
        network.respond(sessionResponse())

        repository.startVerification(credentials, "en-US", null, emptyList()).getOrThrow()

        assertThat(request("consumers/sessions/start_verification", consumer = true).body()).containsExactly(
            "credentials[consumer_session_client_secret]" to "css_request",
            "request_surface" to "web_identity_product",
            "type" to "SMS",
            "locale" to "en-US",
        )
    }

    @Test
    fun `confirm sends code as form data and retains returned session and auth secrets`() = runScenario {
        network.respond(sessionResponse())

        val response = repository.confirmVerification(credentials, "123456", listOf("auth_1")).getOrThrow()

        assertThat(request("consumers/sessions/confirm_verification", consumer = true).body()).containsExactly(
            "credentials[consumer_session_client_secret]" to "css_request",
            "request_surface" to "web_identity_product",
            "type" to "SMS",
            "code" to "123456",
            "cookies[verification_session_client_secrets][]" to "auth_1",
        )
        assertThat(response.session.clientSecret).isEqualTo("css_response")
        assertThat(response.authSessionClientSecret).isEqualTo("auth_response")
    }

    @Test
    fun `unknown verification type and state decode without granting known authentication`() = runScenario {
        val json = sessionResponse()
        json.getJSONObject("consumer_session").getJSONArray("verification_sessions").getJSONObject(0)
            .put("type", "FUTURE_TYPE").put("state", "FUTURE_STATE").remove("id")
        network.respond(json)

        val session = repository.confirmVerification(credentials, "123456", emptyList()).getOrThrow()
            .session.verificationSessions.single()

        request("consumers/sessions/confirm_verification", consumer = true)
        assertThat(session.type).isEqualTo(NetworkedIdentityVerificationType.UNKNOWN)
        assertThat(session.state).isEqualTo(NetworkedIdentityVerificationState.UNKNOWN)
        assertThat(session.id).isNull()
    }

    @Test
    fun `start rejects a null session secret instead of coercing it to a string`() = runScenario {
        val json = sessionResponse()
        json.getJSONObject("consumer_session").put("client_secret", JSONObject.NULL)
        network.respond(json)

        assertThat(repository.startVerification(credentials, "en-US", null, emptyList()).isFailure).isTrue()

        request("consumers/sessions/start_verification", consumer = true)
    }

    @Test
    fun `confirm rejects a blank rotated session secret`() = runScenario {
        val json = sessionResponse()
        json.getJSONObject("consumer_session").put("client_secret", " ")
        network.respond(json)

        assertThat(repository.confirmVerification(credentials, "123456", emptyList()).isFailure).isTrue()

        request("consumers/sessions/confirm_verification", consumer = true)
    }

    @Test
    fun `list documents preserves backend order and optional fields with timestamps in seconds`() = runScenario {
        network.respond(
            JSONObject().put(
                "data",
                JSONArray().put(
                    documentJson("idoc_passport", "passport").put("country", "US").put("region", "CA")
                        .put("redacted_document_number", "****1234").put("expiration_date", 2_000_000_000L)
                        .put("live_captured", true)
                ).put(documentJson("idoc_license", "driving_license"))
                    .put(documentJson("idoc_card", "id_card"))
                    .put(documentJson("idoc_future", "future_type"))
            )
        )

        val documents = repository.listDocuments(credentials).getOrThrow()

        assertThat(request("consumers/identity_documents/list", consumer = true).body()).containsExactlyElementsIn(
            consumerBody()
        )
        assertThat(documents.map { it.id }).containsExactly(
            "idoc_passport", "idoc_license", "idoc_card", "idoc_future"
        ).inOrder()
        assertThat(documents.map { it.documentType }).containsExactly(
            NetworkedIdentityDocumentType.PASSPORT,
            NetworkedIdentityDocumentType.DRIVING_LICENSE,
            NetworkedIdentityDocumentType.ID_CARD,
            NetworkedIdentityDocumentType.UNKNOWN,
        ).inOrder()
        assertThat(documents.first().created).isEqualTo(1_700_000_000L)
        assertThat(documents.first().expirationDate).isEqualTo(2_000_000_000L)
        assertThat(documents.first().country).isEqualTo("US")
        assertThat(documents.first().region).isEqualTo("CA")
        assertThat(documents.first().redactedDocumentNumber).isEqualTo("****1234")
        assertThat(documents.first().liveCaptured).isTrue()
        assertThat(documents[1].expirationDate).isNull()
        assertThat(documents[1].liveCaptured).isNull()
    }

    @Test
    fun `association token encodes the document ID as one path component`() = runScenario {
        network.respond(JSONObject().put("association_token", "association_secret"))

        val result = repository.createAssociationToken(credentials, "idoc/test?value#fragment").getOrThrow()

        val request = request("consumers/identity_documents/idoc%2Ftest%3Fvalue%23fragment/association_token", true)
        assertThat(request.body()).containsExactlyElementsIn(consumerBody())
        assertThat(result.associationToken).isEqualTo("association_secret")
        assertThat(request.toString()).doesNotContain("idoc")
        assertThat(result.toString()).doesNotContain("association_secret")
    }

    @Test
    fun `list rejects an empty document ID`() = runScenario {
        network.respond(JSONObject().put("data", JSONArray().put(documentJson("", "passport"))))

        assertThat(repository.listDocuments(credentials).isFailure).isTrue()

        request("consumers/identity_documents/list", consumer = true)
    }

    @Test
    fun `association token rejects an empty token`() = runScenario {
        network.respond(JSONObject().put("association_token", ""))

        assertThat(repository.createAssociationToken(credentials, "idoc_test").isFailure).isTrue()

        request("consumers/identity_documents/idoc_test/association_token", consumer = true)
    }

    @Test
    fun `logout includes auth cookies and returns rotated secrets`() = runScenario {
        network.respond(sessionResponse())

        val result = repository.logout(credentials, listOf("auth_1")).getOrThrow()

        assertThat(request("consumers/sessions/log_out", consumer = true).body()).containsExactlyElementsIn(
            consumerBody() + ("cookies[verification_session_client_secrets][]" to "auth_1")
        )
        assertThat(result.session.clientSecret).isEqualTo("css_response")
        assertThat(result.authSessionClientSecret).isEqualTo("auth_response")
    }

    @Test
    fun `extend decodes consumer session secret separately from auth session secret`() = runScenario {
        network.respond(
            JSONObject().put("consumer_session_client_secret", "css_extended")
                .put("auth_session_client_secret", "auth_other")
        )

        val result = repository.extendSession(credentials).getOrThrow()

        assertThat(request("consumers/sessions/extend", consumer = true).body())
            .containsExactlyElementsIn(consumerBody())
        assertThat(result.consumerSessionClientSecret).isEqualTo("css_extended")
    }

    @Test
    fun `extend permits absent consumer secret without substituting auth session secret`() = runScenario {
        network.respond(JSONObject().put("auth_session_client_secret", "auth_other"))

        val result = repository.extendSession(credentials).getOrThrow()

        request("consumers/sessions/extend", consumer = true)
        assertThat(result.consumerSessionClientSecret).isNull()
    }

    @Test
    fun `document listing retries server errors once after 250 milliseconds`() = runScenario {
        network.respond(JSONObject(), code = 500)
        network.respond(JSONObject().put("data", JSONArray()))

        val result = testScope.async { repository.listDocuments(credentials) }
        testScope.runCurrent()
        request("consumers/identity_documents/list", consumer = true)
        testScope.advanceTimeBy(249)
        testScope.runCurrent()
        network.requests.expectNoEvents()
        assertThat(result.isCompleted).isFalse()
        testScope.advanceTimeBy(1)
        testScope.runCurrent()

        assertThat(result.await().getOrThrow()).isEmpty()
        request("consumers/identity_documents/list", consumer = true)
    }

    @Test
    fun `document listing makes at most two attempts for persistent server errors`() = runScenario {
        network.respond(JSONObject(), code = 599)
        network.respond(JSONObject(), code = 599)

        val result = repository.listDocuments(credentials)

        assertThat((result.exceptionOrNull() as APIException).statusCode).isEqualTo(599)
        request("consumers/identity_documents/list", consumer = true)
        request("consumers/identity_documents/list", consumer = true)
    }

    @Test
    fun `document listing does not retry rate limiting`() = runScenario {
        network.respond(JSONObject(), code = 429)

        val result = repository.listDocuments(credentials)

        assertThat((result.exceptionOrNull() as APIException).statusCode).isEqualTo(429)
        request("consumers/identity_documents/list", consumer = true)
    }

    @Test
    fun `lookup has no server error retry`() = runScenario {
        network.respond(JSONObject(), code = 503)

        val result = repository.lookup("person@example.com", emptyList())

        assertThat((result.exceptionOrNull() as APIException).statusCode).isEqualTo(503)
        request("consumers/sessions/lookup", consumer = false)
    }

    @Test
    fun `invalid OTP error retains machine code and status without echoing submitted data`() = runScenario {
        network.respond(
            JSONObject().put(
                "error",
                JSONObject().put("type", "invalid_request_error")
                    .put("code", "consumer_verification_code_invalid")
                    .put("message", "Rejected 123456 for person@example.com")
                    .put("param", "123456")
            ),
            code = 400,
        )

        val error = repository.confirmVerification(credentials, "123456", emptyList()).exceptionOrNull() as APIException

        request("consumers/sessions/confirm_verification", consumer = true)
        assertThat(error.stripeError?.code).isEqualTo("consumer_verification_code_invalid")
        assertThat(error.statusCode).isEqualTo(400)
        assertThat(error.stripeError?.message).isNull()
        assertThat(error.stripeError?.param).isNull()
        assertThat(error.stackTraceToString()).doesNotContain("123456")
        assertThat(error.stackTraceToString()).doesNotContain("person@example.com")
    }

    @Test
    fun `document transport errors are not retried and discard identifying exception details`() = runScenario {
        network.responses.add(Result.failure(IOException("failed /idoc_sensitive/association_token css_secret")))

        val error = repository.listDocuments(credentials).exceptionOrNull()

        request("consumers/identity_documents/list", consumer = true)
        assertThat(error).isInstanceOf(APIConnectionException::class.java)
        assertThat(error?.cause).isNull()
        assertThat(error?.stackTraceToString()).doesNotContain("idoc_sensitive")
        assertThat(error?.stackTraceToString()).doesNotContain("css_secret")
    }

    @Test
    fun `malformed responses do not expose the body in errors`() = runScenario {
        network.responses.add(Result.success(StripeResponse(code = 200, body = "css_sensitive invalid json")))

        val error = repository.createAssociationToken(credentials, "idoc_sensitive").exceptionOrNull()

        request("consumers/identity_documents/idoc_sensitive/association_token", consumer = true)
        assertThat(error).isInstanceOf(APIException::class.java)
        assertThat(error?.cause).isNull()
        assertThat(error?.stackTraceToString()).doesNotContain("css_sensitive")
        assertThat(error?.stackTraceToString()).doesNotContain("idoc_sensitive")
    }

    @Test
    fun `cancellation is propagated without retry or conversion to a request failure`() = runScenario {
        network.responses.add(Result.failure(CancellationException("cancelled")))

        assertFailsWith<CancellationException> { repository.listDocuments(credentials) }

        request("consumers/identity_documents/list", consumer = true)
    }

    @Test
    fun `wire models do not print identity or credential data`() = runScenario {
        network.respond(sessionResponse().put("exists", true).put("publishable_key", "pk_test_consumer"))
        val found = repository.lookup("person@example.com", emptyList()).getOrThrow() as NetworkedIdentityLookup.Found
        request("consumers/sessions/lookup", consumer = false)

        val rendered = listOf(found, found.session, found.session.verificationSessions.single(), credentials).toString()

        assertThat(rendered).doesNotContain("css_response")
        assertThat(rendered).doesNotContain("auth_response")
        assertThat(rendered).doesNotContain("token_response")
        assertThat(rendered).doesNotContain("person@example.com")
        assertThat(rendered).doesNotContain("css_request")
        assertThat(rendered).doesNotContain("cvs_fresh")
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val network = FakeStripeNetworkClient()
        val repository = DefaultNetworkedIdentityRepository(
            merchantRequestOptions = ApiRequest.Options(
                apiKey = "pk_test_merchant",
                stripeAccount = "acct_merchant",
                idempotencyKey = null,
            ),
            stripeNetworkClient = network,
        )
        Scenario(repository, network, this).block()
        network.requests.ensureAllEventsConsumed()
    }

    private class Scenario(
        val repository: DefaultNetworkedIdentityRepository,
        val network: FakeStripeNetworkClient,
        val testScope: TestScope,
    ) {
        suspend fun request(path: String, consumer: Boolean): StripeRequest {
            return network.requests.awaitItem().also {
                assertThat(it.url).isEqualTo("${ApiRequest.API_HOST}/v1/$path")
                assertThat(it.method).isEqualTo(StripeRequest.Method.POST)
                assertThat(it.mimeType).isEqualTo(StripeRequest.MimeType.Form)
                assertThat(it.headers["Authorization"]).isEqualTo(
                    "Bearer ${if (consumer) "pk_test_consumer" else "pk_test_merchant"}"
                )
                assertThat(it.headers["Stripe-Account"]).isEqualTo(if (consumer) null else "acct_merchant")
                assertThat(it.headers).doesNotContainKey("Cookie")
                assertThat(it.headers).doesNotContainKey("X-Stripe-Identity-Client-Version")
                assertThat(it.postHeaders?.get("Content-Type")).startsWith("application/x-www-form-urlencoded")
                assertThat(it.retryResponseCodes).isEmpty()
                assertThat(it.shouldCache).isFalse()
            }
        }
    }

    private class FakeStripeNetworkClient : StripeNetworkClient {
        val requests = Turbine<StripeRequest>()
        val responses = ArrayDeque<Result<StripeResponse<String>>>()

        fun respond(json: JSONObject, code: Int = 200) {
            responses.add(Result.success(StripeResponse(code = code, body = json.toString())))
        }

        override suspend fun executeRequest(request: StripeRequest): StripeResponse<String> {
            requests.add(request)
            return responses.removeFirst().getOrThrow()
        }

        override suspend fun executeRequestForFile(request: StripeRequest, outputFile: File): StripeResponse<File> {
            error("NI does not download files")
        }
    }

    private companion object {
        val credentials = NetworkedIdentityCredentials("pk_test_consumer", "css_request")

        fun StripeRequest.body(): List<Pair<String, String>> = ByteArrayOutputStream().also(::writePostBody)
            .toString("UTF-8").split("&").map { part ->
                val (key, value) = part.split("=", limit = 2)
                URLDecoder.decode(key, "UTF-8") to URLDecoder.decode(value, "UTF-8")
            }

        fun consumerBody() = listOf(
            "credentials[consumer_session_client_secret]" to "css_request",
            "request_surface" to "web_identity_product",
        )

        fun sessionResponse() = JSONObject(
            """
            {
              "consumer_session": {
                "client_secret": "css_response",
                "email_address": "person@example.com",
                "redacted_phone_number": "******1234",
                "redacted_formatted_phone_number": "(***) ***-1234",
                "unredacted_phone_number": null,
                "phone_number_country": "US",
                "verification_sessions": [{
                  "id": "cvs_fresh", "type": "SMS", "state": "STARTED", "verification_token": "token_response"
                }]
              },
              "auth_session_client_secret": "auth_response"
            }
            """.trimIndent()
        )

        fun documentJson(id: String, type: String) = JSONObject()
            .put("id", id).put("document_type", type).put("created", 1_700_000_000L)

        fun signUpRequest() = NetworkedIdentitySignUpRequest(
            emailAddress = "person@example.com",
            phoneNumber = "+15555551234",
            country = "US",
            countryInferringMethod = NetworkedIdentityCountryInferringMethod.PHONE_NUMBER,
            locale = "en-US",
            defaultOptInEnabled = null,
            changedPhoneNumber = null,
            checkedOptInBox = null,
            legalName = null,
            hcaptchaResponse = null,
            hcaptchaKey = null,
            sessionId = null,
            authSessionSecrets = emptyList(),
        )
    }
}
