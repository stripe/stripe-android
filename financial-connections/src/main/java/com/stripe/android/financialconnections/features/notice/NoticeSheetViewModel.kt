package com.stripe.android.financialconnections.features.notice

import FinancialConnectionsGenericInfoScreen
import android.os.Bundle
import android.os.Parcelable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.model.DataAccessNotice
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.LegalDetailsNotice
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.repository.NoticeSheetContentRepository
import com.stripe.android.financialconnections.ui.HandleClickableUrl
import com.stripe.android.uicore.navigation.NavigationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.util.Date

internal class NoticeSheetViewModel @AssistedInject constructor(
    @Assisted initialState: NoticeSheetState,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val navigationManager: NavigationManager,
    private val noticeSheetContentRepository: NoticeSheetContentRepository,
    private val handleClickableUrl: HandleClickableUrl,
) : FinancialConnectionsViewModel<NoticeSheetState>(
    initialState = initialState,
    nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
) {

    init {
        loadNoticeSheetContent()
    }

    private fun loadNoticeSheetContent() {
        viewModelScope.launch {
            val content = noticeSheetContentRepository.get()?.content
            if (content != null) {
                setState { copy(content = content) }
            } else {
                navigationManager.tryNavigateBack()
            }
        }
    }

    override fun updateTopAppBar(state: NoticeSheetState): TopAppBarStateUpdate? {
        return null
    }

    fun handleClickableTextClick(uri: String) {
        viewModelScope.launch {
            val date = Date()
            val pane = stateFlow.value.pane

            handleClickableUrl(
                currentPane = pane,
                uri = uri,
                onNetworkUrlClicked = {
                    setState {
                        copy(viewEffect = NoticeSheetState.ViewEffect.OpenUrl(uri, date.time))
                    }
                },
                knownDeeplinkActions = emptyMap(),
            )
        }
    }

    fun handleConfirmModalClick() {
        navigationManager.tryNavigateBack()
    }

    fun onViewEffectLaunched() {
        setState {
            copy(viewEffect = null)
        }
    }

    override fun onCleared() {
        noticeSheetContentRepository.clear()
        super.onCleared()
    }

    @AssistedFactory
    interface Factory {
        fun create(initialState: NoticeSheetState): NoticeSheetViewModel
    }

    companion object {

        fun factory(
            parentComponent: FinancialConnectionsSheetNativeComponent,
            arguments: Bundle?,
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    parentComponent.noticeSheetViewModelFactory.create(NoticeSheetState(arguments))
                }
            }
        }
    }
}

internal data class NoticeSheetState(
    val pane: Pane,
    val content: NoticeSheetContent? = null,
    val viewEffect: ViewEffect? = null,
) {

    constructor(arguments: Bundle?) : this(
        pane = Destination.referrer(arguments)!!,
    )

    internal sealed interface NoticeSheetContent : Parcelable {

        @Parcelize
        data class Generic(
            val generic: FinancialConnectionsGenericInfoScreen,
        ) : NoticeSheetContent

        @Parcelize
        data class Legal(
            val legalDetails: LegalDetailsNotice,
        ) : NoticeSheetContent

        @Parcelize
        data class DataAccess(
            val dataAccess: DataAccessNotice,
        ) : NoticeSheetContent

        @Parcelize
        data class UpdateRequired(
            val generic: FinancialConnectionsGenericInfoScreen,
            val type: Type,
        ) : NoticeSheetContent {
            sealed interface Type : Parcelable {

                @Parcelize
                data class Repair(
                    val authorization: String?,
                    val institution: FinancialConnectionsInstitution?,
                ) : Type

                @Parcelize
                data class Supportability(
                    val institution: FinancialConnectionsInstitution?,
                ) : Type
            }
        }
    }

    internal sealed interface ViewEffect {
        data class OpenUrl(
            val url: String,
            val id: Long,
        ) : ViewEffect
    }
}
