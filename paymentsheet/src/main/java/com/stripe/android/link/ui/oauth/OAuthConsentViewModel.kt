package com.stripe.android.link.ui.oauth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.ConsentPresentation
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.ConsentUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class OAuthConsentViewModel @Inject constructor(
    private val linkAccount: LinkAccount,
    private val linkConfiguration: LinkConfiguration,
    private val linkAccountManager: LinkAccountManager,
    private val dismissWithResult: (LinkActivityResult) -> Unit,
) : ViewModel() {

    private val consentPane = (linkAccount.consentPresentation as? ConsentPresentation.FullScreen)?.consentPane

    private val _viewState = MutableStateFlow(
        OAuthConsentViewState(
            merchantName = linkConfiguration.merchantName,
            merchantLogoUrl = linkConfiguration.merchantLogoUrl,
            userEmail = linkAccount.email,
            consentPane = consentPane
        )
    )
    val viewState: StateFlow<OAuthConsentViewState> = _viewState

    init {
        if (consentPane == null) {
            // Shouldn't happen, if consent data is not available, we can assume it's not needed.
            dismissWithResult(LinkActivityResult.Completed(LinkAccountUpdate.Value(linkAccount)))
        }
    }

    fun onAllowClick() {
        onConsentSubmitted(consentGranted = true)
    }

    fun onDenyClick() {
        onConsentSubmitted(consentGranted = false)
    }

    private fun onConsentSubmitted(consentGranted: Boolean) {
        viewModelScope.launch {
            updateViewState { it.copy(errorMessage = null) }
            linkAccountManager.postConsentUpdate(consentGranted).fold(
                onSuccess = {
                    dismissWithResult(
                        LinkActivityResult.Completed(
                            linkAccountUpdate = LinkAccountUpdate.Value(linkAccount),
                            authorizationConsentGranted = consentGranted,
                        )
                    )
                },
                onFailure = { error ->
                    updateViewState { it.copy(errorMessage = error.stripeErrorMessage()) }
                }
            )
        }
    }

    private fun updateViewState(block: (OAuthConsentViewState) -> OAuthConsentViewState) {
        _viewState.update(block)
    }

    companion object {
        fun factory(
            parentComponent: NativeLinkComponent,
            linkAccount: LinkAccount,
            dismissWithResult: (LinkActivityResult) -> Unit,
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    parentComponent.oAuthConsentViewModelComponentFactory
                        .build(
                            linkAccount = linkAccount,
                            dismissWithResult = dismissWithResult,
                        )
                        .viewModel
                }
            }
        }
    }
}

internal data class OAuthConsentViewState(
    val merchantName: String,
    val userEmail: String,
    val merchantLogoUrl: String?,
    val consentPane: ConsentUi.ConsentPane?,
    val errorMessage: ResolvableString? = null,
)
