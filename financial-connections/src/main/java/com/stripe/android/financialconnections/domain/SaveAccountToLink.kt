package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import com.stripe.android.financialconnections.repository.SuccessContentRepository
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.utils.PollTimingOptions
import com.stripe.android.financialconnections.utils.retryOnException
import com.stripe.android.financialconnections.utils.shouldRetry
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

internal class SaveAccountToLink @Inject constructor(
    private val locale: Locale?,
    private val configuration: FinancialConnectionsSheet.Configuration,
    private val successContentRepository: SuccessContentRepository,
    private val repository: FinancialConnectionsManifestRepository
) {

    suspend fun new(
        email: String,
        phoneNumber: String,
        selectedAccounts: List<PartnerAccount>,
        country: String,
        shouldPollAccountNumbers: Boolean,
    ): FinancialConnectionsSessionManifest {
        return withReadyAccounts(shouldPollAccountNumbers, selectedAccounts) { selectedAccountIds ->
            repository.postSaveAccountsToLink(
                clientSecret = configuration.financialConnectionsSessionClientSecret,
                email = email,
                country = country,
                phoneNumber = phoneNumber,
                locale = (locale ?: Locale.getDefault()).toLanguageTag(),
                consumerSessionClientSecret = null,
                selectedAccounts = selectedAccountIds,
            )
        }
    }

    suspend fun existing(
        consumerSessionClientSecret: String,
        selectedAccounts: List<PartnerAccount>,
        shouldPollAccountNumbers: Boolean,
    ): FinancialConnectionsSessionManifest {
        return withReadyAccounts(shouldPollAccountNumbers, selectedAccounts) { selectedAccountIds ->
            repository.postSaveAccountsToLink(
                clientSecret = configuration.financialConnectionsSessionClientSecret,
                email = null,
                country = null,
                phoneNumber = null,
                locale = null,
                consumerSessionClientSecret = consumerSessionClientSecret,
                selectedAccounts = selectedAccountIds,
            )
        }
    }

    private suspend fun withReadyAccounts(
        shouldPollAccountNumbers: Boolean,
        partnerAccounts: List<PartnerAccount>,
        action: suspend (Set<String>) -> FinancialConnectionsSessionManifest,
    ): FinancialConnectionsSessionManifest {
        val selectedAccountIds = partnerAccounts.map { it.id }.toSet()
        val linkedAccountIds = partnerAccounts.mapNotNull { it.linkedAccountId }.toSet()

        val pollingResult = if (shouldPollAccountNumbers) {
            pollAccountNumbers(linkedAccountIds).onFailure {
                storeFailedToSaveToLinkMessage(selectedAccounts = partnerAccounts.size)
                disableNetworking()
            }
        } else {
            Result.success(Unit)
        }

        return pollingResult.map {
            runCatching {
                action(selectedAccountIds)
            }.updateCustomSuccessMessage(
                selectedAccounts = selectedAccountIds.size,
            ).getOrThrow()
        }.getOrThrow()
    }

    private suspend fun pollAccountNumbers(linkedAccountIds: Set<String>): Result<Unit> {
        return runCatching {
            retryOnException(
                options = PollTimingOptions(
                    initialDelayMs = 1.seconds.inWholeMilliseconds,
                    maxNumberOfRetries = 20,
                ),
                retryCondition = { it.shouldRetry },
                block = { repository.pollAccountNumbers(linkedAccountIds) },
            )
        }
    }

    private suspend fun disableNetworking() {
        repository.disableNetworking(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            disabledReason = "account_numbers_not_available",
        )
    }

    /**
     * Update the custom success message in the [SuccessContentRepository] if the request was successful.
     * If the request failed, update the custom success screen message to an error message
     * (accounts were linked successfully, but not saved to Link).
     */
    private fun Result<FinancialConnectionsSessionManifest>.updateCustomSuccessMessage(
        selectedAccounts: Int
    ): Result<FinancialConnectionsSessionManifest> =
        this.onSuccess { manifest ->
            storeSavedToLinkMessage(manifest, selectedAccounts)
        }.onFailure {
            storeFailedToSaveToLinkMessage(selectedAccounts)
        }

    private fun storeSavedToLinkMessage(
        manifest: FinancialConnectionsSessionManifest,
        selectedAccounts: Int,
    ) {
        successContentRepository.set(
            customSuccessMessage = manifest.displayText?.successPane?.subCaption
                // If backend returns a custom success message, use it
                ?.let { TextResource.Text(it) }
                // If not, build a Link success message locally
                ?: TextResource.PluralId(
                    R.plurals.stripe_success_pane_desc_link_success,
                    selectedAccounts
                )
        )
    }

    private fun storeFailedToSaveToLinkMessage(selectedAccounts: Int) {
        successContentRepository.set(
            customSuccessMessage = TextResource.PluralId(
                value = R.plurals.stripe_success_pane_desc_link_error,
                count = selectedAccounts,
            )
        )
    }
}
