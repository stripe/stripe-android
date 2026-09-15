package com.stripe.android.identity.networked

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiVersion
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.core.version.StripeSdkVersion
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
internal class DefaultNetworkedIdentityRepositoryTest {
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

        assertThat(request("consumers/identity_documents/list").body()).containsExactlyElementsIn(
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

        val result = repository.createAssociationToken(credentials, "idoc/test?value#fragment", "vs_target")
            .getOrThrow()

        val request = request("consumers/identity_documents/idoc%2Ftest%3Fvalue%23fragment/association_token")
        assertThat(request.body()).containsExactlyElementsIn(consumerBody() + ("verification_session" to "vs_target"))
        assertThat(result.associationToken).isEqualTo("association_secret")
        assertThat(request.toString()).doesNotContain("idoc")
        assertThat(result.toString()).doesNotContain("association_secret")
    }

    @Test
    fun `save association token uses consumer auth and identifies target verification session`() = runScenario {
        network.respond(JSONObject().put("association_token", "save_secret"))

        val result = repository.createSaveAssociationToken(credentials, "vs_target").getOrThrow()

        val request = request("consumers/identity_documents/save_association_token")
        assertThat(request.body()).containsExactlyElementsIn(consumerBody() + ("verification_session" to "vs_target"))
        assertThat(result.associationToken).isEqualTo("save_secret")
        assertThat(result.toString()).doesNotContain("save_secret")
    }

    @Test
    fun `save association token does not retry a server failure`() = runScenario {
        network.respond(JSONObject(), code = 500)

        val error = repository.createSaveAssociationToken(credentials, "vs_target").exceptionOrNull() as APIException

        request("consumers/identity_documents/save_association_token")
        assertThat(error.statusCode).isEqualTo(500)
    }

    @Test
    fun `save association token does not retry rate limiting`() = runScenario {
        network.respond(JSONObject(), code = 429)

        val error = repository.createSaveAssociationToken(credentials, "vs_target").exceptionOrNull() as APIException

        request("consumers/identity_documents/save_association_token")
        assertThat(error.statusCode).isEqualTo(429)
    }

    @Test
    fun `save association token does not retry ambiguous transport failure`() = runScenario {
        network.responses.add(Result.failure(IOException("save_secret vs_target css_request")))

        val error = repository.createSaveAssociationToken(credentials, "vs_target").exceptionOrNull()

        request("consumers/identity_documents/save_association_token")
        assertThat(error).isInstanceOf(APIConnectionException::class.java)
        assertThat(error?.cause).isNull()
        assertThat(error?.stackTraceToString()).doesNotContain("save_secret")
        assertThat(error?.stackTraceToString()).doesNotContain("vs_target")
        assertThat(error?.stackTraceToString()).doesNotContain("css_request")
    }

    @Test
    fun `document listing retries server errors once after 250 milliseconds`() = runScenario {
        network.respond(JSONObject(), code = 500)
        network.respond(JSONObject().put("data", JSONArray()))

        val result = testScope.async { repository.listDocuments(credentials) }
        testScope.runCurrent()
        request("consumers/identity_documents/list")
        testScope.advanceTimeBy(249)
        testScope.runCurrent()
        network.requests.expectNoEvents()
        assertThat(result.isCompleted).isFalse()
        testScope.advanceTimeBy(1)
        testScope.runCurrent()

        assertThat(result.await().getOrThrow()).isEmpty()
        request("consumers/identity_documents/list")
    }

    @Test
    fun `document listing makes at most two attempts for persistent server errors`() = runScenario {
        network.respond(JSONObject(), code = 599)
        network.respond(JSONObject(), code = 599)

        val result = repository.listDocuments(credentials)

        assertThat((result.exceptionOrNull() as APIException).statusCode).isEqualTo(599)
        request("consumers/identity_documents/list")
        request("consumers/identity_documents/list")
    }

    @Test
    fun `document listing does not retry rate limiting`() = runScenario {
        network.respond(JSONObject(), code = 429)

        val result = repository.listDocuments(credentials)

        assertThat((result.exceptionOrNull() as APIException).statusCode).isEqualTo(429)
        request("consumers/identity_documents/list")
    }

    @Test
    fun `document transport errors are not retried and discard identifying exception details`() = runScenario {
        network.responses.add(Result.failure(IOException("failed /idoc_sensitive/association_token css_secret")))

        val error = repository.listDocuments(credentials).exceptionOrNull()

        request("consumers/identity_documents/list")
        assertThat(error).isInstanceOf(APIConnectionException::class.java)
        assertThat(error?.cause).isNull()
        assertThat(error?.stackTraceToString()).doesNotContain("idoc_sensitive")
        assertThat(error?.stackTraceToString()).doesNotContain("css_secret")
    }

    @Test
    fun `malformed responses do not expose the body in errors`() = runScenario {
        network.responses.add(Result.success(StripeResponse(code = 200, body = "css_sensitive invalid json")))

        val error = repository.createAssociationToken(credentials, "idoc_sensitive", "vs_target").exceptionOrNull()

        request("consumers/identity_documents/idoc_sensitive/association_token")
        assertThat(error).isInstanceOf(APIException::class.java)
        assertThat(error?.cause).isNull()
        assertThat(error?.stackTraceToString()).doesNotContain("css_sensitive")
        assertThat(error?.stackTraceToString()).doesNotContain("idoc_sensitive")
    }

    @Test
    fun `cancellation is propagated without retry or conversion to a request failure`() = runScenario {
        network.responses.add(Result.failure(CancellationException("cancelled")))

        assertFailsWith<CancellationException> { repository.listDocuments(credentials) }

        request("consumers/identity_documents/list")
    }

    @Test
    fun `errors retain machine code and status without echoing backend details`() = runScenario {
        network.respond(
            JSONObject().put(
                "error",
                JSONObject().put("type", "invalid_request_error")
                    .put("code", "resource_missing")
                    .put("message", "No such document idoc_sensitive for person@example.com")
                    .put("param", "idoc_sensitive")
            ),
            code = 404,
        )

        val error = repository.createAssociationToken(credentials, "idoc_sensitive", "vs_target")
            .exceptionOrNull() as APIException

        request("consumers/identity_documents/idoc_sensitive/association_token")
        assertThat(error.stripeError?.code).isEqualTo("resource_missing")
        assertThat(error.statusCode).isEqualTo(404)
        assertThat(error.stripeError?.message).isNull()
        assertThat(error.stripeError?.param).isNull()
        assertThat(error.stackTraceToString()).doesNotContain("idoc_sensitive")
        assertThat(error.stackTraceToString()).doesNotContain("person@example.com")
    }

    @Test
    fun `wire models do not print identity or credential data`() = runScenario {
        network.respond(
            JSONObject().put(
                "data",
                JSONArray().put(documentJson("idoc_sensitive", "passport").put("redacted_document_number", "****9876"))
            )
        )
        val documents = repository.listDocuments(credentials).getOrThrow()
        request("consumers/identity_documents/list")

        val rendered = listOf(documents, credentials).toString()

        assertThat(rendered).doesNotContain("idoc_sensitive")
        assertThat(rendered).doesNotContain("9876")
        assertThat(rendered).doesNotContain("css_request")
        assertThat(rendered).doesNotContain("pk_test_consumer")
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val network = FakeStripeNetworkClient()
        val repository = DefaultNetworkedIdentityRepository(stripeNetworkClient = network)
        Scenario(repository, network, this).block()
        network.requests.ensureAllEventsConsumed()
    }

    private class Scenario(
        val repository: DefaultNetworkedIdentityRepository,
        val network: FakeStripeNetworkClient,
        val testScope: TestScope,
    ) {
        suspend fun request(path: String): StripeRequest {
            return network.requests.awaitItem().also {
                assertThat(it.url).isEqualTo("${ApiRequest.API_HOST}/v1/$path")
                assertThat(it.method).isEqualTo(StripeRequest.Method.POST)
                assertThat(it.mimeType).isEqualTo(StripeRequest.MimeType.Form)
                assertThat(it.headers["Authorization"]).isEqualTo("Bearer pk_test_consumer")
                assertThat(it.headers["Stripe-Account"]).isNull()
                assertThat(it.headers).doesNotContainKey("Cookie")
                assertThat(it.headers).doesNotContainKey("X-Stripe-Identity-Client-Version")
                assertThat(it.headers["Stripe-Version"]).isEqualTo(ApiVersion.get().code)
                val userAgent = JSONObject(requireNotNull(it.headers["X-Stripe-User-Agent"]))
                assertThat(userAgent.getString("bindings_version")).isEqualTo(StripeSdkVersion.VERSION_NAME)
                assertThat(userAgent.getString("lang")).isEqualTo("kotlin")
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

        fun documentJson(id: String, type: String) = JSONObject()
            .put("id", id).put("document_type", type).put("created", 1_700_000_000L)
    }
}
