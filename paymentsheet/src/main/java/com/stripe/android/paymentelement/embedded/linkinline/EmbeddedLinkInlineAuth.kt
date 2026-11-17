package com.stripe.android.paymentelement.embedded.linkinline

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.common.analytics.experiment.LoggableExperiment
import com.stripe.android.common.di.MOBILE_SESSION_ID
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.LinkAuthCapabilities
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.verification.LinkAuthFlow
import com.stripe.android.link.ui.verification.LinkAuthFlowResult
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.embedded.InternalRowSelectionCallback
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineAuthController.CodeEntry
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineAuthEvent.DialogReason
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineVerificationEligibility.Result
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.utils.childScope
import com.stripe.android.uicore.elements.OTPElement
import com.stripe.android.uicore.elements.OTPElementFactory
import com.stripe.android.uicore.utils.flatMapLatestAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.attestation.IntegrityRequestManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.coroutines.EmptyCoroutineContext

/**
 * A returning Link customer's inline verification in the Embedded Payment Element, across `configure` calls.
 */
@Suppress("TooManyFunctions")
internal interface EmbeddedLinkInlineAuth {
    val listOptions: StateFlow<EmbeddedLinkInlineListOptions>

    /**
     * The card to show in place of the Link row, or null.
     */
    val card: StateFlow<EmbeddedLinkInlineCardUi?>

    /**
     * The Link dialog to show for the attempt, or null.
     */
    val dialog: StateFlow<EmbeddedLinkVerificationDialogSession?>

    /**
     * Whether the element still has to report the first time it's on screen.
     */
    val needsVisibility: StateFlow<Boolean>

    val isPresentingDialog: Boolean

    /**
     * Whether a sheet, the Link dialog or a confirmation is shown over the element.
     */
    fun isUiPresentedOverElement(): Boolean

    /**
     * Starts inline verification if the loaded session is eligible, or continues the current attempt.
     */
    fun onConfigured(
        paymentMethodMetadata: PaymentMethodMetadata,
        customer: CustomerState?,
        configuration: EmbeddedPaymentElement.Configuration,
    )

    /**
     * Link can't start out selected while the card shows.
     */
    fun initialSelection(selection: PaymentSelection?): PaymentSelection?

    /**
     * Whether a loading session takes part in inline verification, in any experiment group.
     */
    fun isInExperiment(
        paymentMethodMetadata: PaymentMethodMetadata,
        customer: CustomerState?,
        configuration: EmbeddedPaymentElement.Configuration,
    ): Boolean

    /**
     * The intent was confirmed, so the card never comes back.
     */
    fun onIntentConfirmed()

    fun onElementVisible()

    fun onCodeTapped()

    fun onClose()

    fun onSwitchAccount()

    fun onAction(action: EmbeddedLinkInlineCardState.Action)

    /**
     * Registers composed UI that can host the Link dialog until the returned handle is closed.
     *
     * @param isReady Whether the host can show the dialog now, i.e. its window is resumed and focused.
     */
    fun registerDialogHost(isReady: () -> Boolean): AutoCloseable
}

@Immutable
internal data class EmbeddedLinkInlineCardUi(
    val state: EmbeddedLinkInlineCardState,
    val otpElement: OTPElement,
    val linkBrand: LinkBrand,
)

@Suppress("TooManyFunctions", "LongParameterList")
@Singleton
internal class DefaultEmbeddedLinkInlineAuth @Inject constructor(
    @ViewModelScope private val viewModelScope: CoroutineScope,
    private val savedStateHandle: SavedStateHandle,
    private val linkAccountHolder: LinkAccountHolder,
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val eventReporter: EventReporter,
    private val sheetStateHolder: SheetStateHolder,
    private val confirmationHandler: ConfirmationHandler,
    private val integrityRequestManager: IntegrityRequestManager,
    private val internalRowSelectionCallback: Provider<InternalRowSelectionCallback?>,
    private val context: Context,
    @Named(MOBILE_SESSION_ID) private val mobileSessionId: String,
    private val registry: EmbeddedLinkInlineAuthRegistry,
) : EmbeddedLinkInlineAuth, EmbeddedLinkInlineAuthController.Host {

    private val controller = MutableStateFlow<EmbeddedLinkInlineAuthController?>(null)
    private var linkConfiguration: LinkConfiguration? = null
    private val dialogHosts = CopyOnWriteArrayList<() -> Boolean>()
    private var isFirstConfiguration = true
    private var hasConfirmedIntent = false

    private var placesLinkFirst: Boolean
        get() = savedStateHandle[PLACES_LINK_FIRST_KEY] ?: false
        set(value) {
            savedStateHandle[PLACES_LINK_FIRST_KEY] = value
        }

    override val listOptions: StateFlow<EmbeddedLinkInlineListOptions> =
        savedStateHandle.getStateFlow(PLACES_LINK_FIRST_KEY, false).mapAsStateFlow { placesLinkFirst ->
            EmbeddedLinkInlineListOptions(
                placesLinkFirst = placesLinkFirst,
                // A Link pick shows its details with "Change" only once Link leads the list.
                linkPickIsChangeable = placesLinkFirst,
            )
        }

    override val card: StateFlow<EmbeddedLinkInlineCardUi?> = controller.flatMapLatestAsStateFlow {
        it?.card ?: stateFlowOf(null)
    }

    override val dialog: StateFlow<EmbeddedLinkVerificationDialogSession?> = controller.flatMapLatestAsStateFlow {
        it?.dialog ?: stateFlowOf(null)
    }

    override val needsVisibility: StateFlow<Boolean> = controller.flatMapLatestAsStateFlow {
        it?.needsVisibility ?: stateFlowOf(false)
    }

    override val isPresentingDialog: Boolean
        get() = controller.value?.isPresentingDialog == true

    init {
        viewModelScope.launch {
            linkAccountHolder.linkAccountInfo.collect { update ->
                controller.value?.onAccountChanged(update.account)
            }
        }
    }

    override fun isUiPresentedOverElement(): Boolean {
        return sheetStateHolder.sheetIsOpen ||
            isPresentingDialog ||
            confirmationHandler.state.value is ConfirmationHandler.State.Confirming
    }

    override fun onConfigured(
        paymentMethodMetadata: PaymentMethodMetadata,
        customer: CustomerState?,
        configuration: EmbeddedPaymentElement.Configuration,
    ) {
        val isInitialLoad = isFirstConfiguration
        isFirstConfiguration = false
        if (hasConfirmedIntent) return
        val linkConfiguration = paymentMethodMetadata.linkState?.configuration
        controller.value?.let { current ->
            // The account is kept across configure, so only recheck what configure can change.
            val canContinue = current.phase.value != EmbeddedLinkInlineAuthController.Phase.Finished &&
                linkConfiguration?.enableEmbeddedInlineOtp == true &&
                paymentMethodMetadata.shouldShowLinkButton &&
                linkAccountHolder.linkAccountInfo.value.account?.email.equals(current.linkAccount.email, true)
            if (canContinue) {
                current.forcesDialog = linkConfiguration?.embeddedInlineOtpForcesDialog == true
                return
            }
            current.invalidate()
            controller.value = null
        }
        setUpIfEligible(
            paymentMethodMetadata = paymentMethodMetadata,
            customer = customer,
            configuration = configuration,
            logsIneligibility = isInitialLoad,
        )
    }

    override fun initialSelection(selection: PaymentSelection?): PaymentSelection? {
        val showsCard = controller.value?.isShowingCard == true
        return selection.takeUnless { showsCard && it is PaymentSelection.Link }
    }

    override fun isInExperiment(
        paymentMethodMetadata: PaymentMethodMetadata,
        customer: CustomerState?,
        configuration: EmbeddedPaymentElement.Configuration,
    ): Boolean {
        // Once a session is in the experiment, it stays in it for the element's lifetime.
        if (controller.value != null || placesLinkFirst) return true
        val linkConfiguration = paymentMethodMetadata.linkState?.configuration ?: return false
        val dryRunScope = viewModelScope.childScope(EmptyCoroutineContext)
        val result = evaluate(
            paymentMethodMetadata = paymentMethodMetadata,
            customer = customer,
            configuration = configuration,
            linkConfiguration = linkConfiguration,
            scope = dryRunScope,
            onFinish = {},
        )
        (result as? Result.Eligible)?.authFlow?.invalidate()
        dryRunScope.cancel()
        return result is Result.Eligible
    }

    override fun onIntentConfirmed() {
        hasConfirmedIntent = true
        controller.value?.invalidate()
        controller.value = null
    }

    override fun onElementVisible() {
        controller.value?.elementBecameVisible()
    }

    override fun onCodeTapped() {
        controller.value?.onCodeTapped()
    }

    override fun onClose() {
        controller.value?.onClose()
    }

    override fun onSwitchAccount() {
        controller.value?.onSwitchAccount()
    }

    override fun onAction(action: EmbeddedLinkInlineCardState.Action) {
        controller.value?.onAction(action)
    }

    override fun registerDialogHost(isReady: () -> Boolean): AutoCloseable {
        dialogHosts.add(isReady)
        return AutoCloseable {
            dialogHosts.remove(isReady)
            if (dialogHosts.isEmpty()) {
                // The dialog can't stay up without UI to host it.
                controller.value?.onDialogDismissed()
            }
        }
    }

    override fun canPresentDialog(): Boolean {
        return dialogHosts.any { it() } &&
            !sheetStateHolder.sheetIsOpen &&
            confirmationHandler.state.value !is ConfirmationHandler.State.Confirming
    }

    override fun onVerified(fromCard: Boolean) {
        // The Link account was updated in place by the verification. The Link row reflects it on its own.
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun logOut(account: LinkAccount) {
        val linkAccountManager = linkConfiguration?.let {
            linkConfigurationCoordinator.getComponent(it).linkAccountManager
        }
        linkAccountHolder.set(
            LinkAccountUpdate.Value(account = null, lastUpdateReason = LinkAccountUpdate.Value.UpdateReason.LoggedOut)
        )
        if (linkAccountManager != null) {
            GlobalScope.launch {
                // This usage is intentional. The request is sent without regard for the UI lifecycle.
                linkAccountManager.logOut(account)
            }
        }
    }

    private fun setUpIfEligible(
        paymentMethodMetadata: PaymentMethodMetadata,
        customer: CustomerState?,
        configuration: EmbeddedPaymentElement.Configuration,
        logsIneligibility: Boolean,
    ) {
        val linkConfiguration = paymentMethodMetadata.linkState?.configuration ?: return
        val controllerScope = viewModelScope.childScope(EmptyCoroutineContext)
        var newController: EmbeddedLinkInlineAuthController? = null
        val result = evaluate(
            paymentMethodMetadata = paymentMethodMetadata,
            customer = customer,
            configuration = configuration,
            linkConfiguration = linkConfiguration,
            scope = controllerScope,
            onFinish = { newController?.onAuthFlowFinished(it) },
        )
        val eligible = when (result) {
            is Result.Eligible -> result
            is Result.Ineligible -> {
                controllerScope.cancel()
                if (logsIneligibility && result.reason.isReported) {
                    eventReporter.onEmbeddedLinkInlineAuthEvent(EmbeddedLinkInlineAuthEvent.Ineligible(result.reason))
                }
                return
            }
        }
        val experiment = EmbeddedLinkInlineOtpExperiment.from(paymentMethodMetadata.experimentsData)
        newController = EmbeddedLinkInlineAuthController(
            linkAccount = eligible.linkAccount,
            authFlow = eligible.authFlow,
            showsCard = experiment.showsCard,
            exposure = exposure(
                experiment = experiment,
                paymentMethodMetadata = paymentMethodMetadata,
                customer = customer,
                configuration = configuration,
                authFlow = eligible.authFlow,
            ),
            paymentMethodMetadata = paymentMethodMetadata,
            forcesDialog = linkConfiguration.embeddedInlineOtpForcesDialog,
            linkBrand = paymentMethodMetadata.effectiveLinkBrand(eligible.linkAccount),
            host = this,
            eventReporter = eventReporter,
            registry = registry,
            registryOwner = this,
            requiresAttestation = linkConfigurationCoordinator.linkGate(linkConfiguration).useAttestationEndpoints,
            prepareAttestation = { integrityRequestManager.prepare().isSuccess },
            isScreenReaderOn = ::isScreenReaderOn,
            // Typing in the card needs keyboard handling, so codes are entered in the dialog for now.
            layoutCodeEntry = { CodeEntry.Dialog(DialogReason.NotInWindow) },
            cardOtpElement = OTPElementFactory.create(),
            scope = controllerScope,
        )
        if (newController.showsCard) {
            placesLinkFirst = true
        }
        this.linkConfiguration = linkConfiguration
        controller.value = newController
    }

    @Suppress("LongParameterList")
    private fun evaluate(
        paymentMethodMetadata: PaymentMethodMetadata,
        customer: CustomerState?,
        configuration: EmbeddedPaymentElement.Configuration,
        linkConfiguration: LinkConfiguration,
        scope: CoroutineScope,
        onFinish: (LinkAuthFlowResult) -> Unit,
    ): Result {
        val linkComponent = linkConfigurationCoordinator.getComponent(linkConfiguration)
        val linkAccountManager = linkComponent.linkAccountManager
        return EmbeddedLinkInlineVerificationEligibility.evaluate(
            inputs = EmbeddedLinkInlineVerificationEligibility.Inputs(
                isEnabledByServer = linkConfiguration.enableEmbeddedInlineOtp,
                isAllowedByMerchant = configuration.allowsLinkInlineVerification,
                isLinkDisplayed = paymentMethodMetadata.shouldShowLinkButton,
                linkAccount = linkAccountManager.linkAccountInfo.value.account,
                canUseNativeLink = linkConfigurationCoordinator.linkGate(linkConfiguration).useNativeLink,
                hasSavedPaymentMethods = !customer?.paymentMethods.isNullOrEmpty(),
            ),
            registry = registry,
            owner = this,
            prepareAuthFlow = {
                LinkAuthFlow(
                    linkAccountManager = linkAccountManager,
                    linkEventsReporter = linkComponent.linkAccountEventsReporter,
                    capabilities = LinkAuthCapabilities.supportedVerificationTypes(
                        enableMfaAuthFlow = linkConfiguration.enableMfaAuthFlow,
                    ),
                    consentGranted = { null },
                    scope = scope,
                    now = System::currentTimeMillis,
                    onFinish = onFinish,
                )
            },
        )
    }

    private fun exposure(
        experiment: EmbeddedLinkInlineOtpExperiment,
        paymentMethodMetadata: PaymentMethodMetadata,
        customer: CustomerState?,
        configuration: EmbeddedPaymentElement.Configuration,
        authFlow: LinkAuthFlow,
    ): LoggableExperiment? {
        val assigned = experiment as? EmbeddedLinkInlineOtpExperiment.Assigned ?: return null
        return LoggableExperiment.OcsMobileEmbeddedLinkInlineOtp(
            experimentsData = assigned.experimentsData,
            group = assigned.group.value,
            metadata = paymentMethodMetadata,
            mobileSessionId = mobileSessionId,
            mobileSdkVersion = StripeSdkVersion.VERSION_NAME,
            hasSavedPaymentMethod = !customer?.paymentMethods.isNullOrEmpty(),
            rowStyle = configuration.appearance.embeddedAppearance.style,
            formSheetAction = configuration.formSheetAction,
            isRowSelectionImmediateAction = internalRowSelectionCallback.get() != null,
            canVerifyWithEmail = authFlow.state.value.canSendToEmail,
            isLargeTextSize = context.resources.configuration.fontScale >= LARGE_TEXT_FONT_SCALE,
        )
    }

    private fun isScreenReaderOn(): Boolean {
        val accessibilityManager = context.getSystemService(AccessibilityManager::class.java)
        return accessibilityManager?.isTouchExplorationEnabled == true
    }

    private companion object {
        const val PLACES_LINK_FIRST_KEY = "EmbeddedLinkInlineAuth_PLACES_LINK_FIRST"

        /**
         * The first of the accessibility font sizes, like iOS's accessibility text sizes.
         */
        const val LARGE_TEXT_FONT_SCALE = 1.5f
    }
}

/**
 * For integrations that don't verify Link inline, e.g. Checkout Sessions.
 */
@Suppress("TooManyFunctions")
internal class DisabledEmbeddedLinkInlineAuth @Inject constructor() : EmbeddedLinkInlineAuth {
    override val listOptions: StateFlow<EmbeddedLinkInlineListOptions> =
        stateFlowOf(EmbeddedLinkInlineListOptions.Disabled)
    override val card: StateFlow<EmbeddedLinkInlineCardUi?> = stateFlowOf(null)
    override val dialog: StateFlow<EmbeddedLinkVerificationDialogSession?> = stateFlowOf(null)
    override val needsVisibility: StateFlow<Boolean> = stateFlowOf(false)
    override val isPresentingDialog: Boolean = false

    override fun isUiPresentedOverElement(): Boolean = false

    override fun onConfigured(
        paymentMethodMetadata: PaymentMethodMetadata,
        customer: CustomerState?,
        configuration: EmbeddedPaymentElement.Configuration,
    ) = Unit

    override fun initialSelection(selection: PaymentSelection?): PaymentSelection? = selection

    override fun isInExperiment(
        paymentMethodMetadata: PaymentMethodMetadata,
        customer: CustomerState?,
        configuration: EmbeddedPaymentElement.Configuration,
    ): Boolean = false

    override fun onIntentConfirmed() = Unit

    override fun onElementVisible() = Unit

    override fun onCodeTapped() = Unit

    override fun onClose() = Unit

    override fun onSwitchAccount() = Unit

    override fun onAction(action: EmbeddedLinkInlineCardState.Action) = Unit

    override fun registerDialogHost(isReady: () -> Boolean): AutoCloseable = AutoCloseable {}
}
