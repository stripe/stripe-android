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
import com.stripe.android.financialconnections.domain.GetAuthorizationSessionAccounts
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.domain.LookupAccount
import com.stripe.android.financialconnections.domain.SaveAccountToLink
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupState.SignUpState.*
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
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
    private val getAuthorizationSessionAccounts: GetAuthorizationSessionAccounts,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val getManifest: GetManifest,
    private val goNext: GoNext,
    private val logger: Logger
) : MavericksViewModel<NetworkingLinkSignupState>(initialState) {

    init {
        logErrors()
        suspend {
            val manifest = getManifest()
            eventTracker.track(PaneLoaded(Pane.NETWORKING_LINK_SIGNUP_PANE))
            NetworkingLinkSignupState.Payload(
                emailController = EmailConfig.createController(manifest.accountholderCustomerEmailAddress),
                phoneController = PhoneNumberController.createPhoneNumberController(
                    initialValue = "",
                    initiallySelectedCountryCode = null,
                )
            )
        }.execute { copy(payload = it) }
    }

    private fun logErrors() {
        onAsync(
            NetworkingLinkSignupState::payload,
            onSuccess = { payload ->
                // Observe controllers and set current form in state.
                viewModelScope.launch {
                    payload.emailController.validFormFieldState().collectLatest { validEmail ->
                        setState { copy(validEmail = validEmail) }
                        if (validEmail != null) {
                            setState { copy(signupState = VerifyingEmail) }
                            delay(2000) // API call looking up for email
                            setState { copy(signupState = InputtingPhone) }
                        } else {
                            setState { copy(signupState = InputtingEmail) }
                        }
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
    }


    fun onSaveAccount() {
        suspend {
            val state = awaitState()
            val authSessionId = getManifest().activeAuthSession!!.id
            val selectedAccounts = getAuthorizationSessionAccounts(authSessionId)
            val phoneController = state.payload()!!.phoneController
            require(state.valid()) { "Form invalid! ${state.validEmail} ${state.validPhone}" }
            saveAccountToLink(
                country = phoneController.getCountryCode(),
                email = state.validEmail!!,
                phoneNumber = phoneController.getE164PhoneNumber(state.validPhone!!),
                selectedAccounts = selectedAccounts.data.map { it.id },
            ).also {
                goNext(nextPane = Pane.SUCCESS)
            }
        }.execute { copy(saveAccountToLink = it) }
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
    }
}

internal data class NetworkingLinkSignupState(
    val payload: Async<Payload> = Uninitialized,
    val validEmail: String? = null,
    val validPhone: String? = null,
    val saveAccountToLink: Async<FinancialConnectionsSessionManifest> = Uninitialized,
    val signupState: SignUpState = InputtingEmail,
) : MavericksState {

    fun valid(): Boolean {
        return validEmail != null && validPhone != null
    }

    /**
     * Enum representing the state of the Sign Up screen.
     */
    internal enum class SignUpState {
        InputtingEmail,
        VerifyingEmail,
        InputtingPhone
    }

    data class Payload(
        val emailController: SimpleTextFieldController,
        val phoneController: PhoneNumberController
    )
}
