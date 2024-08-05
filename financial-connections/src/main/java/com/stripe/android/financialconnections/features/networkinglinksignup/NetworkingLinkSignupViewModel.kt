package com.stripe.android.financialconnections.features.networkinglinksignup

import android.webkit.URLUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.Click
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.NetworkingNewConsumer
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.NetworkingReturningConsumer
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.AttachConsumerToLinkAccountSession
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.GetOrFetchSync.RefetchCondition
import com.stripe.android.financialconnections.domain.LookupAccount
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.SaveAccountToLink
import com.stripe.android.financialconnections.domain.SignUpToLink
import com.stripe.android.financialconnections.features.common.getBusinessName
import com.stripe.android.financialconnections.features.common.isDataFlow
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.Legal
import com.stripe.android.financialconnections.features.notice.PresentSheet
import com.stripe.android.financialconnections.model.Bullet
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.LegalDetailsNotice
import com.stripe.android.financialconnections.model.LinkLoginPane
import com.stripe.android.financialconnections.model.NetworkingLinkSignupPane
import com.stripe.android.financialconnections.navigation.Destination.NetworkingLinkVerification
import com.stripe.android.financialconnections.navigation.Destination.NetworkingSaveToLinkVerification
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeState
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.utils.ConflatedJob
import com.stripe.android.financialconnections.utils.UriUtils
import com.stripe.android.financialconnections.utils.error
import com.stripe.android.financialconnections.utils.isCancellationError
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.InputController
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.SimpleTextFieldController
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import com.stripe.android.financialconnections.navigation.Destination.Success as SuccessDestination

internal class NetworkingLinkSignupViewModel @AssistedInject constructor(
    @Assisted initialState: NetworkingLinkSignupState,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val saveAccountToLink: SaveAccountToLink,
    private val lookupAccount: LookupAccount,
    private val uriUtils: UriUtils,
    private val getCachedAccounts: GetCachedAccounts,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val getOrFetchSync: GetOrFetchSync,
    private val navigationManager: NavigationManager,
    private val logger: Logger,
    private val presentSheet: PresentSheet,
    private val signUpToLink: SignUpToLink,
    private val attachConsumerToLinkAccountSession: AttachConsumerToLinkAccountSession,
) : FinancialConnectionsViewModel<NetworkingLinkSignupState>(initialState, nativeAuthFlowCoordinator) {

    private val pane: Pane by lazy {
        determinePane(initialState.isInstantDebits)
    }

    private var searchJob = ConflatedJob()

    init {
        observeAsyncs()
        suspend {
            val refetchCondition = if (initialState.isInstantDebits) {
                RefetchCondition.None
            } else {
                // Force synchronize to retrieve the networking signup pane content.
                RefetchCondition.Always
            }

            val sync = getOrFetchSync(refetchCondition)

            val content = sync.text?.let { text ->
                text.linkLoginPane?.toContent() ?: text.networkingLinkSignupPane?.toContent()
            }

            eventTracker.track(PaneLoaded(pane))

            NetworkingLinkSignupState.Payload(
                content = requireNotNull(content),
                merchantName = sync.manifest.getBusinessName(),
                emailController = SimpleTextFieldController(
                    textFieldConfig = EmailConfig(label = R.string.stripe_networking_signup_email_label),
                    initialValue = sync.manifest.accountholderCustomerEmailAddress,
                    showOptionalLabel = false
                ),
                phoneController = PhoneNumberController.createPhoneNumberController(
                    initialValue = sync.manifest.accountholderPhoneNumber ?: "",
                ),
                isInstantDebits = initialState.isInstantDebits,
            )
        }.execute { copy(payload = it) }
    }

    override fun updateTopAppBar(state: NetworkingLinkSignupState): TopAppBarStateUpdate {
        return TopAppBarStateUpdate(
            pane = determinePane(state.isInstantDebits),
            allowBackNavigation = state.isInstantDebits,
            error = state.payload.error,
        )
    }

    private fun observeAsyncs() {
        observePayloadResult()
        observeSaveAccountResult()
        observeLookupAccountResult()
    }

    private fun observeLookupAccountResult() {
        onAsync(
            NetworkingLinkSignupState::lookupAccount,
            onSuccess = { consumerSession ->
                if (consumerSession.exists) {
                    eventTracker.track(NetworkingReturningConsumer(pane))
                    navigateToLinkVerification()
                } else {
                    eventTracker.track(NetworkingNewConsumer(pane))
                }
            },
            onFail = { error ->
                eventTracker.logError(
                    extraMessage = "Error looking up account",
                    error = error,
                    logger = logger,
                    pane = pane
                )
            },
        )
    }

    private fun observeSaveAccountResult() {
        val isInstantDebits = stateFlow.value.isInstantDebits

        onAsync(
            NetworkingLinkSignupState::saveAccountToLink,
            onSuccess = { manifest ->
                val destination = if (isInstantDebits) {
                    manifest.nextPane.destination(referrer = pane)
                } else {
                    SuccessDestination(referrer = pane)
                }
                navigationManager.tryNavigateTo(destination)
            },
            onFail = { error ->
                eventTracker.logError(
                    extraMessage = if (isInstantDebits) {
                        "Error creating a Link account"
                    } else {
                        "Error saving account to Link"
                    },
                    error = error,
                    logger = logger,
                    pane = pane
                )

                if (isInstantDebits) {
                    // TODO(tillh-stripe) Display error message
                } else {
                    navigationManager.tryNavigateTo(SuccessDestination(referrer = pane))
                }
            },
        )
    }

    private fun observePayloadResult() {
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
                eventTracker.logError(
                    extraMessage = "Error fetching payload",
                    error = error,
                    logger = logger,
                    pane = pane
                )
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
        eventTracker.track(Click(eventName = "click.not_now", pane = pane))
        navigationManager.tryNavigateTo(SuccessDestination(referrer = pane))
    }

    fun onSaveAccount() {
        withState { state ->
            eventTracker.track(Click(eventName = "click.save_to_link", pane = pane))

            val hasExistingAccount = state.lookupAccount()?.exists == true
            if (hasExistingAccount) {
                navigateToLinkVerification()
            } else {
                saveNewAccount()
            }
        }
    }

    private fun saveNewAccount() {
        val isInstantDebits = stateFlow.value.isInstantDebits

        if (isInstantDebits) {
            saveNewAccountInInstantDebits()
        } else {
            saveNewAccountInFinancialConnections()
        }
    }

    private fun saveNewAccountInFinancialConnections() {
        suspend {
            eventTracker.track(Click(eventName = "click.save_to_link", pane = pane))
            val state = stateFlow.value
            val selectedAccounts = getCachedAccounts()
            val manifest = getOrFetchSync().manifest
            val phoneController = state.payload()!!.phoneController
            require(state.valid) { "Form invalid! ${state.validEmail} ${state.validPhone}" }
            saveAccountToLink.new(
                country = phoneController.getCountryCode(),
                email = state.validEmail!!,
                phoneNumber = phoneController.getE164PhoneNumber(state.validPhone!!),
                selectedAccounts = selectedAccounts,
                shouldPollAccountNumbers = manifest.isDataFlow,
            )
        }.execute { copy(saveAccountToLink = it) }
    }

    private fun saveNewAccountInInstantDebits() {
        suspend {
            val state = stateFlow.value

            if (!state.isInstantDebits) {
                // For the time being, we don't send events for this in the Instant Debits flow
                eventTracker.track(Click(eventName = "click.save_to_link", pane = pane))
            }

            val phoneController = state.payload()!!.phoneController

            val signup = signUpToLink(
                email = state.validEmail!!,
                phoneNumber = phoneController.getE164PhoneNumber(state.validPhone!!),
                country = stateFlow.value.payload()!!.phoneController.getCountryCode(),
            )

            attachConsumerToLinkAccountSession(
                consumerSessionClientSecret = signup.consumerSession.clientSecret,
            )

            val manifest = getOrFetchSync(refetchCondition = RefetchCondition.Always).manifest
            navigationManager.tryNavigateTo(manifest.nextPane.destination(referrer = pane))

            manifest
        }.execute { copy(saveAccountToLink = it) }
    }

    private fun navigateToLinkVerification() {
        val isInstantDebits = stateFlow.value.isInstantDebits

        val destination = if (isInstantDebits) {
            NetworkingLinkVerification(referrer = pane)
        } else {
            NetworkingSaveToLinkVerification(referrer = pane)
        }

        navigationManager.tryNavigateTo(destination)
    }

    fun onClickableTextClick(uri: String) = viewModelScope.launch {
        // if clicked uri contains an eventName query param, track click event.
        uriUtils.getQueryParameter(uri, "eventName")?.let { eventName ->
            eventTracker.track(Click(eventName, pane = pane))
        }
        val date = Date()
        if (URLUtil.isNetworkUrl(uri)) {
            setState { copy(viewEffect = OpenUrl(uri, date.time)) }
        } else {
            val managedUri = NetworkingLinkSignupClickableText.entries
                .firstOrNull { uriUtils.compareSchemeAuthorityAndPath(it.value, uri) }
            when (managedUri) {
                NetworkingLinkSignupClickableText.LEGAL_DETAILS -> {
                    presentLegalDetailsBottomSheet()
                }

                null -> logger.error("Unrecognized clickable text: $uri")
            }
        }
    }

    private fun presentLegalDetailsBottomSheet() {
        val notice = stateFlow.value.payload()?.content?.legalDetailsNotice ?: return
        presentSheet(
            content = Legal(notice),
            referrer = pane,
        )
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

    private fun determinePane(isInstantDebits: Boolean): Pane {
        return if (isInstantDebits) {
            Pane.LINK_LOGIN
        } else {
            Pane.NETWORKING_LINK_SIGNUP_PANE
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(initialState: NetworkingLinkSignupState): NetworkingLinkSignupViewModel
    }

    companion object {

        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val parentState = parentComponent.viewModel.stateFlow.value
                    val state = NetworkingLinkSignupState(parentState)
                    parentComponent.networkingLinkSignupViewModelFactory.create(state)
                }
            }

        private const val SEARCH_DEBOUNCE_MS = 1000L
        private const val SEARCH_DEBOUNCE_FINISHED_EMAIL_MS = 300L
    }
}

internal data class NetworkingLinkSignupState(
    val payload: Async<Payload> = Uninitialized,
    val validEmail: String? = null,
    val validPhone: String? = null,
    val saveAccountToLink: Async<FinancialConnectionsSessionManifest> = Uninitialized,
    val lookupAccount: Async<ConsumerSessionLookup> = Uninitialized,
    val viewEffect: ViewEffect? = null,
    val isInstantDebits: Boolean = false,
) {

    constructor(parentState: FinancialConnectionsSheetNativeState) : this(
        isInstantDebits = parentState.isLinkWithStripe,
    )

    val showFullForm: Boolean
        get() = lookupAccount()?.let { !it.exists } ?: false

    val valid: Boolean
        get() {
            val hasExistingAccount = lookupAccount()?.exists == true
            return validEmail != null && (hasExistingAccount || validPhone != null)
        }

    data class Payload(
        val merchantName: String?,
        val emailController: SimpleTextFieldController,
        val phoneController: PhoneNumberController,
        val isInstantDebits: Boolean,
        val content: Content,
    ) {

        val focusEmailField: Boolean
            get() = isInstantDebits && emailController.initialValue.isNullOrBlank()
    }

    data class Content(
        val title: String,
        val message: String?,
        val bullets: List<Bullet>,
        val aboveCta: String,
        val cta: String,
        val skipCta: String?,
        val legalDetailsNotice: LegalDetailsNotice?,
    )

    sealed class ViewEffect {
        data class OpenUrl(
            val url: String,
            val id: Long
        ) : ViewEffect()
    }
}

private enum class NetworkingLinkSignupClickableText(val value: String) {
    LEGAL_DETAILS("stripe://legal-details-notice"),
}

internal fun NetworkingLinkSignupPane.toContent(): NetworkingLinkSignupState.Content {
    return NetworkingLinkSignupState.Content(
        title = title,
        message = null,
        bullets = body.bullets,
        aboveCta = aboveCta,
        cta = cta,
        skipCta = skipCta,
        legalDetailsNotice = legalDetailsNotice,
    )
}

internal fun LinkLoginPane.toContent(): NetworkingLinkSignupState.Content {
    return NetworkingLinkSignupState.Content(
        title = title,
        message = body,
        bullets = emptyList(),
        aboveCta = aboveCta,
        cta = cta,
        skipCta = null,
        legalDetailsNotice = null,
    )
}
