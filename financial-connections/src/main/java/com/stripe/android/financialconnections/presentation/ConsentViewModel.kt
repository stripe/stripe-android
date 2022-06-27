package com.stripe.android.financialconnections.presentation

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.di.financialConnectionsSubComponentBuilderProvider
import com.stripe.android.financialconnections.domain.AcceptConsent
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.RequestNextStep
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.UpdateManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.ui.TextResource
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ConsentViewModel @Inject constructor(
    initialState: ConsentState,
    private val acceptConsent: AcceptConsent,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val logger: Logger
) : MavericksViewModel<ConsentState>(initialState) {

    fun onContinueClick() {
        viewModelScope.launch {
            val manifest: FinancialConnectionsSessionManifest = acceptConsent()
            with(nativeAuthFlowCoordinator()) {
                emit(UpdateManifest(manifest))
                emit(RequestNextStep(currentStep = NavigationDirections.consent))
            }
        }
    }

    fun onClickableTextClick(tag: String) {
        logger.debug("$tag clicked")
        setState {
            when (tag) {
                "terms" -> copy(
                    viewEffect = ConsentState.ViewEffect.OpenUrl("http://www.google.com")
                )
                "privacy" -> copy(
                    viewEffect = ConsentState.ViewEffect.OpenUrl("http://www.google.com")
                )
                "disconnect" -> copy(
                    viewEffect = ConsentState.ViewEffect.OpenUrl("http://www.google.com")
                )
                "data" -> copy(
                    bottomSheetType = ConsentState.BottomSheetType.DATA
                )
                "more" -> copy(
                    viewEffect = ConsentState.ViewEffect.OpenUrl("http://www.google.com")
                )
                "data_access" -> copy(
                    viewEffect = ConsentState.ViewEffect.OpenUrl("http://www.google.com")
                )
                else -> TODO("Unrecognized")
            }
        }
    }

    fun onConfirmModalClick() {
        setState {
            copy(
                bottomSheetType = ConsentState.BottomSheetType.NONE
            )
        }
    }

    fun onManifestChanged(manifest: FinancialConnectionsSessionManifest) {
        setState {
            copy(
                title = TextResource.StringId(
                    R.string.stripe_consent_pane_title,
                    listOf(requireNotNull(manifest.businessName))
                ),
                bullets = listOf(
                    R.drawable.stripe_ic_safe to TextResource.StringId(
                        R.string.stripe_consent_pane_body1,
                        listOf(requireNotNull(manifest.businessName))
                    ),
                    R.drawable.stripe_ic_shield to TextResource.StringId(
                        R.string.stripe_consent_pane_body2
                    ),
                    R.drawable.stripe_ic_lock to TextResource.StringId(
                        R.string.stripe_consent_pane_body3
                    ),
                ),
                requestedDataTitle = TextResource.StringId(
                    R.string.stripe_consent_requested_data_title,
                    listOf(requireNotNull(manifest.businessName))
                ),
                requestedDataBullets = manifest.permissions.mapNotNull { permission ->
                    when (permission) {
                        FinancialConnectionsAccount.Permissions.BALANCES ->
                            Pair(
                                TextResource.StringId(R.string.stripe_consent_requested_data_balances_title),
                                TextResource.StringId(R.string.stripe_consent_requested_data_balances_desc)
                            )
                        FinancialConnectionsAccount.Permissions.OWNERSHIP ->
                            Pair(
                                TextResource.StringId(R.string.stripe_consent_requested_data_ownership_title),
                                TextResource.StringId(R.string.stripe_consent_requested_data_ownership_desc)
                            )
                        FinancialConnectionsAccount.Permissions.PAYMENT_METHOD ->
                            Pair(
                                TextResource.StringId(R.string.stripe_consent_requested_data_accountdetails_title),
                                TextResource.StringId(R.string.stripe_consent_requested_data_accountdetails_desc)
                            )
                        FinancialConnectionsAccount.Permissions.TRANSACTIONS ->
                            Pair(
                                TextResource.StringId(R.string.stripe_consent_requested_data_transactions_title),
                                TextResource.StringId(R.string.stripe_consent_requested_data_transactions_desc)
                            )
                        FinancialConnectionsAccount.Permissions.UNKNOWN -> null
                    }
                }
            )
        }
    }

    fun onModalBottomSheetClosed() {
        setState {
            copy(
                bottomSheetType = ConsentState.BottomSheetType.NONE
            )
        }
    }

    fun onViewEffectLaunched() {
        setState {
            copy(
                viewEffect = null
            )
        }
    }

    companion object : MavericksViewModelFactory<ConsentViewModel, ConsentState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: ConsentState
        ): ConsentViewModel {
            return viewModelContext.financialConnectionsSubComponentBuilderProvider
                .consentSubComponentBuilder.get()
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

internal data class ConsentState(
    val title: TextResource = TextResource.Text(""),
    val bullets: List<Pair<Int, TextResource>> = emptyList(),
    val requestedDataTitle: TextResource = TextResource.Text(""),
    val requestedDataBullets: List<Pair<TextResource, TextResource>> = emptyList(),
    val bottomSheetType: BottomSheetType = BottomSheetType.NONE,
    val viewEffect: ViewEffect? = null
) : MavericksState {

    enum class BottomSheetType {
        NONE, DATA
    }

    sealed interface ViewEffect {
        data class OpenUrl(val url: String) : ViewEffect
    }
}
