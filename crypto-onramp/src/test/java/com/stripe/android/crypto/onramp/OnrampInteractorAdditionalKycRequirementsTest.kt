package com.stripe.android.crypto.onramp

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.analytics.OnrampAnalyticsService
import com.stripe.android.crypto.onramp.exception.MissingConsumerSecretException
import com.stripe.android.crypto.onramp.exception.MissingCryptoCustomerException
import com.stripe.android.crypto.onramp.exception.OnrampErrorLogger
import com.stripe.android.crypto.onramp.model.AdditionalKycRequirementResponse
import com.stripe.android.crypto.onramp.model.AdditionalKycRequirementsResponse
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
import com.stripe.android.crypto.onramp.model.OnrampSessionClientSecretProvider
import com.stripe.android.crypto.onramp.model.RetrieveCryptoCustomerResponse
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import com.stripe.android.link.LinkAppearance
import com.stripe.android.link.LinkController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class OnrampInteractorAdditionalKycRequirementsTest {
    private val expectedRepositoryError = IllegalStateException("Could not retrieve customer")

    @Test
    fun `requirements are retrieved and converted`() = runScenario(
        repositoryResult = Result.success(
            customerResponse(
                entries = listOf(
                    requirement(description = "proof_of_address", awaitingActionFrom = "user"),
                    requirement(description = "proof_of_address", awaitingActionFrom = "partner"),
                )
            )
        )
    ) {
        val result = interactor.retrieveAdditionalKycRequirements().getOrThrow()

        assertThat(result.userActionRequired.single().description).isEqualTo("proof_of_address")
        assertThat(result.pendingPartnerAction.single().description).isEqualTo("proof_of_address")
        assertThat(result.pendingStripeAction).isEmpty()
        assertThat(result.unrecognizedActionOwner).isEmpty()
        verify(cryptoApiRepository).retrieveCryptoCustomer(
            cryptoCustomerId = CRYPTO_CUSTOMER_ID,
            consumerSessionClientSecret = CONSUMER_SESSION_CLIENT_SECRET,
        )
    }

    @Test
    fun `missing crypto customer returns failure without requesting customer`() = runScenario(
        cryptoCustomerId = null,
    ) {
        val error = interactor.retrieveAdditionalKycRequirements().exceptionOrNull()

        assertThat(error).isInstanceOf(MissingCryptoCustomerException::class.java)
        verify(cryptoApiRepository, never()).retrieveCryptoCustomer(any(), any())
    }

    @Test
    fun `missing consumer secret returns failure without requesting customer`() = runScenario(
        consumerSessionClientSecret = null,
    ) {
        val error = interactor.retrieveAdditionalKycRequirements().exceptionOrNull()

        assertThat(error).isInstanceOf(MissingConsumerSecretException::class.java)
        verify(cryptoApiRepository, never()).retrieveCryptoCustomer(any(), any())
    }

    @Test
    fun `repository failure is propagated`() = runScenario(
        repositoryResult = Result.failure(expectedRepositoryError),
    ) {
        val error = interactor.retrieveAdditionalKycRequirements().exceptionOrNull()

        assertThat(error).isSameInstanceAs(expectedRepositoryError)
    }

    private fun runScenario(
        cryptoCustomerId: String? = CRYPTO_CUSTOMER_ID,
        consumerSessionClientSecret: String? = CONSUMER_SESSION_CLIENT_SECRET,
        repositoryResult: Result<RetrieveCryptoCustomerResponse> = Result.success(customerResponse()),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val application: Application = RuntimeEnvironment.getApplication()
        val linkController = mock<LinkController>()
        val cryptoApiRepository = mock<CryptoApiRepository>()
        val linkState = linkState(consumerSessionClientSecret)

        whenever(linkController.state(any())).thenReturn(MutableStateFlow(linkState))
        whenever(linkController.configure(any())).thenReturn(Result.success(Unit))
        if (cryptoCustomerId != null && consumerSessionClientSecret != null) {
            whenever(
                cryptoApiRepository.retrieveCryptoCustomer(
                    cryptoCustomerId = cryptoCustomerId,
                    consumerSessionClientSecret = consumerSessionClientSecret,
                )
            ).thenReturn(repositoryResult)
        }

        val interactor = OnrampInteractor(
            application = application,
            linkController = linkController,
            cryptoApiRepository = cryptoApiRepository,
            analyticsServiceFactory = mock<OnrampAnalyticsService.Factory>(),
            errorLogger = mock<OnrampErrorLogger>(),
            checkoutHandler = OnrampSessionClientSecretProvider { "unused" },
            savedStateHandle = SavedStateHandle(),
        )
        if (cryptoCustomerId != null) {
            interactor.configure(configuration(cryptoCustomerId))
        }

        Scenario(
            interactor = interactor,
            cryptoApiRepository = cryptoApiRepository,
        ).block()
    }

    private data class Scenario(
        val interactor: OnrampInteractor,
        val cryptoApiRepository: CryptoApiRepository,
    )

    private companion object {
        const val CRYPTO_CUSTOMER_ID = "crc_123"
        const val CONSUMER_SESSION_CLIENT_SECRET = "secret_123"

        fun configuration(cryptoCustomerId: String): OnrampConfiguration.State {
            return OnrampConfiguration()
                .merchantDisplayName("Test merchant")
                .publishableKey("pk_test_123")
                .appearance(LinkAppearance())
                .cryptoCustomerId(cryptoCustomerId)
                .build()
        }

        fun linkState(consumerSessionClientSecret: String?): LinkController.State {
            return LinkController.State(
                internalLinkAccount = LinkController.LinkAccount(
                    email = "test@example.com",
                    redactedPhoneNumber = "***-***-1234",
                    sessionState = LinkController.SessionState.LoggedIn,
                    consumerSessionClientSecret = consumerSessionClientSecret,
                ),
                merchantLogoUrl = null,
                selectedPaymentMethodPreview = null,
                createdPaymentMethod = null,
                elementsSessionId = null,
            )
        }

        fun customerResponse(
            entries: List<AdditionalKycRequirementResponse> = emptyList(),
        ): RetrieveCryptoCustomerResponse {
            return RetrieveCryptoCustomerResponse(
                id = CRYPTO_CUSTOMER_ID,
                requirements = AdditionalKycRequirementsResponse(entries),
            )
        }

        fun requirement(
            description: String,
            awaitingActionFrom: String,
        ): AdditionalKycRequirementResponse {
            return AdditionalKycRequirementResponse(
                description = description,
                requestedBy = "swapped",
                awaitingActionFrom = awaitingActionFrom,
                submissionType = "document",
            )
        }
    }
}
