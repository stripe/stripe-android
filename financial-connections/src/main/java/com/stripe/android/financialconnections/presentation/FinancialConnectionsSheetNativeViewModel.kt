package com.stripe.android.financialconnections.presentation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.di.DaggerFinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.CompleteFinancialConnectionsSession
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message
import com.stripe.android.financialconnections.exception.WebAuthFlowCancelledException
import com.stripe.android.financialconnections.exception.WebAuthFlowFailedException
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Canceled
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Completed
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Failed
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetNativeActivityArgs
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.NextPane
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewEffect.Finish
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewEffect.OpenUrl
import com.stripe.android.financialconnections.utils.UriComparator
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
internal class FinancialConnectionsSheetNativeViewModel @Inject constructor(
    /**
     * Exposes parent dagger component (activity viewModel scoped so that it survives config changes)
     * No other dependencies should be exposed from the viewModel
     */
    val activityRetainedComponent: FinancialConnectionsSheetNativeComponent,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val getManifest: GetManifest,
    private val uriComparator: UriComparator,
    private val completeFinancialConnectionsSession: CompleteFinancialConnectionsSession,
    private val logger: Logger,
    initialState: FinancialConnectionsSheetNativeState
) : MavericksViewModel<FinancialConnectionsSheetNativeState>(initialState) {

    init {
        viewModelScope.launch {
            nativeAuthFlowCoordinator().collect { message ->
                when (message) {
                    Message.OpenPartnerWebAuth -> {
                        val manifest = getManifest()
                        setState { copy(viewEffect = OpenUrl(manifest.hostedAuthUrl)) }
                    }

                    is Message.Finish -> {
                        setState { copy(viewEffect = Finish(message.result)) }
                    }

                    Message.ClearPartnerWebAuth -> {
                        setState { copy(webAuthFlow = Uninitialized) }
                    }
                }
            }
        }
    }

    /**
     * When authorization flow finishes, it will redirect to a URL scheme stripe-auth://link-accounts
     * captured by [com.stripe.android.financialconnections.FinancialConnectionsSheetRedirectActivity]
     * that will launch this activity in `singleTask` mode.
     *
     * @param intent the new intent with the redirect URL in the intent data
     */
    fun handleOnNewIntent(intent: Intent?) {
        viewModelScope.launch {
            val receivedUrl: String? = intent?.data?.toString()
            when {
                receivedUrl == null -> setState {
                    copy(webAuthFlow = Fail(WebAuthFlowFailedException(url = null)))
                }

                uriComparator.compareSchemeAuthorityAndPath(receivedUrl, SUCCESS_URL) -> setState {
                    copy(webAuthFlow = Success(receivedUrl))
                }

                else -> setState {
                    copy(webAuthFlow = Fail(WebAuthFlowFailedException(receivedUrl)))
                }
            }
        }
    }

    /**
     *  If activity resumes and we did not receive a callback from the AuthFlow,
     *  then the user hit the back button or closed the custom tabs UI, so return result as
     *  canceled.
     */
    fun onResume() {
        setState {
            if (webAuthFlow is Loading) {
                copy(webAuthFlow = Fail(WebAuthFlowCancelledException()))
            } else {
                this
            }
        }
    }

    fun openPartnerAuthFlowInBrowser(url: String) {
        setState {
            copy(
                webAuthFlow = Loading(),
                viewEffect = OpenUrl(url)
            )
        }
    }

    fun onViewEffectLaunched() {
        setState { copy(viewEffect = null) }
    }

    fun onCloseWithConfirmationClick() = setState { copy(showCloseDialog = true) }

    fun onCloseNoConfirmationClick() = closeAuthFlow(closeAuthFlowError = null)

    fun onCloseFromErrorClick(error: Throwable) = closeAuthFlow(closeAuthFlowError = error)

    fun onCloseConfirm() = closeAuthFlow(closeAuthFlowError = null)

    fun onCloseDismiss() = setState { copy(showCloseDialog = false) }

    /**
     * [NavHost] handles back presses except for when backstack is empty, where it delegates
     * to the container activity. [onBackPressed] will be triggered on these empty backstack cases.
     */
    fun onBackPressed() = closeAuthFlow(closeAuthFlowError = null)

    /**
     * There's at least three types of close cases:
     * 1. User closes when getting an error. In that case `error != nil`. That's an error.
     * 2. User closes, there is no error,
     *    and fetching accounts returns accounts (or `paymentAccount`). That's a success.
     * 3. User closes, there is no error,
     *    and fetching accounts returns NO accounts. That's a cancel.
     */
    private fun closeAuthFlow(
        closeAuthFlowError: Throwable? = null
    ) {
        viewModelScope.launch {
            kotlin
                .runCatching { completeFinancialConnectionsSession() }
                .onSuccess { session ->
                    when {
                        closeAuthFlowError != null ->
                            setState { copy(viewEffect = Finish(Failed(closeAuthFlowError))) }
                        else -> when {
                            session.accounts.data.isNotEmpty() ||
                                session.paymentAccount != null ||
                                session.bankAccountToken != null -> {
                                val result = Completed(
                                    financialConnectionsSession = session,
                                    token = session.parsedToken
                                )
                                setState { copy(viewEffect = Finish(result)) }
                            }
                            else -> {
                                // TODO@carlosmuvi handle terminal errors
                                setState { copy(viewEffect = Finish(Canceled)) }
                            }
                        }
                    }
                }
                .onFailure { completeSessionError ->
                    logger.error("Error completing session before closing", completeSessionError)
                    setState {
                        copy(
                            viewEffect = Finish(
                                Failed(closeAuthFlowError ?: completeSessionError)
                            )
                        )
                    }
                }
        }
    }

    companion object :
        MavericksViewModelFactory<FinancialConnectionsSheetNativeViewModel, FinancialConnectionsSheetNativeState> {

        // TODO@carlosmuvi: temporary redirect url for native.
        private const val SUCCESS_URL = "stripe-auth://link-accounts/login"

        override fun create(
            viewModelContext: ViewModelContext,
            state: FinancialConnectionsSheetNativeState
        ): FinancialConnectionsSheetNativeViewModel {
            val args = viewModelContext.args<FinancialConnectionsSheetNativeActivityArgs>()
            return DaggerFinancialConnectionsSheetNativeComponent
                .builder()
                .initialManifest(args.manifest)
                .application(viewModelContext.app())
                .configuration(state.configuration)
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

internal data class FinancialConnectionsSheetNativeState(
    val webAuthFlow: Async<String>,
    val configuration: FinancialConnectionsSheet.Configuration,
    val showCloseDialog: Boolean,
    val viewEffect: FinancialConnectionsSheetNativeViewEffect?,
    val initialPane: NextPane
) : MavericksState {

    /**
     * Used by Mavericks to build initial state based on args.
     */
    @Suppress("Unused")
    constructor(args: FinancialConnectionsSheetNativeActivityArgs) : this(
        webAuthFlow = Uninitialized,
        initialPane = args.manifest.nextPane,
        configuration = args.configuration,
        showCloseDialog = false,
        viewEffect = null
    )
}

@Composable
internal fun parentViewModel(): FinancialConnectionsSheetNativeViewModel =
    mavericksActivityViewModel()

internal sealed interface FinancialConnectionsSheetNativeViewEffect {
    /**
     * Open the Web AuthFlow.
     */
    data class OpenUrl(
        val url: String
    ) : FinancialConnectionsSheetNativeViewEffect

    /**
     * Finish the container activity.
     */
    data class Finish(
        val result: FinancialConnectionsSheetActivityResult
    ) : FinancialConnectionsSheetNativeViewEffect
}
