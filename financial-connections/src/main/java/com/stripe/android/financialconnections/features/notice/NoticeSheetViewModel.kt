package com.stripe.android.financialconnections.features.notice

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.model.DataAccessNotice
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.LegalDetailsNotice
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.repository.NoticeSheetContentRepository
import com.stripe.android.financialconnections.ui.HandleClickableUrl
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

internal class NoticeSheetViewModel @Inject constructor(
    initialState: NoticeSheetState,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val navigationManager: NavigationManager,
    private val noticeSheetContentRepository: NoticeSheetContentRepository,
    private val handleClickableUrl: HandleClickableUrl,
) : FinancialConnectionsViewModel<NoticeSheetState>(
    initialState = initialState,
    nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
) {

    init {
        viewModelScope.launch {
            val content = noticeSheetContentRepository.get()
            setState {
                copy(content = content.content)
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

    override fun onCleared() {
        noticeSheetContentRepository.update { copy(content = null) }
        super.onCleared()
    }

    companion object {

        fun factory(
            parentComponent: FinancialConnectionsSheetNativeComponent,
            arguments: Bundle?,
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    parentComponent
                        .noticeSheetSubcomponent
                        .create(NoticeSheetState(arguments))
                        .viewModel
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

    internal sealed interface NoticeSheetContent {

        data class Legal(
            val legalDetails: LegalDetailsNotice,
        ) : NoticeSheetContent

        data class DataAccess(
            val dataAccess: DataAccessNotice,
        ) : NoticeSheetContent
    }

    internal sealed interface ViewEffect {
        data class OpenUrl(
            val url: String,
            val id: Long,
        ) : ViewEffect
    }
}
