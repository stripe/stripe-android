package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import com.stripe.android.financialconnections.repository.SuccessContentRepository
import com.stripe.android.financialconnections.ui.TextResource
import java.util.Locale
import javax.inject.Inject

internal class SaveAccountToLink @Inject constructor(
    private val locale: Locale?,
    private val configuration: FinancialConnectionsSheet.Configuration,
    private val successContentRepository: SuccessContentRepository,
    private val repository: FinancialConnectionsManifestRepository
) {

    suspend fun new(
        email: String,
        phoneNumber: String,
        selectedAccounts: Set<String>,
        country: String
    ): FinancialConnectionsSessionManifest = runCatching {
        repository.postSaveAccountsToLink(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            email = email,
            country = country,
            phoneNumber = phoneNumber,
            locale = (locale ?: Locale.getDefault()).toLanguageTag(),
            consumerSessionClientSecret = null,
            selectedAccounts = selectedAccounts
        )
    }
        .updateCustomSuccessMessage(selectedAccounts.size)
        .getOrThrow()

    suspend fun existing(
        consumerSessionClientSecret: String,
        selectedAccounts: Set<String>,
    ): FinancialConnectionsSessionManifest = runCatching {
        repository.postSaveAccountsToLink(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            email = null,
            country = null,
            phoneNumber = null,
            locale = null,
            consumerSessionClientSecret = consumerSessionClientSecret,
            selectedAccounts = selectedAccounts
        )
    }
        .updateCustomSuccessMessage(selectedAccounts.size)
        .getOrThrow()

    /**
     * Update the custom success message in the [SuccessContentRepository] if the request was successful.
     * If the request failed, update the custom success screen message to an error message
     * (accounts were linked successfully, but not saved to Link).
     */
    private fun Result<FinancialConnectionsSessionManifest>.updateCustomSuccessMessage(
        selectedAccounts: Int
    ): Result<FinancialConnectionsSessionManifest> =
        this.onSuccess {
            successContentRepository.set(
                customSuccessMessage = it.displayText?.successPane?.subCaption
                    // If backend returns a custom success message, use it
                    ?.let { TextResource.Text(it) }
                    // If not, build a Link success message locally
                    ?: TextResource.PluralId(
                        R.plurals.stripe_success_pane_desc_link_success,
                        selectedAccounts
                    )
            )
        }.onFailure {
            successContentRepository.set(
                customSuccessMessage = TextResource.PluralId(
                    R.plurals.stripe_success_pane_desc_link_error,
                    selectedAccounts
                )
            )
        }
}
