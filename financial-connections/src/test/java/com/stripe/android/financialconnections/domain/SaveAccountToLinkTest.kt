package com.stripe.android.financialconnections.domain

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.ApiKeyFixtures.partnerAccount
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.networking.AbsFinancialConnectionsManifestRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import com.stripe.android.financialconnections.repository.SuccessContentRepository
import com.stripe.android.financialconnections.ui.TextResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.Locale
import kotlin.test.assertFails

@OptIn(ExperimentalCoroutinesApi::class)
internal class SaveAccountToLinkTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun `Polls account numbers if requested to do so`() = runTest(testDispatcher) {
        val polledAccountIds = mutableSetOf<String>()

        val partnerAccounts = listOf(
            partnerAccount().copy(id = "id_1", linkedAccountId = "lid_1"),
            partnerAccount().copy(id = "id_2", linkedAccountId = "lid_2"),
        )

        val repository = mockManifestRepository(
            onPollAccountNumbers = polledAccountIds::addAll,
        )

        val saveAccountToLink = SaveAccountToLink(
            locale = Locale.getDefault(),
            configuration = FinancialConnectionsSheet.Configuration(
                ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
                ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
            ),
            successContentRepository = SuccessContentRepository(SavedStateHandle()),
            repository = repository,
        )

        saveAccountToLink.new(
            email = "email@email.com",
            phoneNumber = "+15555555555",
            selectedAccounts = partnerAccounts,
            country = "US",
            shouldPollAccountNumbers = true,
        )

        assertThat(polledAccountIds).containsExactly("lid_1", "lid_2")
    }

    @Test
    fun `Skips polling account numbers if not requested to do so`() = runTest(testDispatcher) {
        val polledAccountIds = mutableSetOf<String>()

        val partnerAccounts = listOf(
            partnerAccount().copy(id = "id_1", linkedAccountId = "lid_1"),
            partnerAccount().copy(id = "id_2", linkedAccountId = "lid_2"),
        )

        val repository = mockManifestRepository(
            onPollAccountNumbers = polledAccountIds::addAll,
        )

        val saveAccountToLink = SaveAccountToLink(
            locale = Locale.getDefault(),
            configuration = FinancialConnectionsSheet.Configuration(
                ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
                ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
            ),
            successContentRepository = SuccessContentRepository(SavedStateHandle()),
            repository = repository,
        )

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

        val partnerAccounts = listOf(
            partnerAccount().copy(id = "id_1", linkedAccountId = "lid_1"),
            partnerAccount().copy(id = "id_2", linkedAccountId = "lid_2"),
        )

        val repository = mockManifestRepository(
            onPollAccountNumbers = { error("This is failing") },
            onDisabledNetworking = { disabledNetworking = true },
        )

        val successRepository = SuccessContentRepository(SavedStateHandle())

        val saveAccountToLink = SaveAccountToLink(
            locale = Locale.getDefault(),
            configuration = FinancialConnectionsSheet.Configuration(
                ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
                ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
            ),
            successContentRepository = successRepository,
            repository = repository,
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

        assertThat(successRepository.get()?.customSuccessMessage).isEqualTo(
            TextResource.PluralId(
                value = R.plurals.stripe_success_pane_desc_link_error,
                count = 2,
            )
        )
    }

    private fun mockManifestRepository(
        onPollAccountNumbers: (Set<String>) -> Unit,
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
                selectedAccounts: Set<String>
            ): FinancialConnectionsSessionManifest {
                return sessionManifest()
            }

            override suspend fun pollAccountNumbers(linkedAccounts: Set<String>): Result<Unit> {
                onPollAccountNumbers(linkedAccounts)
                return Result.success(Unit)
            }

            override suspend fun disableNetworking(
                clientSecret: String,
                disabledReason: String?
            ): FinancialConnectionsSessionManifest {
                onDisabledNetworking()
                return sessionManifest()
            }
        }
    }
}
