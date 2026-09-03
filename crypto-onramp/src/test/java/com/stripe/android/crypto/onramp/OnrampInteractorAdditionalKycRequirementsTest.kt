package com.stripe.android.crypto.onramp

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.analytics.OnrampAnalyticsService
import com.stripe.android.crypto.onramp.exception.LinkAccountNotVerifiedException
import com.stripe.android.crypto.onramp.exception.MissingConsumerSecretException
import com.stripe.android.crypto.onramp.exception.OnrampErrorLogger
import com.stripe.android.crypto.onramp.exception.UnexpectedException
import com.stripe.android.crypto.onramp.model.AdditionalKycRequirementResponse
import com.stripe.android.crypto.onramp.model.AdditionalKycRequirementsResponse
import com.stripe.android.crypto.onramp.model.OnrampSessionClientSecretProvider
import com.stripe.android.crypto.onramp.model.RetrieveCryptoCustomerResponse
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
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
            consumerSessionClientSecret = CONSUMER_SESSION_CLIENT_SECRET,
        )
    }

    @Test
    fun `customer with no requirement entries returns empty classifications`() = runScenario(
        repositoryResult = Result.success(
            RetrieveCryptoCustomerResponse(
                requirements = AdditionalKycRequirementsResponse(entries = emptyList())
            )
        ),
    ) {
        val result = interactor.retrieveAdditionalKycRequirements().getOrThrow()

        assertThat(result.userActionRequired).isEmpty()
        assertThat(result.pendingPartnerAction).isEmpty()
        assertThat(result.pendingStripeAction).isEmpty()
        assertThat(result.unrecognizedActionOwner).isEmpty()
    }

    @Test
    fun `requirements are retrieved without a crypto customer ID`() = runScenario {
        val result = interactor.retrieveAdditionalKycRequirements()

        assertThat(result.isSuccess).isTrue()
        verify(cryptoApiRepository).retrieveCryptoCustomer(CONSUMER_SESSION_CLIENT_SECRET)
    }

    @Test
    fun `missing consumer secret returns failure without requesting customer`() = runScenario(
        consumerSessionClientSecret = null,
    ) {
        val error = interactor.retrieveAdditionalKycRequirements().exceptionOrNull()

        assertUnexpectedError<MissingConsumerSecretException>(error)
        verify(cryptoApiRepository, never()).retrieveCryptoCustomer(any())
    }

    @Test
    fun `unverified Link account returns failure without requesting customer`() = runScenario(
        linkSessionState = LinkController.SessionState.NeedsVerification,
    ) {
        val error = interactor.retrieveAdditionalKycRequirements().exceptionOrNull()

        assertUnexpectedError<LinkAccountNotVerifiedException>(error)
        verify(cryptoApiRepository, never()).retrieveCryptoCustomer(any())
    }

    @Test
    fun `repository failure is propagated`() = runScenario(
        repositoryResult = Result.failure(expectedRepositoryError),
    ) {
        val error = interactor.retrieveAdditionalKycRequirements().exceptionOrNull()

        val unexpectedError = assertUnexpectedError<IllegalStateException>(error)
        assertThat(unexpectedError.underlyingError).isSameInstanceAs(expectedRepositoryError)
    }

    private fun runScenario(
        consumerSessionClientSecret: String? = CONSUMER_SESSION_CLIENT_SECRET,
        linkSessionState: LinkController.SessionState = LinkController.SessionState.LoggedIn,
        repositoryResult: Result<RetrieveCryptoCustomerResponse> = Result.success(customerResponse()),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val application = createApplication()
        val linkController = mock<LinkController>()
        val cryptoApiRepository = mock<CryptoApiRepository>()
        val linkState = linkState(consumerSessionClientSecret, linkSessionState)

        whenever(linkController.state(any())).thenReturn(MutableStateFlow(linkState))
        if (consumerSessionClientSecret != null) {
            whenever(
                cryptoApiRepository.retrieveCryptoCustomer(
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
        Scenario(
            interactor = interactor,
            cryptoApiRepository = cryptoApiRepository,
        ).block()
    }

    private data class Scenario(
        val interactor: OnrampInteractor,
        val cryptoApiRepository: CryptoApiRepository,
    )

    private inline fun <reified T : Throwable> assertUnexpectedError(error: Throwable?): UnexpectedException {
        assertThat(error).isInstanceOf(UnexpectedException::class.java)
        return (error as UnexpectedException).also {
            assertThat(it.underlyingError).isInstanceOf(T::class.java)
        }
    }

    private fun createApplication(): Application {
        val application = mock<Application>()
        val runtimeApplication: Application = RuntimeEnvironment.getApplication()
        whenever(application.packageName).thenReturn(runtimeApplication.packageName)
        whenever(application.getString(R.string.stripe_onramp_default_api_error_user_message))
            .thenReturn("Something went wrong. Please try again later.")
        return application
    }

    private companion object {
        const val CONSUMER_SESSION_CLIENT_SECRET = "secret_123"

        fun linkState(
            consumerSessionClientSecret: String?,
            sessionState: LinkController.SessionState,
        ): LinkController.State {
            return LinkController.State(
                internalLinkAccount = LinkController.LinkAccount(
                    email = "test@example.com",
                    redactedPhoneNumber = "***-***-1234",
                    sessionState = sessionState,
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
                errors = emptyList(),
                submissionType = "document",
            )
        }
    }
}
