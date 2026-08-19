package com.stripe.android.crypto.onramp

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.StripeFile
import com.stripe.android.core.model.StripeFileParams
import com.stripe.android.core.model.StripeFilePurpose
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository.Companion.CRYPTO_ONRAMP_API_VERSION
import com.stripe.android.link.LinkController
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

class CryptoApiRepositoryFileUploadTest {
    private val uploadError = IllegalStateException("Upload failed")

    @Test
    fun `additional KYC document is uploaded with the expected purpose`() = runScenario(
        uploadResult = Result.success(
            StripeFile(
                id = "file_123",
                purpose = StripeFilePurpose.CryptoOnrampKycDocument,
            )
        )
    ) {
        val result = repository.uploadAdditionalKycDocument(file)

        assertThat(result.getOrThrow().id).isEqualTo("file_123")
        verify(stripeRepository).createFile(
            fileParams = eq(
                StripeFileParams(
                    file = file,
                    purpose = StripeFilePurpose.CryptoOnrampKycDocument,
                )
            ),
            requestOptions = eq(
                ApiRequest.Options(
                    apiKey = "pk_test_123",
                    stripeAccount = "acct_123",
                )
            ),
        )
    }

    @Test
    fun `file upload failure is propagated`() = runScenario(
        uploadResult = Result.failure(uploadError),
    ) {
        val result = repository.uploadAdditionalKycDocument(file)

        assertThat(result.exceptionOrNull()).isSameInstanceAs(uploadError)
    }

    private fun runScenario(
        uploadResult: Result<StripeFile>,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val file = File("document.pdf")
        val stripeRepository = mock<StripeRepository>()
        whenever(stripeRepository.createFile(any(), any()))
            .thenReturn(uploadResult)

        Scenario(
            repository = CryptoApiRepository(
                stripeNetworkClient = mock<StripeNetworkClient>(),
                stripeRepository = stripeRepository,
                linkController = mock<LinkController>(),
                publishableKeyProvider = { "pk_test_123" },
                stripeAccountIdProvider = { "acct_123" },
                apiVersion = CRYPTO_ONRAMP_API_VERSION,
                sdkVersion = StripeSdkVersion.VERSION,
                appInfo = null,
            ),
            stripeRepository = stripeRepository,
            file = file,
        ).block()
    }

    private data class Scenario(
        val repository: CryptoApiRepository,
        val stripeRepository: StripeRepository,
        val file: File,
    )
}
