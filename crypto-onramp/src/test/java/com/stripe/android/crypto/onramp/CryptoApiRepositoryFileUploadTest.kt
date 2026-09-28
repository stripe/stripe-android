package com.stripe.android.crypto.onramp

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.model.StripeFilePurpose
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository.Companion.CRYPTO_ONRAMP_API_VERSION
import com.stripe.android.link.LinkController
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
class CryptoApiRepositoryFileUploadTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `additional KYC document uses Link authentication and preserves file bytes`() = runScenario {
        val file = temporaryFolder.newFile("document.pdf").apply { writeBytes(byteArrayOf(0, 1, -1, 42)) }
        val result = repository.uploadAdditionalKycDocument(file, "lsk_test_123")
        val request = network.requests.awaitItem()

        assertThat(result.getOrThrow().id).isEqualTo("file_123")
        assertThat(result.getOrThrow().purpose).isEqualTo(StripeFilePurpose.CryptoOnrampKycDocument)
        assertThat(request.url).isEqualTo("https://files.stripe.com/v1/files")
        assertThat(request.method).isEqualTo(StripeRequest.Method.POST)
        assertThat(request.headers["Authorization"]).isEqualTo("Bearer lsk_test_123")
        assertThat(request.headers["Stripe-Account"]).isEqualTo("acct_123")
        val body = ByteArrayOutputStream().also(request::writePostBody).toByteArray()
        val text = body.toString(Charsets.ISO_8859_1)
        assertThat(text).contains("Content-Type: application/pdf\r\n")
        assertThat(text).contains("name=\"purpose\"\r\n\r\ncrypto_onramp_kyc_document\r\n")
        assertThat(text).contains("filename=\"document.pdf\"")
        assertThat(
            text.substringAfter("Content-Transfer-Encoding: binary\r\n\r\n").take(4)
                .toByteArray(Charsets.ISO_8859_1)
        ).isEqualTo(file.readBytes())
        val boundary = requireNotNull(request.postHeaders?.get("Content-Type")).substringAfter("boundary=")
        assertThat(text).endsWith("\r\n--$boundary--")
    }

    @Test
    fun `Word document has an Office MIME type`() = assertFileContentType(
        "document.docx",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    )

    @Test
    fun `spreadsheet has an Office MIME type`() = assertFileContentType(
        "document.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    )

    @Test
    fun `unknown extension uploads as binary data`() =
        assertFileContentType("document.unknown", "application/octet-stream")

    @Test
    fun `uppercase extension is recognized`() = assertFileContentType("document.PDF", "application/pdf")

    @Test
    fun `file upload connection failure is returned`() = runScenario {
        val error = IllegalStateException("Upload failed")
        network.result = Result.failure(error)

        val result = repository.uploadAdditionalKycDocument(File("document.pdf"), "lsk_test_123")
        network.requests.awaitItem()

        assertThat(result.exceptionOrNull()).isInstanceOf(APIConnectionException::class.java)
        assertThat(result.exceptionOrNull()?.cause).isSameInstanceAs(error)
    }

    @Test
    fun `file upload API error is returned`() = runScenario {
        network.result = Result.success(
            StripeResponse(400, """{"error":{"message":"Invalid file","type":"invalid_request_error"}}""")
        )

        val result = repository.uploadAdditionalKycDocument(File("document.pdf"), "lsk_test_123")
        network.requests.awaitItem()

        assertThat(result.exceptionOrNull()).isInstanceOf(APIException::class.java)
        assertThat((result.exceptionOrNull() as APIException).statusCode).isEqualTo(400)
    }

    @Test
    fun `malformed upload response fails parsing`() = runScenario {
        network.result = Result.success(StripeResponse(200, "not JSON"))

        val result = repository.uploadAdditionalKycDocument(File("document.pdf"), "lsk_test_123")
        network.requests.awaitItem()

        assertThat(result.exceptionOrNull()).isInstanceOf(APIException::class.java)
    }

    private fun assertFileContentType(fileName: String, contentType: String) = runScenario {
        val file = temporaryFolder.newFile(fileName).apply { writeText("document contents") }
        assertThat(repository.uploadAdditionalKycDocument(file, "lsk_test_123").isSuccess).isTrue()
        val request = network.requests.awaitItem()
        val body = ByteArrayOutputStream().also(request::writePostBody).toString(Charsets.UTF_8.name())

        assertThat(body).contains("Content-Type: $contentType\r\n")
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val network = FakeUploadNetworkClient()
        Scenario(
            repository = CryptoApiRepository(
                stripeNetworkClient = network,
                stripeRepository = mock<StripeRepository>(),
                linkController = mock<LinkController>(),
                apiConfigProvider = { ApiConfiguration.State("pk_test_123", "acct_123") },
                apiVersion = CRYPTO_ONRAMP_API_VERSION,
                sdkVersion = StripeSdkVersion.VERSION,
                appInfo = null,
            ),
            network = network,
        ).block()
        network.requests.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val repository: CryptoApiRepository,
        val network: FakeUploadNetworkClient,
    )

    internal class FakeUploadNetworkClient : StripeNetworkClient {
        val requests = Turbine<StripeRequest>()
        var result = Result.success(
            StripeResponse(200, """{"id":"file_123","purpose":"crypto_onramp_kyc_document"}""")
        )

        override suspend fun executeRequest(request: StripeRequest): StripeResponse<String> {
            requests.add(request)
            return result.getOrThrow()
        }

        override suspend fun executeRequestForFile(request: StripeRequest, outputFile: File): StripeResponse<File> {
            error("Unexpected download")
        }
    }
}
