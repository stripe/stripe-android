package com.stripe.android.link

import android.app.Application
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.flatMapCatching
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.LinkAuthResult
import com.stripe.android.link.account.toLinkAuthResult
import com.stripe.android.link.attestation.LinkAttestationCheck
import com.stripe.android.link.confirmation.computeExpectedPaymentMethodType
import com.stripe.android.link.exceptions.AppAttestationException
import com.stripe.android.link.exceptions.MissingConfigurationException
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.toLoginState
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.wallet.displayName
import com.stripe.android.link.ui.wallet.makeFallbackCardName
import com.stripe.android.link.utils.isLinkAuthorizationError
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.EmailSource
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.paymentdatacollection.ach.TransformToBankIcon
import com.stripe.android.paymentsheet.paymentdatacollection.ach.transformBankIconCodeToBankIcon
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.ui.getCardBrandIconForVerticalMode
import com.stripe.android.paymentsheet.ui.getLinkIcon
import com.stripe.android.uicore.isSystemDarkTheme
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Suppress("TooManyFunctions")
@Singleton
internal class LinkControllerInteractor @Inject constructor(
    private val application: Application,
    private val logger: Logger,
    private val linkConfigurationLoader: LinkConfigurationLoader,
    private val linkAccountHolder: LinkAccountHolder,
    private val linkComponentBuilderProvider: Provider<LinkComponent.Builder>,
) {

    private val tag = "LinkControllerViewInteractor"

    private val _account = linkAccountHolder.linkAccountInfo.mapAsStateFlow { it.account }

    private val _internalLinkAccount = _account.mapAsStateFlow {
        it?.let { account ->
            LinkController.LinkAccount(
                email = account.email,
                redactedPhoneNumber = account.redactedPhoneNumber,
                sessionState = when (account.accountStatus.toLoginState()) {
                    LinkState.LoginState.LoggedOut ->
                        LinkController.SessionState.LoggedOut
                    LinkState.LoginState.NeedsVerification ->
                        LinkController.SessionState.NeedsVerification
                    LinkState.LoginState.LoggedIn ->
                        LinkController.SessionState.LoggedIn
                },
                consumerSessionClientSecret = account.clientSecret
            )
        }
    }

    private val _state = MutableStateFlow(State())

    private val _presentPaymentMethodsResultFlow =
        MutableSharedFlow<LinkController.PresentPaymentMethodsResult>(extraBufferCapacity = 1)
    val presentPaymentMethodsResultFlow = _presentPaymentMethodsResultFlow.asSharedFlow()

    private val _authenticationResultFlow =
        MutableSharedFlow<LinkController.AuthenticationResult>(extraBufferCapacity = 1)
    val authenticationResultFlow = _authenticationResultFlow.asSharedFlow()

    private val _authorizeResultFlow =
        MutableSharedFlow<LinkController.AuthorizeResult>(extraBufferCapacity = 1)
    val authorizeResultFlow = _authorizeResultFlow.asSharedFlow()

    fun state(context: Context): StateFlow<LinkController.State> {
        return combineAsStateFlow(_internalLinkAccount, _state) { account, state ->
            LinkController.State(
                elementsSessionId = state.linkComponent?.configuration?.elementsSessionId,
                internalLinkAccount = account,
                merchantLogoUrl = state.linkComponent?.configuration?.merchantLogoUrl,
                selectedPaymentMethodPreview = state.selectedPaymentMethod?.details?.toPreview(context),
                createdPaymentMethod = state.createdPaymentMethod,
            )
        }
    }

    suspend fun configure(configuration: LinkController.Configuration): LinkController.ConfigureResult {
        logger.debug("$tag: updating configuration")
        updateState { State() }
        PaymentConfiguration.init(
            context = application,
            publishableKey = configuration.publishableKey,
            stripeAccountId = configuration.stripeAccountId,
        )
        return linkConfigurationLoader.load(configuration)
            .flatMapCatching { config ->
                val component = linkComponentBuilderProvider.get()
                    .configuration(config)
                    .build()
                component.linkAttestationCheck.invoke()
                    .toResult()
                    .map { component }
            }
            .fold(
                onSuccess = { component ->
                    updateState { it.copy(linkComponent = component) }
                    LinkController.ConfigureResult.Success
                },
                onFailure = { error ->
                    LinkController.ConfigureResult.Failed(error)
                }
            )
    }

    fun presentPaymentMethods(
        launcher: ActivityResultLauncher<LinkActivityContract.Args>,
        email: String?,
        paymentMethodType: LinkController.PaymentMethodType?,
    ) {
        present(
            launcher = launcher,
            email = email,
            paymentMethodType = paymentMethodType,
            onConfigurationError = { error ->
                _presentPaymentMethodsResultFlow.tryEmit(
                    LinkController.PresentPaymentMethodsResult.Failed(error)
                )
            },
            getLaunchMode = { _, state ->
                LinkLaunchMode.PaymentMethodSelection(
                    selectedPayment = state.selectedPaymentMethod?.details,
                    paymentMethodFilter = paymentMethodType?.toFilter(),
                    sharePaymentDetailsImmediatelyAfterCreation = false,
                    shouldShowSecondaryCta = false,
                )
            }
        )
    }

    fun authenticate(
        launcher: ActivityResultLauncher<LinkActivityContract.Args>,
        email: String?
    ) {
        performAuthentication(launcher, email, existingOnly = false)
    }

    fun authenticateExistingConsumer(
        launcher: ActivityResultLauncher<LinkActivityContract.Args>,
        email: String
    ) {
        performAuthentication(launcher, email, existingOnly = true)
    }

    private fun performAuthentication(
        launcher: ActivityResultLauncher<LinkActivityContract.Args>,
        email: String?,
        existingOnly: Boolean
    ) {
        present(
            launcher = launcher,
            email = email,
            onConfigurationError = { error ->
                _authenticationResultFlow.tryEmit(
                    LinkController.AuthenticationResult.Failed(error)
                )
            },
            getLaunchMode = { linkAccount, _ ->
                // This condition will need to change for web fallback.
                if (linkAccount?.hasVerifiedSMSSession == true) {
                    logger.debug("$tag: account is already verified, skipping authentication")
                    _authenticationResultFlow.tryEmit(LinkController.AuthenticationResult.Success)
                    null
                } else {
                    LinkLaunchMode.Authentication(existingOnly = existingOnly)
                }
            }
        )
    }

    private fun withConfiguration(
        email: String?,
        paymentMethodType: LinkController.PaymentMethodType?,
        onError: (Throwable) -> Unit,
        onSuccess: (LinkConfiguration) -> Unit
    ) {
        val configuration = requireLinkComponent()
            .map { it.configuration }
            .map { config ->
                if (email == null && paymentMethodType == null) {
                    // No change needed.
                    config
                } else {
                    val customerInfo = config.customerInfo
                        .copy(email = email ?: config.customerInfo.email)
                    val nameCollectionConfig =
                        if (paymentMethodType == LinkController.PaymentMethodType.BankAccount) {
                            PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                        } else {
                            config.billingDetailsCollectionConfiguration.name
                        }
                    val billingDetailsCollectionConfiguration = config.billingDetailsCollectionConfiguration
                        .copy(name = nameCollectionConfig)
                    config.copy(
                        customerInfo = customerInfo,
                        billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
                    )
                }
            }

        configuration.fold(
            onSuccess = { onSuccess(it) },
            onFailure = { error -> onError(error) }
        )
    }

    fun onLinkActivityResult(result: LinkActivityResult) {
        val currentLaunchMode = _state.value.currentLaunchMode
        updateState { it.copy(currentLaunchMode = null) }
        updateLinkAccountOnLinkResult(result)

        when (currentLaunchMode) {
            is LinkLaunchMode.PaymentMethodSelection ->
                handlePaymentMethodSelectionResult(result)
            is LinkLaunchMode.Authentication ->
                handleAuthenticationResult(result)
            is LinkLaunchMode.Authorization ->
                handleAuthorizationResult(result)
            else ->
                logger.warning("$tag: unexpected result for launch mode: $currentLaunchMode")
        }
    }

    private fun updateLinkAccountOnLinkResult(result: LinkActivityResult) {
        val error: Throwable? = (result as? LinkActivityResult.Failed)?.error
        val linkAccountUpdate = when {
            // Clear Link account if we got a Link auth error during any flow.
            error?.isLinkAuthorizationError() == true -> LinkAccountUpdate.Value(null)
            else -> result.linkAccountUpdate
        }
        updateStateOnAccountUpdate(update = linkAccountUpdate)
    }

    private fun updateStateOnNewEmail(email: String?) {
        val currentAccountEmail = _account.value?.email
        // Keep state if...
        val keepState =
            // input email matches previous input email (to support user changing emails), or
            email == _state.value.emailInput ||
                // not previously logged in, or
                currentAccountEmail == null ||
                // input email matches current logged in account email
                email == currentAccountEmail
        if (!keepState) {
            linkAccountHolder.set(LinkAccountUpdate.Value(null))
        }
        updateState {
            it.copy(
                emailInput = email,
                selectedPaymentMethod = it.selectedPaymentMethod.takeIf { keepState },
                createdPaymentMethod = it.createdPaymentMethod.takeIf { keepState },
            )
        }
    }

    private fun updateStateOnAccountUpdate(update: LinkAccountUpdate?) {
        when (update) {
            is LinkAccountUpdate.Value -> {
                val currentAccountEmail = _account.value?.email
                val newAccountEmail = update.account?.email
                // Keep state if not previously logged in or new account email matches previous email
                val keepState = currentAccountEmail == null || newAccountEmail == currentAccountEmail
                linkAccountHolder.set(update)
                updateState {
                    it.copy(
                        selectedPaymentMethod = it.selectedPaymentMethod.takeIf { keepState },
                        createdPaymentMethod = it.createdPaymentMethod.takeIf { keepState },
                    )
                }
            }
            is LinkAccountUpdate.None, null -> {
                // Do nothing.
            }
        }
    }

    private fun handlePaymentMethodSelectionResult(result: LinkActivityResult) {
        when (result) {
            is LinkActivityResult.Canceled -> {
                logger.debug("$tag: presentPaymentMethods canceled")
                _presentPaymentMethodsResultFlow.tryEmit(
                    LinkController.PresentPaymentMethodsResult.Canceled
                )
            }
            is LinkActivityResult.Completed -> {
                logger.debug("$tag: presentPaymentMethods completed: details=${result.selectedPayment?.details}")
                updateState {
                    it.copy(selectedPaymentMethod = result.selectedPayment)
                }
                _presentPaymentMethodsResultFlow.tryEmit(LinkController.PresentPaymentMethodsResult.Success)
            }
            is LinkActivityResult.Failed -> {
                logger.debug("$tag: presentPaymentMethods failed")
                _presentPaymentMethodsResultFlow.tryEmit(
                    LinkController.PresentPaymentMethodsResult.Failed(result.error)
                )
            }
            is LinkActivityResult.PaymentMethodObtained -> {
                logger.warning("$tag: presentPaymentMethods unexpected result: $result")
            }
        }
    }

    private fun handleAuthenticationResult(result: LinkActivityResult) {
        when (result) {
            is LinkActivityResult.Canceled -> {
                logger.debug("$tag: authentication canceled")
                _authenticationResultFlow.tryEmit(LinkController.AuthenticationResult.Canceled)
            }
            is LinkActivityResult.Completed -> {
                logger.debug("$tag: authentication completed")
                _authenticationResultFlow.tryEmit(LinkController.AuthenticationResult.Success)
            }
            is LinkActivityResult.Failed -> {
                logger.debug("$tag: authentication failed")
                _authenticationResultFlow.tryEmit(
                    LinkController.AuthenticationResult.Failed(result.error)
                )
            }
            is LinkActivityResult.PaymentMethodObtained -> {
                logger.warning("$tag: authentication unexpected result: $result")
            }
        }
    }

    private fun handleAuthorizationResult(result: LinkActivityResult) {
        when (result) {
            is LinkActivityResult.Canceled -> {
                logger.debug("$tag: authorization canceled")
                _authorizeResultFlow.tryEmit(LinkController.AuthorizeResult.Canceled)
            }
            is LinkActivityResult.Completed -> {
                logger.debug("$tag: authorization completed")
                _authorizeResultFlow.tryEmit(
                    when (result.authorizationConsentGranted) {
                        true -> LinkController.AuthorizeResult.Consented
                        false -> LinkController.AuthorizeResult.Denied
                        null -> LinkController.AuthorizeResult.Canceled // Shouldn't happen.
                    }
                )
            }
            is LinkActivityResult.Failed -> {
                logger.debug("$tag: authorization failed")
                _authorizeResultFlow.tryEmit(
                    LinkController.AuthorizeResult.Failed(result.error)
                )
            }
            is LinkActivityResult.PaymentMethodObtained -> {
                logger.warning("$tag: authorization unexpected result: $result")
            }
        }
    }

    suspend fun lookupConsumer(email: String): LinkController.LookupConsumerResult {
        return requireLinkComponent()
            .flatMapCatching { component ->
                component.linkAccountManager.lookupByEmail(
                    email = email,
                    emailSource = EmailSource.USER_ACTION,
                    startSession = true,
                    customerId = null,
                ).toResult()
            }
            .fold(
                onSuccess = { account ->
                    updateStateOnAccountUpdate(LinkAccountUpdate.Value(account))
                    LinkController.LookupConsumerResult.Success(email, account != null)
                },
                onFailure = {
                    LinkController.LookupConsumerResult.Failed(email, it)
                }
            )
    }

    suspend fun logOut(): LinkController.LogOutResult {
        return requireLinkComponent()
            .mapCatching { component ->
                component.linkAccountManager.logOut()
                updateStateOnAccountUpdate(
                    LinkAccountUpdate.Value(
                        account = null,
                        lastUpdateReason = LinkAccountUpdate.Value.UpdateReason.LoggedOut
                    )
                )
            }
            .fold(
                onSuccess = { LinkController.LogOutResult.Success() },
                onFailure = { LinkController.LogOutResult.Failed(it) }
            )
    }

    suspend fun createPaymentMethod(apiKey: String? = null): LinkController.CreatePaymentMethodResult {
        val paymentMethodResult = performCreatePaymentMethod(apiKey)
        updateState { it.copy(createdPaymentMethod = paymentMethodResult.getOrNull()) }
        return paymentMethodResult.fold(
            onSuccess = { LinkController.CreatePaymentMethodResult.Success(it) },
            onFailure = { LinkController.CreatePaymentMethodResult.Failed(it) },
        )
    }

    suspend fun registerConsumer(
        email: String,
        phone: String,
        country: String,
        name: String?,
    ): LinkController.RegisterConsumerResult {
        return requireLinkComponent()
            .flatMapCatching {
                it.linkAccountManager.signUp(
                    email = email,
                    phoneNumber = phone,
                    country = country,
                    countryInferringMethod = "PHONE_NUMBER",
                    name = name,
                    consentAction = SignUpConsentAction.Implied
                ).toResult()
            }
            .fold(
                onSuccess = { account ->
                    updateStateOnAccountUpdate(LinkAccountUpdate.Value(account))
                    LinkController.RegisterConsumerResult.Success
                },
                onFailure = {
                    updateStateOnAccountUpdate(LinkAccountUpdate.Value(null))
                    LinkController.RegisterConsumerResult.Failed(it)
                }
            )
    }

    suspend fun updatePhoneNumber(phoneNumber: String): LinkController.UpdatePhoneNumberResult {
        return requireLinkComponent()
            .flatMapCatching { component ->
                component.linkAccountManager.updatePhoneNumber(phoneNumber)
            }
            .fold(
                onSuccess = { linkAccount ->
                    // Update the account with the new phone number info
                    updateStateOnAccountUpdate(LinkAccountUpdate.Value(linkAccount))
                    LinkController.UpdatePhoneNumberResult.Success
                },
                onFailure = {
                    LinkController.UpdatePhoneNumberResult.Failed(it)
                }
            )
    }

    fun authorize(
        launcher: ActivityResultLauncher<LinkActivityContract.Args>,
        linkAuthIntentId: String
    ) {
        present(
            launcher = launcher,
            onConfigurationError = { error ->
                _authorizeResultFlow.tryEmit(
                    LinkController.AuthorizeResult.Failed(error)
                )
            },
            getLaunchMode = { _, _ ->
                LinkLaunchMode.Authorization(linkAuthIntentId = linkAuthIntentId)
            }
        )
    }

    private fun requireLinkComponent(state: State = _state.value): Result<LinkComponent> {
        return state.linkComponent
            ?.let { Result.success(it) }
            ?: Result.failure(MissingConfigurationException())
    }

    private fun present(
        launcher: ActivityResultLauncher<LinkActivityContract.Args>,
        email: String? = null,
        paymentMethodType: LinkController.PaymentMethodType? = null,
        onConfigurationError: (Throwable) -> Unit,
        getLaunchMode: (linkAccount: LinkAccount?, state: State) -> LinkLaunchMode?
    ) {
        logger.debug("$tag: presenting")

        withConfiguration(
            email = email,
            paymentMethodType = paymentMethodType,
            onError = onConfigurationError,
            onSuccess = { configuration ->
                updateStateOnNewEmail(email)

                val launchMode = getLaunchMode(_account.value, _state.value)
                    ?: return@withConfiguration

                updateState {
                    it.copy(
                        emailInput = email,
                        currentLaunchMode = launchMode,
                    )
                }

                launcher.launch(
                    LinkActivityContract.Args(
                        configuration = configuration,
                        linkExpressMode = LinkExpressMode.ENABLED,
                        linkAccountInfo = linkAccountHolder.linkAccountInfo.value,
                        launchMode = launchMode,
                        passiveCaptchaParams = null
                    )
                )
            }
        )
    }

    private suspend fun performCreatePaymentMethod(apiKey: String?): Result<PaymentMethod> {
        val state = _state.value
        val component = requireLinkComponent(state)
            .getOrElse { return Result.failure(it) }
        val configuration = component.configuration
        val paymentMethod = state.selectedPaymentMethod
            ?: return Result.failure(IllegalStateException("No selected payment method"))

        return if (configuration.passthroughModeEnabled) {
            component.linkAccountManager.sharePaymentDetails(
                paymentDetailsId = paymentMethod.details.id,
                expectedPaymentMethodType = computeExpectedPaymentMethodType(configuration, paymentMethod.details),
                cvc = paymentMethod.collectedCvc,
                billingPhone = null,
                apiKey = apiKey,
            ).map { shareDetails ->
                val json = JSONObject(shareDetails.encodedPaymentMethod)
                PaymentMethodJsonParser().parse(json)
            }
        } else {
            component.linkAccountManager.createPaymentMethod(
                linkPaymentMethod = paymentMethod
            )
        }
    }

    private fun LinkAttestationCheck.Result.toResult(): Result<Unit> =
        when (this) {
            is LinkAttestationCheck.Result.AccountError ->
                Result.failure(error)
            is LinkAttestationCheck.Result.AttestationFailed ->
                Result.failure(AppAttestationException(error))
            is LinkAttestationCheck.Result.Error ->
                Result.failure(error)
            LinkAttestationCheck.Result.Successful ->
                Result.success(Unit)
        }

    private fun Result<LinkAccount?>.toResult(): Result<LinkAccount?> =
        when (val linkAuthResult = this.toLinkAuthResult()) {
            is LinkAuthResult.AccountError ->
                Result.failure(linkAuthResult.error)
            is LinkAuthResult.AttestationFailed ->
                Result.failure(AppAttestationException(linkAuthResult.error))
            is LinkAuthResult.Error ->
                Result.failure(linkAuthResult.error)
            LinkAuthResult.NoLinkAccountFound ->
                Result.success(null)
            is LinkAuthResult.Success ->
                Result.success(linkAuthResult.account)
        }

    @VisibleForTesting
    internal fun updateState(block: (State) -> State) {
        _state.update(block)
    }

    fun clearLinkAccount() {
        updateStateOnAccountUpdate(LinkAccountUpdate.Value(account = null))
    }

    internal data class State(
        val linkComponent: LinkComponent? = null,
        val emailInput: String? = null,
        val selectedPaymentMethod: LinkPaymentMethod? = null,
        val createdPaymentMethod: PaymentMethod? = null,
        val currentLaunchMode: LinkLaunchMode? = null,
    ) {
        val linkConfiguration: LinkConfiguration?
            get() = linkComponent?.configuration
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface PaymentMethodPreviewDetails {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Card(
        val brand: CardBrand,
        val funding: String,
        val last4: String
    ) : PaymentMethodPreviewDetails

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class BankAccount(
        val bankIconCode: String?,
        val bankName: String?,
        val last4: String
    ) : PaymentMethodPreviewDetails
}

internal fun PaymentMethodPreviewDetails.toPreview(
    context: Context,
): LinkController.PaymentMethodPreview {
    val label = context.getString(com.stripe.android.R.string.stripe_link)
    val drawableResourceId = getIconDrawableRes(this, context.isSystemDarkTheme())
    val sublabel = buildString {
        val name: ResolvableString
        val last4: String
        when (this@toPreview) {
            is PaymentMethodPreviewDetails.Card -> {
                name = makeFallbackCardName(funding, brand.displayName)
                last4 = this@toPreview.last4
            }
            is PaymentMethodPreviewDetails.BankAccount -> {
                name = bankName?.resolvableString
                    ?: com.stripe.android.ui.core.R.string.stripe_payment_method_bank.resolvableString
                last4 = this@toPreview.last4
            }
        }
        append(name.resolve(context))
        append(" •••• ")
        append(last4)
    }

    return LinkController.PaymentMethodPreview(
        iconRes = drawableResourceId,
        label = label,
        sublabel = sublabel,
    )
}

internal fun ConsumerPaymentDetails.PaymentDetails.toPreview(
    context: Context,
): LinkController.PaymentMethodPreview {
    val label = context.getString(com.stripe.android.R.string.stripe_link)
    val sublabel = buildString {
        // It should never be `Passthrough`, but handling it here just in case.
        if (this@toPreview !is ConsumerPaymentDetails.Passthrough) {
            append(displayName.resolve(context))
        }
        append(" •••• ")
        append(last4)
    }
    val drawableResourceId = getIconDrawableRes(context.isSystemDarkTheme())

    return LinkController.PaymentMethodPreview(
        iconRes = drawableResourceId,
        label = label,
        sublabel = sublabel,
    )
}

@DrawableRes
internal fun ConsumerPaymentDetails.PaymentDetails.getIconDrawableRes(isDarkTheme: Boolean): Int {
    return when (this) {
        is ConsumerPaymentDetails.BankAccount ->
            getIconDrawableRes(PaymentMethodPreviewDetails.BankAccount(bankIconCode, bankName, last4), isDarkTheme)
        is ConsumerPaymentDetails.Card ->
            getIconDrawableRes(PaymentMethodPreviewDetails.Card(brand, funding, last4), isDarkTheme)
        is ConsumerPaymentDetails.Passthrough ->
            getLinkIcon(iconOnly = true)
    }
}

@DrawableRes
internal fun getIconDrawableRes(type: PaymentMethodPreviewDetails, isDarkTheme: Boolean): Int {
    return when (type) {
        is PaymentMethodPreviewDetails.BankAccount -> {
            val fallbackIcon =
                if (!isDarkTheme) {
                    R.drawable.stripe_link_bank_with_bg_day
                } else {
                    R.drawable.stripe_link_bank_with_bg_night
                }

            type.bankIconCode
                ?.let {
                    transformBankIconCodeToBankIcon(
                        iconCode = it,
                        fallbackIcon = fallbackIcon
                    )
                }
                ?: TransformToBankIcon(
                    bankName = type.bankName,
                    fallbackIcon = fallbackIcon
                )
        }
        is PaymentMethodPreviewDetails.Card ->
            type.brand.getCardBrandIconForVerticalMode()
    }
}

private fun LinkController.PaymentMethodType.toFilter(): LinkPaymentMethodFilter =
    when (this) {
        LinkController.PaymentMethodType.Card -> LinkPaymentMethodFilter.Card
        LinkController.PaymentMethodType.BankAccount -> LinkPaymentMethodFilter.BankAccount
    }
