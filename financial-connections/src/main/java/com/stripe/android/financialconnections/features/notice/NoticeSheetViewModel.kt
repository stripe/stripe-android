package com.stripe.android.financialconnections.features.notice

import android.os.Bundle
import android.os.Parcelable
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
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.repository.NoticeSheetContentRepository
import com.stripe.android.financialconnections.ui.HandleClickableUrl
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
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
        observeErrors()
        loadNoticeSheetContent()
    }

    private fun observeErrors() {
        onAsync(
            prop = NoticeSheetState::content,
            onFail = { navigationManager.tryNavigateBack() },
        )
    }

    private fun loadNoticeSheetContent() {
        suspend {
            val state = noticeSheetContentRepository.await()
            requireNotNull(state.content)
        }.execute {
            copy(content = it)
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
    val content: Async<NoticeSheetContent> = Uninitialized,
    val viewEffect: ViewEffect? = null,
) {

    constructor(arguments: Bundle?) : this(
        pane = Destination.referrer(arguments)!!,
    )

    internal sealed interface NoticeSheetContent : Parcelable {

        @Parcelize
        data class Legal(
            val legalDetails: LegalDetailsNotice,
        ) : NoticeSheetContent

        @Parcelize
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
