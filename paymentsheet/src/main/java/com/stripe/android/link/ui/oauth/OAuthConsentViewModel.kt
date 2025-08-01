package com.stripe.android.link.ui.oauth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.LinkAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

internal class OAuthConsentViewModel @Inject constructor(
    private val linkAccount: LinkAccount,
    private val linkConfiguration: LinkConfiguration,
    private val linkAccountManager: LinkAccountManager,
    private val logger: Logger,
) : ViewModel() {

    private val _viewState = MutableStateFlow(
        OAuthConsentViewState(
            merchantName = linkConfiguration.merchantName,
            merchantLogoUrl = linkConfiguration.merchantLogoUrl,
            // TODO
            consentPane = consentPanePreview.let {
                it.copy(
                    title = "Connect ${linkConfiguration.merchantName} with Link",
                    userSection = it.userSection.copy(
                        label = linkAccount.email
                    ),
                    scopesSection = it.scopesSection.copy(
                        header = "${linkConfiguration.merchantName} will have access to:"
                    ),
                )
            }
        )
    )
    val viewState: StateFlow<OAuthConsentViewState> = _viewState

    fun onAllowClick() {
        // TODO
    }

    fun onDenyClick() {
        // TODO
    }

    private fun updateViewState(block: (OAuthConsentViewState) -> OAuthConsentViewState) {
        _viewState.update(block)
    }

    companion object {
        fun factory(
            parentComponent: NativeLinkComponent,
            linkAccount: LinkAccount,
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    parentComponent.oAuthConsentViewModelComponentFactory
                        .build(linkAccount)
                        .viewModel
                }
            }
        }
    }
}

internal data class OAuthConsentViewState(
    val merchantName: String,
    val merchantLogoUrl: String? = null,
    val consentPane: ConsentPane? = null,
)
