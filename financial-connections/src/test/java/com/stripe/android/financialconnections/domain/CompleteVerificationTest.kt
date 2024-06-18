package com.stripe.android.financialconnections.domain

import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.NETWORKING_LINK_VERIFICATION
import com.stripe.android.financialconnections.model.PaymentAccountParams
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.repository.AttachedPaymentAccountRepository
import com.stripe.android.financialconnections.utils.TestHandleError
import com.stripe.android.financialconnections.utils.TestNavigationManager
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

internal class CompleteVerificationTest {

    private val cachedAccounts: GetCachedAccounts = mock()
    private val saveAccountToLink: SaveAccountToLink = mock()
    private val getOrFetchSync: GetOrFetchSync = mock()
    private val analyticsTracker: TestFinancialConnectionsAnalyticsTracker = TestFinancialConnectionsAnalyticsTracker()
    private val navigationManager: TestNavigationManager = TestNavigationManager()
    private val logger: Logger = Logger.noop()
    private val markLinkVerified: MarkLinkVerified = mock()
    private val attachedPaymentAccountRepository: AttachedPaymentAccountRepository = mock()
    private val handleError: TestHandleError = TestHandleError()

    private val syncResponse = syncResponse().copy(
        manifest = ApiKeyFixtures.sessionManifest().copy(
            // data flow.
            paymentMethodType = null
        )
    )

    private val attachedBankAccount = PaymentAccountParams.BankAccount("id", "token")

    private val completeVerification = CompleteVerification(
        cachedAccounts = cachedAccounts,
        saveAccountToLink = saveAccountToLink,
        getOrFetchSync = getOrFetchSync,
        analyticsTracker = analyticsTracker,
        navigationManager = navigationManager,
        logger = logger,
        handleError = handleError,
        markLinkVerified = markLinkVerified,
        attachedPaymentAccountRepository = attachedPaymentAccountRepository
    )

    @Test
    fun `invoke with non-empty accounts saves to link and marks verified`() = runTest {
        val cachedAccounts = listOf(CachedPartnerAccount("id", "linked_account_id"))
        whenever(cachedAccounts()).thenReturn(cachedAccounts)
        whenever(getOrFetchSync()).thenReturn(syncResponse)

        completeVerification.invoke(NETWORKING_LINK_VERIFICATION, "secret")

        verify(saveAccountToLink).existing(
            consumerSessionClientSecret = eq("secret"),
            selectedAccounts = eq(cachedAccounts),
            // account numbers should be polled on data flows.
            shouldPollAccountNumbers = eq(true)
        )
        verify(markLinkVerified).invoke()
    }

    @Test
    fun `invoke with empty accounts and attached bank account saves to link and marks verified`() = runTest {
        whenever(cachedAccounts()).thenReturn(emptyList())
        whenever(getOrFetchSync()).thenReturn(syncResponse)

        whenever(attachedPaymentAccountRepository.get()).thenReturn(
            AttachedPaymentAccountRepository.State(attachedBankAccount)
        )

        completeVerification.invoke(NETWORKING_LINK_VERIFICATION, "secret")

        verify(saveAccountToLink).existing(
            consumerSessionClientSecret = eq("secret"),
            // no accounts selected, but a bank account (manually entered) is attached
            selectedAccounts = eq(null),
            // account numbers should be polled on data flows.
            shouldPollAccountNumbers = eq(true)
        )
        verify(markLinkVerified).invoke()
    }

    @Test
    fun `invoke with empty accounts and no attached bank account just marks link verified`() = runTest {
        whenever(cachedAccounts()).thenReturn(emptyList())
        whenever(getOrFetchSync()).thenReturn(syncResponse)

        whenever(attachedPaymentAccountRepository.get()).thenReturn(null)

        completeVerification.invoke(NETWORKING_LINK_VERIFICATION, "secret")

        verifyNoInteractions(saveAccountToLink)
        verify(markLinkVerified).invoke()
        navigationManager.assertNavigatedTo(
            destination = Destination.LinkAccountPicker,
            pane = NETWORKING_LINK_VERIFICATION
        )
    }

    @Test
    fun `invoke with non-empty accounts and saveAccountToLink fails still marks link verified and navigates to success`() =
        runTest {
            val cachedAccounts = listOf(CachedPartnerAccount("id", "linked_account_id"))
            whenever(cachedAccounts()).thenReturn(cachedAccounts)
            whenever(getOrFetchSync()).thenReturn(syncResponse)
            whenever(saveAccountToLink.existing(any(), any(), any())).thenThrow(RuntimeException())

            completeVerification.invoke(NETWORKING_LINK_VERIFICATION, "secret")

            analyticsTracker.assertContainsEvent(
                "linked_accounts.networking.verification.error",
                mapOf(
                    "pane" to "networking_link_verification",
                    "error" to "SaveToLinkError"
                )
            )
        }

    @Test
    fun `invoke with non-empty accounts and markLinkVerified throws exception`() = runTest {
        val cachedAccounts = listOf(CachedPartnerAccount("id", "linked_account_id"))
        whenever(cachedAccounts()).thenReturn(cachedAccounts)
        whenever(getOrFetchSync()).thenReturn(syncResponse)
        val linkVerifiedError = RuntimeException()
        whenever(markLinkVerified()).thenThrow(linkVerifiedError)

        completeVerification.invoke(NETWORKING_LINK_VERIFICATION, "secret")

        // saving accounts to link still succeeds, even if markLinkVerified fails.
        analyticsTracker.assertContainsEvent(
            "linked_accounts.networking.verification.success",
            mapOf(
                "pane" to "networking_link_verification",
            )
        )

        handleError.assertError(
            extraMessage = "Error marking link as verified",
            error = linkVerifiedError,
            pane = NETWORKING_LINK_VERIFICATION,
            displayErrorScreen = true

        )
    }
}
