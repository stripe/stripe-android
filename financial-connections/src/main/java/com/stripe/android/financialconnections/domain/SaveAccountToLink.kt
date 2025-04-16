package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.PaymentAccountParams.BankAccount
import com.stripe.android.financialconnections.repository.AttachedPaymentAccountRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import com.stripe.android.financialconnections.repository.SuccessContentRepository
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.utils.PollTimingOptions
import com.stripe.android.financialconnections.utils.retryOnException
import com.stripe.android.financialconnections.utils.shouldRetry
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds

internal class SaveAccountToLink @Inject constructor(
    private val locale: Locale?,
    private val configuration: FinancialConnectionsSheetConfiguration,
    private val attachedPaymentAccountRepository: AttachedPaymentAccountRepository,
    private val successContentRepository: SuccessContentRepository,
    private val repository: FinancialConnectionsManifestRepository,
    private val accountsRepository: FinancialConnectionsAccountsRepository,
    private val isNetworkingRelinkSession: IsNetworkingRelinkSession,
) {

    suspend fun new(
        email: String,
        phoneNumber: String,
        selectedAccounts: List<CachedPartnerAccount>,
        country: String,
        shouldPollAccountNumbers: Boolean,
    ): FinancialConnectionsSessionManifest {
        return ensureReadyAccounts(shouldPollAccountNumbers, selectedAccounts) { selectedAccountIds ->
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
        selectedAccounts: List<CachedPartnerAccount>?,
        shouldPollAccountNumbers: Boolean,
    ): FinancialConnectionsSessionManifest {
        return ensureReadyAccounts(shouldPollAccountNumbers, selectedAccounts) { selectedAccountIds ->
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

    private suspend fun ensureReadyAccounts(
        shouldPollAccountNumbers: Boolean,
        partnerAccounts: List<CachedPartnerAccount>?,
        action: suspend (Set<String>?) -> FinancialConnectionsSessionManifest,
    ): FinancialConnectionsSessionManifest {
        val selectedAccountIds = partnerAccounts?.map { it.id }?.toSet() ?: emptySet()
        val linkedAccountIds = partnerAccounts?.mapNotNull { it.linkedAccountId }?.toSet() ?: emptySet()

        val pollingResult = when {
            partnerAccounts.isNullOrEmpty() -> when (attachedPaymentAccountRepository.get()?.attachedPaymentAccount) {
                is BankAccount -> Result.success(Unit)
                else -> Result.failure(
                    IllegalStateException(
                        "Must have a bank account attached if no accounts are selected"
                    )
                )
            }
            shouldPollAccountNumbers -> runCatching { awaitAccountNumbersReady(linkedAccountIds) }
            else -> Result.success(Unit)
        }

        return pollingResult.onFailure {
            disableNetworking()
        }.mapCatching {
            action(selectedAccountIds)
        }.onSuccess { manifest ->
            if (!isNetworkingRelinkSession()) {
                storeSavedToLinkMessage(manifest, selectedAccountIds.size)
            }
        }.onFailure {
            if (!isNetworkingRelinkSession()) {
                storeFailedToSaveToLinkMessage(selectedAccountIds.size)
            }
        }.getOrThrow()
    }

    private suspend fun awaitAccountNumbersReady(linkedAccountIds: Set<String>) {
        return retryOnException(
            options = PollTimingOptions(
                initialDelayMs = 1.seconds.inWholeMilliseconds,
                maxNumberOfRetries = 20,
            ),
            retryCondition = { it.shouldRetry },
            block = { accountsRepository.pollAccountNumbers(linkedAccountIds) },
        )
    }

    private suspend fun disableNetworking() {
        repository.disableNetworking(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            disabledReason = "account_numbers_not_available",
            clientSuggestedNextPaneOnDisableNetworking = null
        )
    }

    private fun storeSavedToLinkMessage(
        manifest: FinancialConnectionsSessionManifest,
        selectedAccounts: Int,
    ) {
        successContentRepository.set(
            heading = manifest.displayText?.successPane?.caption
                ?.let { TextResource.Text(it) },
            message = manifest.displayText?.successPane?.subCaption
                // If backend returns a custom success message, use it
                ?.let { TextResource.Text(it) }
                // If not, build a Link success message locally
                ?: TextResource.PluralId(
                    singular = R.string.stripe_success_pane_desc_link_success_singular,
                    plural = R.string.stripe_success_pane_desc_link_success_plural,
                    // No selected accounts means a manually entered account was already attached.
                    count = max(1, selectedAccounts),
                )
        )
    }

    private fun storeFailedToSaveToLinkMessage(selectedAccounts: Int) {
        successContentRepository.set(
            message = TextResource.PluralId(
                singular = R.string.stripe_success_pane_desc_link_error_singular,
                plural = R.string.stripe_success_pane_desc_link_error_plural,
                // No selected accounts means a manually entered account was already attached.
                count = max(1, selectedAccounts),
            )
        )
    }
}
