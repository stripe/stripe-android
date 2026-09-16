package com.stripe.android.identity.networked

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.ConnectionFactory
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.StripeConnection
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.identity.networking.IDENTITY_STRIPE_API_VERSION_WITH_BETA_HEADER
import com.stripe.android.identity.networking.VERIFICATION_PAGE_DATA_JSON_STRING
import com.stripe.android.identity.networking.models.Requirement
import com.stripe.android.identity.networking.models.VerificationPageData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URLDecoder
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestParameterInjector::class)
internal class DefaultNetworkedIdentityActionsTest {
    @Test
    fun `actions send only capability form data under Identity ephemeral authorization`(
        @TestParameter action: Action,
    ) = runScenario {
        val result = action.execute(actions).getOrThrow()

        val request = request(action)
        assertThat(request.body()).containsExactlyEntriesIn(
            action.parameter?.let { mapOf(it to TOKEN) }.orEmpty()
        )
        assertThat(request.headers["Authorization"]).isEqualTo("Bearer $EPHEMERAL_KEY")
        assertThat(request.headers["Stripe-Version"]).isEqualTo(IDENTITY_STRIPE_API_VERSION_WITH_BETA_HEADER)
        assertThat(request.headers).doesNotContainKey("Stripe-Account")
        assertThat(request.headers).doesNotContainKey("X-Stripe-Mock-Request")
        assertThat(request.headers).doesNotContainKey("X-Stripe-Identity-Client-Version")
        val userAgent = JSONObject(requireNotNull(request.headers["X-Stripe-User-Agent"]))
        assertThat(userAgent.getString("bindings_version")).isEqualTo(StripeSdkVersion.VERSION_NAME)
        assertThat(request.postHeaders?.get("Content-Type")).startsWith("application/x-www-form-urlencoded")
        assertThat(result.id).isEqualTo("vs_1KWvnMEAjaOkiuGMvfNAA0vo")
        assertThat(result.status).isEqualTo(VerificationPageData.Status.REQUIRESINPUT)
        assertThat(result.requirements.missings).containsExactly(
            Requirement.IDDOCUMENTFRONT, Requirement.IDDOCUMENTBACK
        )
        assertThat(result.submitted).isFalse()
        assertThat(result.closed).isFalse()
    }

    @Test
    fun `mutations never inherit automatic retries from the shared network client`(
        @TestParameter action: Action,
        @TestParameter(value = ["429", "500"]) status: Int,
    ) = runScenario(response = Result.success(StripeResponse(code = status, body = "{}"))) {
        val error = action.execute(actions).exceptionOrNull() as APIException

        request(action)
        assertThat(error.statusCode).isEqualTo(status)
    }

    @Test
    fun `ambiguous transport failure is not replayed and discards sensitive exception details`(
        @TestParameter action: Action,
    ) = runScenario(response = Result.failure(IOException("$TOKEN $SESSION_ID $EPHEMERAL_KEY"))) {
        val error = action.execute(actions).exceptionOrNull()

        request(action)
        assertThat(error).isInstanceOf(APIConnectionException::class.java)
        assertThat(error?.cause).isNull()
        assertThat(error?.stackTraceToString()).doesNotContain(TOKEN)
        assertThat(error?.stackTraceToString()).doesNotContain(SESSION_ID)
        assertThat(error?.stackTraceToString()).doesNotContain(EPHEMERAL_KEY)
    }

    @Test
    fun `unavailable code is preserved while backend echoes of credentials are redacted`() = runScenario(
        response = Result.success(
            StripeResponse(
                code = 400,
                body = """{"error":{"type":"invalid_request_error","code":"networked_identity_unavailable",
                    "message":"$TOKEN $EPHEMERAL_KEY","param":"$TOKEN"}}""",
            )
        ),
    ) {
        val error = actions.attachDocument(TOKEN).exceptionOrNull() as APIException

        request(Action.Attach)
        assertThat(error.stripeError?.code).isEqualTo("networked_identity_unavailable")
        assertThat(error.stripeError?.message).isNull()
        assertThat(error.stripeError?.param).isNull()
        assertThat(error.stackTraceToString()).doesNotContain(TOKEN)
        assertThat(error.stackTraceToString()).doesNotContain(EPHEMERAL_KEY)
    }

    @Test
    fun `malformed successful response does not expose raw response or a consumed token`() = runScenario(
        response = Result.success(StripeResponse(code = 200, body = "$TOKEN invalid json")),
    ) {
        val error = actions.prepareDocumentSave(TOKEN).exceptionOrNull()

        request(Action.PrepareSave)
        assertThat(error).isInstanceOf(APIException::class.java)
        assertThat(error?.cause).isNull()
        assertThat(error?.stackTraceToString()).doesNotContain(TOKEN)
    }

    @Test
    fun `cancellation propagates without conversion or replay`() = runScenario(
        response = Result.failure(CancellationException("cancelled")),
    ) {
        assertFailsWith<CancellationException> { actions.skip() }

        request(Action.Skip)
    }

    private fun runScenario(
        response: Result<StripeResponse<String>> = Result.success(
            StripeResponse(code = 200, body = VERIFICATION_PAGE_DATA_JSON_STRING)
        ),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val connections = FakeActionConnectionFactory(response)
        val actions = DefaultNetworkedIdentityActions(
            verificationSessionId = SESSION_ID,
            ephemeralKey = EPHEMERAL_KEY,
            stripeNetworkClient = DefaultStripeNetworkClient(
                workContext = StandardTestDispatcher(testScheduler),
                connectionFactory = connections,
                // Deliberately retain the shared client's default retry count to exercise the request policy.
                logger = Logger.noop(),
            ),
        )
        Scenario(actions, connections).block()
        connections.requests.ensureAllEventsConsumed()
    }

    private class Scenario(
        val actions: DefaultNetworkedIdentityActions,
        val connections: FakeActionConnectionFactory,
    ) {
        suspend fun request(action: Action): StripeRequest = connections.requests.awaitItem().also {
            assertThat(it.url).isEqualTo(
                "https://api.stripe.com/v1/identity/verification_pages/vs%2Ftarget%3Fvalue%23fragment" +
                    "/networked_identity/${action.path}"
            )
            assertThat(it.method).isEqualTo(StripeRequest.Method.POST)
            assertThat(it.mimeType).isEqualTo(StripeRequest.MimeType.Form)
            assertThat(it.retryResponseCodes).isEmpty()
            assertThat(it.shouldCache).isFalse()
            assertThat(it.toString()).doesNotContain(SESSION_ID)
            assertThat(it.toString()).doesNotContain("vs%2Ftarget%3Fvalue%23fragment")
            assertThat(it.toString()).doesNotContain(TOKEN)
        }
    }

    internal enum class Action(val path: String, val parameter: String?) {
        Attach("attach_document", "identity_document_association_token"),
        PrepareSave("prepare_document_save", "identity_document_save_association_token"),
        Skip("skip", null);

        suspend fun execute(actions: NetworkedIdentityActions): Result<VerificationPageData> = when (this) {
            Attach -> actions.attachDocument(TOKEN)
            PrepareSave -> actions.prepareDocumentSave(TOKEN)
            Skip -> actions.skip()
        }
    }

    private companion object {
        const val SESSION_ID = "vs/target?value#fragment"
        const val EPHEMERAL_KEY = "ek_test_identity"
        const val TOKEN = "association+token&value"

        fun StripeRequest.body(): Map<String, String> = ByteArrayOutputStream().also(::writePostBody)
            .toString("UTF-8").split("&").filter { it.isNotEmpty() }.associate { part ->
                val (key, value) = part.split("=", limit = 2)
                URLDecoder.decode(key, "UTF-8") to URLDecoder.decode(value, "UTF-8")
            }
    }
}

internal class FakeActionConnectionFactory(
    private val result: Result<StripeResponse<String>>,
) : ConnectionFactory {
    val requests = Turbine<StripeRequest>()

    override fun create(request: StripeRequest): StripeConnection<String> {
        requests.add(request)
        return object : StripeConnection<String> {
            override val responseCode: Int get() = result.getOrThrow().code
            override val response: StripeResponse<String> get() = result.getOrThrow()
            override fun createBodyFromResponseStream(responseStream: InputStream?): String = error("Unused")
            override fun close() = Unit
        }
    }

    override fun createForFile(request: StripeRequest, outputFile: File): StripeConnection<File> = error("Unused")
}
