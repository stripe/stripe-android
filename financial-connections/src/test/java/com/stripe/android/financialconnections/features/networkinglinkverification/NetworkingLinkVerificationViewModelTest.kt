package com.stripe.android.financialconnections.features.networkinglinkverification

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.financialconnections.ApiKeyFixtures.cachedConsumerSession
import com.stripe.android.financialconnections.ApiKeyFixtures.consumerSession
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.CoroutineTestRule
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.AttachConsumerToLinkAccountSession
import com.stripe.android.financialconnections.domain.ConfirmVerification
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.GetOrFetchSync.RefetchCondition
import com.stripe.android.financialconnections.domain.HandleError
import com.stripe.android.financialconnections.domain.LookupConsumerAndStartVerification
import com.stripe.android.financialconnections.domain.MarkLinkVerified
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.INSTITUTION_PICKER
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.NETWORKING_LINK_VERIFICATION
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.repository.CachedConsumerSession
import com.stripe.android.financialconnections.utils.TestNavigationManager
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.VerificationType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class NetworkingLinkVerificationViewModelTest {

    @get:Rule
    val testRule = CoroutineTestRule()

    private val getOrFetchSync = mock<GetOrFetchSync>()
    private val navigationManager = TestNavigationManager()
    private val confirmVerification = mock<ConfirmVerification>()
    private val lookupConsumerAndStartVerification = mock<LookupConsumerAndStartVerification>()
    private val markLinkVerified = mock<MarkLinkVerified>()
    private val analyticsTracker = TestFinancialConnectionsAnalyticsTracker()
    private val nativeAuthFlowCoordinator = NativeAuthFlowCoordinator()
    private val attachConsumerToLinkAccountSession = mock<AttachConsumerToLinkAccountSession>()
    private val handleError = mock<HandleError>()

    private fun buildViewModel(
        state: NetworkingLinkVerificationState = NetworkingLinkVerificationState(),
        consumerSession: CachedConsumerSession? = cachedConsumerSession(),
        isLinkWithStripe: Boolean = false,
    ) = NetworkingLinkVerificationViewModel(
        navigationManager = navigationManager,
        getOrFetchSync = getOrFetchSync,
        lookupConsumerAndStartVerification = lookupConsumerAndStartVerification,
        confirmVerification = confirmVerification,
        markLinkVerified = markLinkVerified,
        analyticsTracker = analyticsTracker,
        logger = Logger.noop(),
        initialState = state,
        nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
        consumerSessionProvider = { consumerSession },
        isLinkWithStripe = { isLinkWithStripe },
        attachConsumerToLinkAccountSession = attachConsumerToLinkAccountSession,
        handleError = handleError,
    )

    @Test
    fun `init - uses consumersession email over accountholder customer email if in Instant Debits`() = runTest {
        val consumerSession = cachedConsumerSession()
        val manifest = sessionManifest().copy(
            accountholderCustomerEmailAddress = "email@email.com",
        )

        whenever(getOrFetchSync()).thenReturn(
            syncResponse(manifest)
        )

        buildViewModel(
            isLinkWithStripe = true,
            consumerSession = consumerSession
        )

        verify(lookupConsumerAndStartVerification).invoke(
            email = eq(consumerSession.emailAddress),
            businessName = anyOrNull(),
            verificationType = any(),
            onConsumerNotFound = any(),
            onLookupError = any(),
            onStartVerification = any(),
            onVerificationStarted = any(),
            onStartVerificationError = any(),
        )
    }

    @Test
    fun `init - falls back to accountholder customer email if no consumer session in Instant Debits`() = runTest {
        val manifest = sessionManifest().copy(
            accountholderCustomerEmailAddress = "email@email.com",
        )

        whenever(getOrFetchSync()).thenReturn(
            syncResponse(manifest)
        )

        buildViewModel(
            consumerSession = null,
            isLinkWithStripe = true
        )

        verify(lookupConsumerAndStartVerification).invoke(
            email = eq("email@email.com"),
            businessName = anyOrNull(),
            verificationType = any(),
            onConsumerNotFound = any(),
            onLookupError = any(),
            onStartVerification = any(),
            onVerificationStarted = any(),
            onStartVerificationError = any(),
        )
    }

    @Test
    fun `init - starts SMS verification with consumer session secret`() = runTest {
        val email = "test@test.com"
        val consumerSession = consumerSession()
        whenever(getOrFetchSync()).thenReturn(
            syncResponse(sessionManifest().copy(accountholderCustomerEmailAddress = email))
        )

        val onStartVerificationCaptor = argumentCaptor<suspend () -> Unit>()
        val onVerificationStartedCaptor = argumentCaptor<suspend (ConsumerSession) -> Unit>()

        val viewModel = buildViewModel()

        assertThat(viewModel.stateFlow.value.payload).isInstanceOf(Loading::class.java)

        verify(lookupConsumerAndStartVerification).invoke(
            email = eq(email),
            businessName = anyOrNull(),
            verificationType = eq(VerificationType.SMS),
            onConsumerNotFound = any(),
            onLookupError = any(),
            onStartVerification = onStartVerificationCaptor.capture(),
            onVerificationStarted = onVerificationStartedCaptor.capture(),
            onStartVerificationError = any()
        )

        onStartVerificationCaptor.firstValue()
        onVerificationStartedCaptor.firstValue(consumerSession)

        val state = viewModel.stateFlow.value
        assertThat(state.payload()!!.consumerSessionClientSecret)
            .isEqualTo(consumerSession.clientSecret)
    }

    @Test
    fun `init - ConsumerNotFound sends analytics and navigates to institution picker`() = runTest {
        val email = "test@test.com"
        val onConsumerNotFoundCaptor = argumentCaptor<suspend () -> Unit>()

        whenever(getOrFetchSync()).thenReturn(
            syncResponse(sessionManifest().copy(accountholderCustomerEmailAddress = email))
        )

        val viewModel = buildViewModel()

        assertThat(viewModel.stateFlow.value.payload).isInstanceOf(Loading::class.java)

        verify(lookupConsumerAndStartVerification).invoke(
            email = eq(email),
            businessName = anyOrNull(),
            verificationType = eq(VerificationType.SMS),
            onConsumerNotFound = onConsumerNotFoundCaptor.capture(),
            onLookupError = any(),
            onStartVerification = any(),
            onVerificationStarted = any(),
            onStartVerificationError = any()
        )

        onConsumerNotFoundCaptor.firstValue()

        assertThat(viewModel.stateFlow.value.payload).isInstanceOf(Loading::class.java)
        navigationManager.assertNavigatedTo(
            destination = Destination.InstitutionPicker,
            pane = NETWORKING_LINK_VERIFICATION
        )

        analyticsTracker.assertContainsEvent(
            "linked_accounts.networking.verification.error",
            mapOf(
                "pane" to "networking_link_verification",
                "error" to "ConsumerNotFoundError"
            )
        )
    }

    @Test
    fun `otpEntered - valid OTP and confirms navigates to LINK_ACCOUNT_PICKER`() =
        runTest {
            val email = "test@test.com"
            val consumerSession = consumerSession()
            val onStartVerificationCaptor = argumentCaptor<suspend () -> Unit>()
            val onVerificationStartedCaptor = argumentCaptor<suspend (ConsumerSession) -> Unit>()
            val linkVerifiedManifest = sessionManifest().copy(nextPane = INSTITUTION_PICKER)
            whenever(getOrFetchSync()).thenReturn(
                syncResponse(sessionManifest().copy(accountholderCustomerEmailAddress = email))
            )

            // polling returns some networked accounts
            whenever(markLinkVerified()).thenReturn((linkVerifiedManifest))

            val viewModel = buildViewModel()

            verify(lookupConsumerAndStartVerification).invoke(
                email = eq(email),
                businessName = anyOrNull(),
                verificationType = eq(VerificationType.SMS),
                onConsumerNotFound = any(),
                onLookupError = any(),
                onStartVerification = onStartVerificationCaptor.capture(),
                onVerificationStarted = onVerificationStartedCaptor.capture(),
                onStartVerificationError = any()
            )

            onStartVerificationCaptor.firstValue()
            onVerificationStartedCaptor.firstValue(consumerSession)

            val otpController = viewModel.stateFlow.value.payload()!!.otpElement.controller

            // enters valid OTP
            for (i in 0 until otpController.otpLength) {
                otpController.onValueChanged(i, "1")
            }

            verify(confirmVerification).sms(any(), eq("111111"))
            navigationManager.assertNavigatedTo(
                destination = Destination.LinkAccountPicker,
                pane = NETWORKING_LINK_VERIFICATION
            )
        }

    @Test
    fun `otpEntered - save to link fails with no initial institution navigates to INSTITUTI`() =
        runTest {
            val email = "test@test.com"
            val consumerSession = consumerSession()
            val onStartVerificationCaptor = argumentCaptor<suspend () -> Unit>()
            val onVerificationStartedCaptor = argumentCaptor<suspend (ConsumerSession) -> Unit>()

            whenever(getOrFetchSync()).thenReturn(
                syncResponse(
                    sessionManifest().copy(
                        accountholderCustomerEmailAddress = email,
                        initialInstitution = null
                    )
                )
            )

            // polling returns some networked accounts
            whenever(markLinkVerified()).thenAnswer {
                throw LocalStripeException(
                    displayMessage = "error marking link as verified",
                    analyticsValue = null
                )
            }

            val viewModel = buildViewModel()

            verify(lookupConsumerAndStartVerification).invoke(
                email = eq(email),
                businessName = anyOrNull(),
                verificationType = eq(VerificationType.SMS),
                onConsumerNotFound = any(),
                onLookupError = any(),
                onStartVerification = onStartVerificationCaptor.capture(),
                onVerificationStarted = onVerificationStartedCaptor.capture(),
                onStartVerificationError = any()
            )

            onStartVerificationCaptor.firstValue()
            onVerificationStartedCaptor.firstValue(consumerSession)

            val otpController = viewModel.stateFlow.value.payload()!!.otpElement.controller

            // enters valid OTP
            for (i in 0 until otpController.otpLength) {
                otpController.onValueChanged(i, "1")
            }

            verify(confirmVerification).sms(any(), eq("111111"))
            navigationManager.assertNavigatedTo(
                destination = Destination.InstitutionPicker,
                pane = NETWORKING_LINK_VERIFICATION
            )
        }

    @Test
    fun `otpEntered - attaches consumer to LAS and navigates to account picker in Instant Debits`() = runTest {
        val consumerSession = consumerSession()
        val onStartVerificationCaptor = argumentCaptor<suspend () -> Unit>()
        val onVerificationStartedCaptor = argumentCaptor<suspend (ConsumerSession) -> Unit>()

        whenever(getOrFetchSync(any(), anyOrNull())).thenReturn(
            syncResponse(sessionManifest().copy(accountholderCustomerEmailAddress = consumerSession.emailAddress))
        )
        whenever(attachConsumerToLinkAccountSession.invoke(any())).thenReturn(Unit)

        val viewModel = buildViewModel(isLinkWithStripe = true)

        verify(lookupConsumerAndStartVerification).invoke(
            email = eq(consumerSession.emailAddress),
            businessName = anyOrNull(),
            verificationType = eq(VerificationType.SMS),
            onConsumerNotFound = any(),
            onLookupError = any(),
            onStartVerification = onStartVerificationCaptor.capture(),
            onVerificationStarted = onVerificationStartedCaptor.capture(),
            onStartVerificationError = any()
        )

        onStartVerificationCaptor.firstValue()
        onVerificationStartedCaptor.firstValue(consumerSession)

        val otpController = viewModel.stateFlow.value.payload()!!.otpElement.controller

        val otpCode = "111111"

        for (index in otpCode.indices) {
            otpController.onValueChanged(index, otpCode[index].toString())
        }

        verify(attachConsumerToLinkAccountSession).invoke(consumerSession.clientSecret)
        verify(getOrFetchSync).invoke(RefetchCondition.Always)
        verify(markLinkVerified, never()).invoke()

        navigationManager.assertNavigatedTo(
            destination = Destination.LinkAccountPicker,
            pane = NETWORKING_LINK_VERIFICATION,
        )
    }

    @Test
    fun `otpEntered - shows terminal error if failing to attach consumer to LAS in Instant Debits`() = runTest {
        val consumerSession = consumerSession()
        val onStartVerificationCaptor = argumentCaptor<suspend () -> Unit>()
        val onVerificationStartedCaptor = argumentCaptor<suspend (ConsumerSession) -> Unit>()

        whenever(getOrFetchSync(any(), anyOrNull())).thenReturn(
            syncResponse(sessionManifest().copy(accountholderCustomerEmailAddress = consumerSession.emailAddress))
        )

        whenever(attachConsumerToLinkAccountSession.invoke(any())).then {
            throw APIConnectionException()
        }

        val viewModel = buildViewModel(isLinkWithStripe = true)

        verify(lookupConsumerAndStartVerification).invoke(
            email = eq(consumerSession.emailAddress),
            businessName = anyOrNull(),
            verificationType = eq(VerificationType.SMS),
            onConsumerNotFound = any(),
            onLookupError = any(),
            onStartVerification = onStartVerificationCaptor.capture(),
            onVerificationStarted = onVerificationStartedCaptor.capture(),
            onStartVerificationError = any()
        )

        onStartVerificationCaptor.firstValue()
        onVerificationStartedCaptor.firstValue(consumerSession)

        val otpController = viewModel.stateFlow.value.payload()!!.otpElement.controller

        val otpCode = "111111"

        for (index in otpCode.indices) {
            otpController.onValueChanged(index, otpCode[index].toString())
        }

        verify(attachConsumerToLinkAccountSession).invoke(consumerSession.clientSecret)
        verify(handleError).invoke(
            extraMessage = any(),
            error = any(),
            pane = eq(NETWORKING_LINK_VERIFICATION),
            displayErrorScreen = eq(true),
        )
    }
}
