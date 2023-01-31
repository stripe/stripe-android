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
import com.stripe.android.financialconnections.domain.SaveAccountToLink
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupState.Form
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.InputController
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.SimpleTextFieldController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

internal class NetworkingLinkSignupViewModel @Inject constructor(
    initialState: NetworkingLinkSignupState,
    private val saveAccountToLink: SaveAccountToLink,
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
                combine(
                    payload.emailController.validFormFieldState(),
                    payload.phoneController.validFormFieldState()
                ) { validEmail, validPhone ->
                    Form(
                        validEmail = validEmail,
                        validPhone = validPhone
                    )
                }.collect { setState { copy(form = it) } }
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
            val form = state.form
            require(form.valid()) { "Form invalid! $form" }
            saveAccountToLink(
                country = state.payload()!!.phoneController.getCountryCode(),
                email = form.validEmail!!,
                phoneNumber = form.validPhone!!,
                selectedAccounts = selectedAccounts.data.map { it.id },
            ).also {
                goNext(nextPane = it.nextPane)
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
    val saveAccountToLink: Async<FinancialConnectionsSessionManifest> = Uninitialized,
    val form: Form = Form()
) : MavericksState {

    data class Form(
        val validEmail: String? = null,
        val validPhone: String? = null
    ) {
        fun valid(): Boolean {
            return validEmail != null && validPhone != null
        }
    }

    data class Payload(
        val emailController: SimpleTextFieldController,
        val phoneController: PhoneNumberController
    )
}
