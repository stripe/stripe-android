package com.stripe.android.financialconnections.domain

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.PaymentAccountParams
import com.stripe.android.financialconnections.networking.AbsFinancialConnectionsAccountsRepository
import com.stripe.android.financialconnections.networking.AbsFinancialConnectionsManifestRepository
import com.stripe.android.financialconnections.repository.AttachedPaymentAccountRepository
import com.stripe.android.financialconnections.repository.AttachedPaymentAccountRepository.State
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import com.stripe.android.financialconnections.repository.SuccessContentRepository
import com.stripe.android.financialconnections.ui.TextResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import java.util.Locale
import kotlin.test.assertFails

@OptIn(ExperimentalCoroutinesApi::class)
internal class SaveAccountToLinkTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun `Polls account numbers if requested to do so`() = runTest(testDispatcher) {
        val polledAccountIds = mutableSetOf<String>()

        val partnerAccounts = ApiKeyFixtures.cachedPartnerAccounts()

        val accountsRepository = mockAccountsRepository(
            onPollAccountNumbers = polledAccountIds::addAll,
        )

        val saveAccountToLink = makeSaveAccountToLink(accountsRepository = accountsRepository)

        saveAccountToLink.new(
            email = "email@email.com",
            phoneNumber = "+15555555555",
            selectedAccounts = partnerAccounts,
            country = "US",
            shouldPollAccountNumbers = true,
        )

        assertThat(polledAccountIds).containsExactly("linked_id_1", "linked_id_2")
    }

    @Test
    fun `Skips polling account numbers if not requested to do so`() = runTest(testDispatcher) {
        val polledAccountIds = mutableSetOf<String>()

        val partnerAccounts = ApiKeyFixtures.cachedPartnerAccounts()

        val accountsRepository = mockAccountsRepository(
            onPollAccountNumbers = polledAccountIds::addAll,
        )

        val saveAccountToLink = makeSaveAccountToLink(accountsRepository = accountsRepository)

        saveAccountToLink.new(
            email = "email@email.com",
            phoneNumber = "+15555555555",
            selectedAccounts = partnerAccounts,
            country = "US",
            shouldPollAccountNumbers = false,
        )

        assertThat(polledAccountIds).isEmpty()
    }

    @Test
    fun `Disables networking if polling account numbers fails`() = runTest(testDispatcher) {
        var disabledNetworking = false

        val partnerAccounts = ApiKeyFixtures.cachedPartnerAccounts()

        val repository = mockManifestRepository(
            onDisabledNetworking = { disabledNetworking = true },
        )

        val accountsRepository = mockAccountsRepository(
            onPollAccountNumbers = { error("This is failing") },
        )

        val saveAccountToLink = makeSaveAccountToLink(
            repository = repository,
            accountsRepository = accountsRepository,
        )

        assertFails {
            saveAccountToLink.new(
                email = "email@email.com",
                phoneNumber = "+15555555555",
                selectedAccounts = partnerAccounts,
                country = "US",
                shouldPollAccountNumbers = true,
            )
        }

        assertThat(disabledNetworking).isTrue()
    }

    @Test
    fun `Sets custom success message if polling account numbers fails`() = runTest(testDispatcher) {
        val partnerAccounts = ApiKeyFixtures.cachedPartnerAccounts()

        val accountsRepository = mockAccountsRepository(
            onPollAccountNumbers = { error("This is failing") },
        )

        val successRepository = SuccessContentRepository(SavedStateHandle())

        val saveAccountToLink = makeSaveAccountToLink(
            accountsRepository = accountsRepository,
            successRepository = successRepository,
        )

        assertFails {
            saveAccountToLink.new(
                email = "email@email.com",
                phoneNumber = "+15555555555",
                selectedAccounts = partnerAccounts,
                country = "US",
                shouldPollAccountNumbers = true,
            )
        }

        assertThat(successRepository.get()?.message).isEqualTo(
            TextResource.PluralId(
                singular = R.string.stripe_success_pane_desc_link_error_singular,
                plural = R.string.stripe_success_pane_desc_link_error_plural,
                count = 2,
            )
        )
    }

    @Test
    fun `Sets custom success message with one account if manual entry account was attached`() =
        runTest(testDispatcher) {
            val accountsRepository = mockAccountsRepository()
            val attachedPaymentAccountRepository = mock<AttachedPaymentAccountRepository>()
            val successRepository = SuccessContentRepository(SavedStateHandle())

            whenever(attachedPaymentAccountRepository.get()).thenReturn(
                State(
                    attachedPaymentAccount = PaymentAccountParams.BankAccount(
                        accountNumber = "acct_123",
                        routingNumber = "110000000",
                    )
                )
            )

            val saveAccountToLink = makeSaveAccountToLink(
                accountsRepository = accountsRepository,
                successRepository = successRepository,
                attachedPaymentAccountRepository = attachedPaymentAccountRepository,
            )

            saveAccountToLink.new(
                email = "email@email.com",
                phoneNumber = "+15555555555",
                selectedAccounts = emptyList(),
                country = "US",
                shouldPollAccountNumbers = true,
            )

            assertThat(successRepository.get()?.message).isEqualTo(
                TextResource.PluralId(
                    singular = R.string.stripe_success_pane_desc_link_success_singular,
                    plural = R.string.stripe_success_pane_desc_link_success_plural,
                    count = 1,
                )
            )
        }

    @Test
    fun `Doesn't set success message if in networking relink session`() = runTest(testDispatcher) {
        val accountsRepository = mockAccountsRepository()
        val attachedPaymentAccountRepository = mock<AttachedPaymentAccountRepository>()
        val successRepository = SuccessContentRepository(SavedStateHandle())

        whenever(attachedPaymentAccountRepository.get()).thenReturn(
            State(
                attachedPaymentAccount = PaymentAccountParams.BankAccount(
                    accountNumber = "acct_123",
                    routingNumber = "110000000",
                )
            )
        )

        val saveAccountToLink = makeSaveAccountToLink(
            accountsRepository = accountsRepository,
            successRepository = successRepository,
            attachedPaymentAccountRepository = attachedPaymentAccountRepository,
            isNetworkingRelinkSession = { true },
        )

        saveAccountToLink.existing(
            consumerSessionClientSecret = "cscs_123",
            selectedAccounts = emptyList(),
            shouldPollAccountNumbers = true,
        )

        assertThat(successRepository.get()).isNull()
    }

    private fun makeSaveAccountToLink(
        repository: FinancialConnectionsManifestRepository = mockManifestRepository(),
        accountsRepository: FinancialConnectionsAccountsRepository = mockAccountsRepository(),
        successRepository: SuccessContentRepository = SuccessContentRepository(SavedStateHandle()),
        attachedPaymentAccountRepository: AttachedPaymentAccountRepository = mock(),
        isNetworkingRelinkSession: IsNetworkingRelinkSession = IsNetworkingRelinkSession { false },
    ): SaveAccountToLink {
        return SaveAccountToLink(
            locale = Locale.getDefault(),
            configuration = FinancialConnectionsSheetConfiguration(
                ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
                ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
            ),
            successContentRepository = successRepository,
            repository = repository,
            attachedPaymentAccountRepository = attachedPaymentAccountRepository,
            accountsRepository = accountsRepository,
            isNetworkingRelinkSession = isNetworkingRelinkSession,
        )
    }

    private fun mockManifestRepository(
        onDisabledNetworking: () -> Unit = {},
    ): FinancialConnectionsManifestRepository {
        return object : AbsFinancialConnectionsManifestRepository() {

            override suspend fun postSaveAccountsToLink(
                clientSecret: String,
                email: String?,
                country: String?,
                locale: String?,
                phoneNumber: String?,
                consumerSessionClientSecret: String?,
                selectedAccounts: Set<String>?
            ): FinancialConnectionsSessionManifest {
                return sessionManifest()
            }

            override suspend fun disableNetworking(
                clientSecret: String,
                disabledReason: String?,
                clientSuggestedNextPaneOnDisableNetworking: String?
            ): FinancialConnectionsSessionManifest {
                onDisabledNetworking()
                return sessionManifest()
            }
        }
    }

    private fun mockAccountsRepository(
        onPollAccountNumbers: (Set<String>) -> Unit = {},
    ): FinancialConnectionsAccountsRepository {
        return object : AbsFinancialConnectionsAccountsRepository() {

            override suspend fun pollAccountNumbers(linkedAccounts: Set<String>) {
                onPollAccountNumbers(linkedAccounts)
            }
        }
    }
}
