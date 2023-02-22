package com.stripe.android.financialconnections.features.networkinglinksignup

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Error
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.PaneLoaded
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.domain.LookupAccount
import com.stripe.android.financialconnections.domain.SaveAccountToLink
import com.stripe.android.financialconnections.features.consent.ConsentTextBuilder
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.utils.ConflatedJob
import com.stripe.android.financialconnections.utils.isCancellationError
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.InputController
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.SimpleTextFieldController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class NetworkingLinkSignupViewModel @Inject constructor(
    initialState: NetworkingLinkSignupState,
    private val saveAccountToLink: SaveAccountToLink,
    private val lookupAccount: LookupAccount,
    private val getCachedAccounts: GetCachedAccounts,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val getManifest: GetManifest,
    private val goNext: GoNext,
    private val logger: Logger
) : MavericksViewModel<NetworkingLinkSignupState>(initialState) {

    private var searchJob = ConflatedJob()

    init {
        logErrors()
        suspend {
            val manifest = getManifest()
            eventTracker.track(PaneLoaded(Pane.NETWORKING_LINK_SIGNUP_PANE))
            NetworkingLinkSignupState.Payload(
                merchantName = ConsentTextBuilder.getBusinessName(manifest),
                emailController = EmailConfig.createController(manifest.accountholderCustomerEmailAddress),
                phoneController = PhoneNumberController.createPhoneNumberController()
            )
        }.execute { copy(payload = it) }
    }

    private fun logErrors() {
        onAsync(
            NetworkingLinkSignupState::payload,
            onSuccess = { payload ->
                // Observe controllers and set current form in state.
                viewModelScope.launch {
                    payload.emailController
                        .validFormFieldState()
                        .collectLatest {
                            onEmailEntered(it)
                        }
                }
                viewModelScope.launch {
                    payload.phoneController.validFormFieldState().collectLatest {
                        setState { copy(validPhone = it) }
                    }
                }
            },
            onFail = { error ->
                logger.error("Error fetching payload", error)
                eventTracker.track(Error(Pane.NETWORKING_LINK_SIGNUP_PANE, error))
            },
        )
        onAsync(
            NetworkingLinkSignupState::saveAccountToLink,
            onFail = { error ->
                logger.error("Error saving account to Link", error)
                eventTracker.track(Error(Pane.NETWORKING_LINK_SIGNUP_PANE, error))
            },
        )
        onAsync(
            NetworkingLinkSignupState::lookupAccount,
            onSuccess = {
                if (it.exists) goNext(Pane.NETWORKING_SAVE_TO_LINK_VERIFICATION)
            },
            onFail = { error ->
                logger.error("Error looking up account", error)
                eventTracker.track(Error(Pane.NETWORKING_LINK_SIGNUP_PANE, error))
            },
        )
    }

    /**
     * @param validEmail valid email, or null if entered email is invalid.
     */
    private suspend fun onEmailEntered(
        validEmail: String?
    ) {
        setState { copy(validEmail = validEmail) }
        if (validEmail != null) {
            logger.debug("VALID EMAIL ADDRESS $validEmail.")
            searchJob += suspend {
                delay(SEARCH_DEBOUNCE_MS)
                lookupAccount(validEmail)
            }.execute { copy(lookupAccount = if (it.isCancellationError()) Uninitialized else it) }
        } else {
            setState { copy(lookupAccount = Uninitialized) }
        }
    }

    fun onSaveAccount() {
        suspend {
            val state = awaitState()
            val selectedAccounts = getCachedAccounts()
            val phoneController = state.payload()!!.phoneController
            require(state.valid()) { "Form invalid! ${state.validEmail} ${state.validPhone}" }
            saveAccountToLink.new(
                country = phoneController.getCountryCode(),
                email = state.validEmail!!,
                phoneNumber = phoneController.getE164PhoneNumber(state.validPhone!!),
                selectedAccounts = selectedAccounts.map { it.id },
            ).also {
                goNext(nextPane = Pane.SUCCESS)
            }
        }.execute { copy(saveAccountToLink = it) }
    }

    fun onClickableTextClick(text: String) {
        // TODO handle clicks.
        logger.debug("Clicked text: $text")
    }

    /**
     * Emits the entered [InputController.formFieldValue] if the form is valid, null otherwise.
     */
    private fun InputController.validFormFieldState() =
        formFieldValue
            .map { it.takeIf { it.isComplete }?.value }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    companion object :
        MavericksViewModelFactory<NetworkingLinkSignupViewModel, NetworkingLinkSignupState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: NetworkingLinkSignupState
        ): NetworkingLinkSignupViewModel {
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .networkingLinkSignupSubcomponent
                .initialState(state)
                .build()
                .viewModel
        }

        private const val SEARCH_DEBOUNCE_MS = 300L
    }
}

internal data class NetworkingLinkSignupState(
    val payload: Async<Payload> = Uninitialized,
    val validEmail: String? = null,
    val validPhone: String? = null,
    val saveAccountToLink: Async<FinancialConnectionsSessionManifest> = Uninitialized,
    val lookupAccount: Async<ConsumerSessionLookup> = Uninitialized,
) : MavericksState {

    val showFullForm: Boolean
        get() = lookupAccount()?.let { !it.exists } ?: false

    fun valid(): Boolean {
        return validEmail != null && validPhone != null
    }

    data class Payload(
        val merchantName: String?,
        val emailController: SimpleTextFieldController,
        val phoneController: PhoneNumberController
    )
}
