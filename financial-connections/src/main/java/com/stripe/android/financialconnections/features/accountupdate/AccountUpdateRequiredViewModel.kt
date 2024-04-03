package com.stripe.android.financialconnections.features.accountupdate

import android.os.Bundle
import android.os.Parcelable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.UpdateLocalManifest
import com.stripe.android.financialconnections.exception.UnclassifiedError
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.repository.AccountUpdateRequiredContentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

internal class AccountUpdateRequiredViewModel @AssistedInject constructor(
    @Assisted initialState: AccountUpdateRequiredState,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val updateRequiredContentRepository: AccountUpdateRequiredContentRepository,
    private val navigationManager: NavigationManager,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val updateLocalManifest: UpdateLocalManifest,
    private val logger: Logger,
) : FinancialConnectionsViewModel<AccountUpdateRequiredState>(
    initialState = initialState,
    nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
) {

    init {
        loadContent()
    }

    private fun loadContent() {
        suspend {
            requireNotNull(updateRequiredContentRepository.get()?.payload)
        }.execute {
            copy(payload = it)
        }
    }

    override fun updateTopAppBar(state: AccountUpdateRequiredState): TopAppBarStateUpdate? {
        return null
    }

    fun handleContinue() {
        viewModelScope.launch {
            val state = stateFlow.value
            val referrer = state.referrer
            val type = requireNotNull(state.payload()?.type)

            when (type) {
                is AccountUpdateRequiredState.Type.Repair -> {
                    handleUnsupportedRepairAction(referrer)
                }
                is AccountUpdateRequiredState.Type.PartnerAuth -> {
                    openPartnerAuth(type.institution, referrer)
                }
            }
        }
    }

    private fun handleUnsupportedRepairAction(referrer: Pane) {
        eventTracker.logError(
            extraMessage = "Updating a repair account, but repairs are not supported in Mobile.",
            error = UnclassifiedError("UpdateRepairAccountError"),
            logger = logger,
            pane = PANE,
        )
        // Fall back to the institution picker for now
        navigationManager.tryNavigateTo(Destination.InstitutionPicker(referrer))
    }

    private fun openPartnerAuth(
        institution: FinancialConnectionsInstitution?,
        referrer: Pane,
    ) {
        if (institution != null) {
            updateLocalManifest {
                it.copy(activeInstitution = institution)
            }
            navigationManager.tryNavigateTo(Destination.PartnerAuth(referrer))
        } else {
            // Fall back to the institution picker
            navigationManager.tryNavigateTo(Destination.InstitutionPicker(referrer))
        }
    }

    fun handleCancel() {
        navigationManager.tryNavigateBack()
    }

    override fun onCleared() {
        updateRequiredContentRepository.clear()
        super.onCleared()
    }

    @AssistedFactory
    interface Factory {
        fun create(initialState: AccountUpdateRequiredState): AccountUpdateRequiredViewModel
    }

    companion object {

        fun factory(
            parentComponent: FinancialConnectionsSheetNativeComponent,
            arguments: Bundle?,
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    parentComponent.accountUpdateRequiredViewModelFactory.create(AccountUpdateRequiredState(arguments))
                }
            }
        }

        internal val PANE = Pane.ACCOUNT_UPDATE_REQUIRED
    }
}

internal data class AccountUpdateRequiredState(
    val referrer: Pane,
    val payload: Async<Payload> = Uninitialized,
) {

    constructor(arguments: Bundle?) : this(
        referrer = Destination.referrer(arguments)!!,
    )

    @Parcelize
    data class Payload(
        val iconUrl: String? = null,
        val type: Type,
    ) : Parcelable

    sealed class Type(val pane: Pane) : Parcelable {

        @Parcelize
        data class Repair(val authorization: String?) : Type(Pane.BANK_AUTH_REPAIR)

        @Parcelize
        data class PartnerAuth(val institution: FinancialConnectionsInstitution?) : Type(Pane.PARTNER_AUTH)
    }
}
