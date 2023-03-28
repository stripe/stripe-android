package com.stripe.android.financialconnections.features.networkinglinksignup

import android.webkit.URLUtil
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Click
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Error
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.NetworkingNewConsumer
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.NetworkingReturningConsumer
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.PaneLoaded
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.domain.LookupAccount
import com.stripe.android.financialconnections.domain.SaveAccountToLink
import com.stripe.android.financialconnections.domain.SynchronizeFinancialConnectionsSession
import com.stripe.android.financialconnections.features.common.getBusinessName
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.NetworkingLinkSignupPane
import com.stripe.android.financialconnections.repository.SaveToLinkWithStripeSucceededRepository
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.utils.ConflatedJob
import com.stripe.android.financialconnections.utils.UriUtils
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
import java.security.InvalidParameterException
import java.util.Date
import javax.inject.Inject

internal class NetworkingLinkSignupViewModel @Inject constructor(
    initialState: NetworkingLinkSignupState,
    private val saveToLinkWithStripeSucceeded: SaveToLinkWithStripeSucceededRepository,
    private val saveAccountToLink: SaveAccountToLink,
    private val lookupAccount: LookupAccount,
    private val uriUtils: UriUtils,
    private val getCachedAccounts: GetCachedAccounts,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val getManifest: GetManifest,
    private val sync: SynchronizeFinancialConnectionsSession,
    private val goNext: GoNext,
    private val logger: Logger
) : MavericksViewModel<NetworkingLinkSignupState>(initialState) {

    private var searchJob = ConflatedJob()

    init {
        logErrors()
        suspend {
            val manifest = getManifest()
            val content = requireNotNull(sync().text?.networkingLinkSignupPane)
            eventTracker.track(PaneLoaded(PANE))
            NetworkingLinkSignupState.Payload(
                content = content,
                merchantName = manifest.getBusinessName(),
                emailController = EmailConfig
                    .createController(manifest.accountholderCustomerEmailAddress),
                phoneController = PhoneNumberController
                    .createPhoneNumberController(manifest.accountholderPhoneNumber ?: ""),
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
                        .collectLatest(::onEmailEntered)
                }
                viewModelScope.launch {
                    payload.phoneController.validFormFieldState().collectLatest {
                        setState { copy(validPhone = it) }
                    }
                }
            },
            onFail = { error ->
                logger.error("Error fetching payload", error)
                eventTracker.track(Error(PANE, error))
            },
        )
        onAsync(
            NetworkingLinkSignupState::saveAccountToLink,
            onSuccess = {
                saveToLinkWithStripeSucceeded.set(true)
            },
            onFail = { error ->
                saveToLinkWithStripeSucceeded.set(false)
                logger.error("Error saving account to Link", error)
                eventTracker.track(Error(PANE, error))
                goNext(nextPane = Pane.SUCCESS)
            },
        )
        onAsync(
            NetworkingLinkSignupState::lookupAccount,
            onSuccess = { consumerSession ->
                if (consumerSession.exists) {
                    eventTracker.track(NetworkingReturningConsumer(PANE))
                    goNext(Pane.NETWORKING_SAVE_TO_LINK_VERIFICATION)
                } else {
                    eventTracker.track(NetworkingNewConsumer(PANE))
                }
            },
            onFail = { error ->
                logger.error("Error looking up account", error)
                eventTracker.track(Error(PANE, error))
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
                delay(getLookupDelayMs(validEmail))
                lookupAccount(validEmail)
            }.execute { copy(lookupAccount = if (it.isCancellationError()) Uninitialized else it) }
        } else {
            setState { copy(lookupAccount = Uninitialized) }
        }
    }

    /**
     * A valid e-mail will transition the user to the phone number field (sometimes prematurely),
     * so we increase debounce if there's a high chance the e-mail is not yet finished being typed
     * (high chance of not finishing == not .com suffix)
     *
     * @return delay in milliseconds
     */
    private fun getLookupDelayMs(validEmail: String) =
        if (validEmail.endsWith(".com")) SEARCH_DEBOUNCE_FINISHED_EMAIL_MS else SEARCH_DEBOUNCE_MS

    fun onSkipClick() = viewModelScope.launch {
        eventTracker.track(Click(eventName = "click.not_now", pane = PANE))
        goNext(Pane.SUCCESS)
    }

    fun onSaveAccount() {
        suspend {
            eventTracker.track(Click(eventName = "click.save_to_link", pane = PANE))
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

    fun onClickableTextClick(uri: String) = viewModelScope.launch {
        // if clicked uri contains an eventName query param, track click event.
        uriUtils.getQueryParameter(uri, "eventName")?.let { eventName ->
            eventTracker.track(Click(eventName, pane = PANE))
        }
        val date = Date()
        if (URLUtil.isNetworkUrl(uri)) {
            setState { copy(viewEffect = OpenUrl(uri, date.time)) }
        } else {
            val errorMessage = "Unrecognized clickable text: $uri"
            logger.error(errorMessage)
            eventTracker.track(Error(PANE, InvalidParameterException(errorMessage)))
        }
    }

    /**
     * Emits the entered [InputController.formFieldValue] if the form is valid, null otherwise.
     */
    private fun InputController.validFormFieldState() =
        formFieldValue
            .map { it.takeIf { it.isComplete }?.value }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun onViewEffectLaunched() {
        setState { copy(viewEffect = null) }
    }

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

        private const val SEARCH_DEBOUNCE_MS = 1000L
        private const val SEARCH_DEBOUNCE_FINISHED_EMAIL_MS = 300L
        internal val PANE = Pane.NETWORKING_LINK_SIGNUP_PANE
    }
}

internal data class NetworkingLinkSignupState(
    val payload: Async<Payload> = Uninitialized,
    val validEmail: String? = null,
    val validPhone: String? = null,
    val saveAccountToLink: Async<FinancialConnectionsSessionManifest> = Uninitialized,
    val lookupAccount: Async<ConsumerSessionLookup> = Uninitialized,
    val viewEffect: ViewEffect? = null
) : MavericksState {

    val showFullForm: Boolean
        get() = lookupAccount()?.let { !it.exists } ?: false

    fun valid(): Boolean {
        return validEmail != null && validPhone != null
    }

    data class Payload(
        val merchantName: String?,
        val emailController: SimpleTextFieldController,
        val phoneController: PhoneNumberController,
        val content: NetworkingLinkSignupPane
    )

    sealed class ViewEffect {
        data class OpenUrl(
            val url: String,
            val id: Long
        ) : ViewEffect()
    }
}
