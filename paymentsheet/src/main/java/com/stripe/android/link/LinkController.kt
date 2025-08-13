package com.stripe.android.link

import android.app.Application
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.common.configuration.ConfigurationDefaults
import com.stripe.android.common.ui.DelegateDrawable
import com.stripe.android.link.injection.DaggerLinkControllerComponent
import com.stripe.android.link.injection.LinkControllerPresenterComponent
import com.stripe.android.link.model.LinkAppearance
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.RequestSurface
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.image.rememberDrawablePainter
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.flow.StateFlow
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A controller to perform various Link operations.
 */
@Singleton
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LinkController @Inject internal constructor(
    private val interactor: LinkControllerInteractor,
    private val presenterComponentFactory: LinkControllerPresenterComponent.Factory
) {
    /**
     * The current [State] of the Link controller.
     */
    fun state(context: Context): StateFlow<State> = interactor.state(context)

    /**
     * Configure the controller with a [Configuration].
     *
     * The [state] will reset and the Link session will be reloaded to reflect the new configuration.
     *
     * @param configuration The [Configuration] to use for Link operations.
     * @return The result of the configuration.
     */
    suspend fun configure(configuration: Configuration): ConfigureResult {
        return interactor.configure(configuration)
    }

    /**
     * Create a payment method from the currently selected Link payment method.
     *
     * This converts the selected Link payment method into a Stripe [PaymentMethod] that can be
     * used for payment processing. The created payment method will be available in [State.createdPaymentMethod].
     *
     * **Note**: This requires a payment method to be selected via [Presenter.presentPaymentMethods] first,
     * and a valid Link configuration and account. If these requirements are not met, the operation will fail.
     *
     * @return The result of the payment method creation.
     */
    suspend fun createPaymentMethod(): CreatePaymentMethodResult {
        return interactor.createPaymentMethod()
    }

    /**
     * Look up whether the provided email address is associated with an existing Link consumer account.
     *
     * This is useful for determining whether to show Link-specific UI elements or messaging to the user
     * before they interact with Link payment methods.
     *
     * @param email The email address to check for an existing Link consumer account.
     * @return The result of the consumer lookup.
     */
    suspend fun lookupConsumer(email: String): LookupConsumerResult {
        return interactor.lookupConsumer(email)
    }

    /**
     * Creates a [Presenter] for the Link controller that can present user-interactive flows.
     *
     * @param activity The [ComponentActivity] that will host the Link UI.
     * @param presentPaymentMethodsCallback Callback to receive results from presenting payment methods.
     * @param authenticationCallback Callback to receive results from authentication flows.
     */
    fun createPresenter(
        activity: ComponentActivity,
        presentPaymentMethodsCallback: PresentPaymentMethodsCallback,
        authenticationCallback: AuthenticationCallback,
    ): Presenter {
        return presenterComponentFactory
            .build(
                activity = activity,
                lifecycleOwner = activity,
                activityResultRegistryOwner = activity,
                presentPaymentMethodsCallback = presentPaymentMethodsCallback,
                authenticationCallback = authenticationCallback,
            )
            .presenter
    }

    // Crypto Onramp specific methods

    /**
     * [CRYPTO ONRAMP ONLY] Register a new Link consumer account.
     *
     * @param email The email address to register for the new Link consumer account.
     * @param phone The phone number associated with the new account.
     * @param country The country code for the new account, in ISO 3166-1 alpha-2 format.
     * @param name The name of the consumer. Optional, can be null.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    suspend fun registerConsumer(
        email: String,
        phone: String,
        country: String,
        name: String?,
    ): RegisterConsumerResult {
        return interactor.registerConsumer(
            email = email,
            phone = phone,
            country = country,
            name = name,
        )
    }

    /**
     * Configuration for [LinkController].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    @Poko
    class Configuration internal constructor(
        internal val merchantDisplayName: String,
        internal val cardBrandAcceptance: PaymentSheet.CardBrandAcceptance,
        internal val defaultBillingDetails: PaymentSheet.BillingDetails?,
        internal val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
        internal val allowUserEmailEdits: Boolean,
        internal val linkAppearance: LinkAppearance? = null
    ) : Parcelable {

        /**
         * [Configuration] builder.
         *
         * @param merchantDisplayName Your customer-facing business name.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class Builder(
            /**
             * Your customer-facing business name.
             */
            private val merchantDisplayName: String,
            private val appearance: LinkAppearance? = null
        ) {
            private var cardBrandAcceptance: PaymentSheet.CardBrandAcceptance =
                ConfigurationDefaults.cardBrandAcceptance
            private var defaultBillingDetails: PaymentSheet.BillingDetails? =
                ConfigurationDefaults.billingDetails
            private var billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
                ConfigurationDefaults.billingDetailsCollectionConfiguration
            private var allowUserEmailEdits: Boolean = true

            /**
             * Configuration for which card brands should be accepted or blocked.
             *
             * By default, Link will accept all card brands supported by Stripe. You can use this
             * to restrict which card brands are available for Link payment methods.
             *
             * @param cardBrandAcceptance Configuration for which card brands should be accepted.
             * @return This builder instance for method chaining.
             */
            fun cardBrandAcceptance(cardBrandAcceptance: PaymentSheet.CardBrandAcceptance) = apply {
                this.cardBrandAcceptance = cardBrandAcceptance
            }

            /**
             * The billing information for the customer.
             *
             * If set, PaymentSheet will pre-populate the form fields with the values provided.
             * If `billingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod` is `true`,
             * these values will be attached to the payment method even if they are not collected by
             * the PaymentSheet UI.
             */
            fun defaultBillingDetails(defaultBillingDetails: PaymentSheet.BillingDetails?) =
                apply { this.defaultBillingDetails = defaultBillingDetails }

            /**
             * Describes how billing details should be collected.
             * All values default to `automatic`.
             * If `never` is used for a required field for the Payment Method used during checkout,
             * you **must** provide an appropriate value as part of [defaultBillingDetails].
             */
            fun billingDetailsCollectionConfiguration(
                billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
            ) = apply {
                this.billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration
            }

            /**
             * Whether to allow users to edit their email address within Link.
             *
             * @param allowUserEmailEdits True to allow email editing, false to disable it.
             * @return This builder instance for method chaining.
             */
            fun allowUserEmailEdits(allowUserEmailEdits: Boolean) = apply {
                this.allowUserEmailEdits = allowUserEmailEdits
            }

            /**
             * Build the [Configuration] instance.
             *
             * @return A new [Configuration] with the specified settings.
             */
            fun build(): Configuration = Configuration(
                allowUserEmailEdits = allowUserEmailEdits,
                merchantDisplayName = merchantDisplayName,
                cardBrandAcceptance = cardBrandAcceptance,
                defaultBillingDetails = defaultBillingDetails,
                billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
                linkAppearance = appearance
            )
        }

        internal companion object {
            fun default(context: Context): Configuration {
                val appName = context.applicationInfo.loadLabel(context.packageManager).toString()
                return Builder(appName).build()
            }
        }
    }

    /**
     * Contains information about the current state of the Link controller.
     *
     * @param selectedPaymentMethodPreview A preview of the currently selected payment method from Link, if any.
     * @param createdPaymentMethod The [PaymentMethod] created from the selected Link payment method, if any.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Poko
    class State
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @field:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val internalLinkAccount: LinkAccount? = null,
        val selectedPaymentMethodPreview: PaymentMethodPreview? = null,
        val createdPaymentMethod: PaymentMethod? = null,
    ) {
        /**
         * Whether the Link consumer account is verified. Null if no account is loaded.
         */
        val isConsumerVerified: Boolean?
            get() = internalLinkAccount?.sessionState?.let { it == SessionState.LoggedIn }
    }

    /**
     * A presenter for the Link controller that handles UI operations requiring Activity context.
     *
     * The Presenter is tied to an Activity lifecycle and should be created and destroyed appropriately
     * to avoid memory leaks.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Presenter @Inject internal constructor(
        private val coordinator: LinkControllerCoordinator,
        private val interactor: LinkControllerInteractor,
    ) {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        var paymentSelectionHint: String? = null

        /**
         * Present the Link payment methods selection screen.
         *
         * This will launch the Link activity where users can select from their saved payment methods
         * or add new ones. The result will be communicated through the [PresentPaymentMethodsCallback]
         * provided during controller creation.
         *
         * If a presentation is already in progress, this call will be ignored.
         *
         * @param email The email address to use for Link account lookup. If provided and the email
         * matches an existing Link account, the account's payment methods will be available for selection.
         * If null, the user will need to sign in or create a Link account.
         */
        fun presentPaymentMethods(email: String?) {
            interactor.presentPaymentMethods(
                launcher = coordinator.linkActivityResultLauncher,
                email = email,
                hint = paymentSelectionHint,
            )
        }

        // Crypto Onramp specific methods

        /**
         * [CRYPTO ONRAMP ONLY] Authenticate with Link.
         *
         * This will launch the Link activity where users can authenticate with their Link account.
         * The authentication flow will close after successful authentication instead of continuing
         * to payment selection. The result will be communicated through the [AuthenticationCallback]
         * provided during controller creation.
         *
         * If authentication is already in progress, this call will be ignored.
         *
         * @param email The email address to use for Link account lookup. If provided and the email
         * matches an existing Link account, the user will be able to authenticate with that account.
         * If null, the user will need to sign in or create a Link account.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun authenticate(email: String?) {
            interactor.authenticate(
                launcher = coordinator.linkActivityResultLauncher,
                email = email
            )
        }

        /**
         * [CRYPTO ONRAMP ONLY] Authenticate with Link for existing consumers only.
         *
         * This will launch the Link activity where users can authenticate with their Link account.
         * Unlike [authenticate], this method will fail with [NoLinkAccountFoundException] if the
         * provided email is not associated with an existing Link consumer account, rather than
         * allowing the user to sign up for a new account.
         *
         * The authentication flow will close after successful authentication instead of continuing
         * to payment selection. The result will be communicated through the [AuthenticationCallback]
         * provided during controller creation.
         *
         * If authentication is already in progress, this call will be ignored.
         *
         * @param email The email address to use for Link account lookup. Must be associated with
         * an existing Link consumer account, otherwise the authentication will fail.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun authenticateExistingConsumer(email: String) {
            interactor.authenticateExistingConsumer(
                launcher = coordinator.linkActivityResultLauncher,
                email = email
            )
        }
    }

    /**
     * Result of presenting Link payment methods to the user.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface ConfigureResult {
        /**
         * Configuration was successful.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data object Success : ConfigureResult

        /**
         * Configuration failed.
         *
         * @param error The error that occurred.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Poko
        class Failed internal constructor(val error: Throwable) : ConfigureResult
    }

    /**
     * Result of presenting Link payment methods to the user.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface PresentPaymentMethodsResult {

        /**
         * The user successfully selected a payment method from Link.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data object Success : PresentPaymentMethodsResult

        /**
         * The user canceled the Link payment methods selection.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data object Canceled : PresentPaymentMethodsResult

        /**
         * An error occurred while presenting Link payment methods.
         *
         * @param error The error that occurred.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Poko
        class Failed internal constructor(val error: Throwable) : PresentPaymentMethodsResult
    }

    /**
     * Result of looking up a consumer account by email.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface LookupConsumerResult {

        /**
         * The consumer lookup completed successfully.
         *
         * @param email The email address that was looked up.
         * @param isConsumer Whether the email is associated with an existing Link consumer account.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Poko
        class Success internal constructor(val email: String, val isConsumer: Boolean) : LookupConsumerResult

        /**
         * An error occurred while looking up the consumer.
         *
         * @param email The email address that was being looked up.
         * @param error The error that occurred.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Poko
        class Failed internal constructor(val email: String, val error: Throwable) : LookupConsumerResult
    }

    /**
     * Result of creating a payment method from a selected Link payment method.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface CreatePaymentMethodResult {

        /**
         * The payment method was created successfully.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data object Success : CreatePaymentMethodResult

        /**
         * An error occurred while creating the payment method.
         *
         * @param error The error that occurred.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Poko
        class Failed internal constructor(val error: Throwable) : CreatePaymentMethodResult
    }

    /**
     * [CRYPTO ONRAMP ONLY] Result of authenticating with Link.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface AuthenticationResult {

        /**
         * The user successfully authenticated with Link.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data object Success : AuthenticationResult

        /**
         * The user canceled the Link authentication.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data object Canceled : AuthenticationResult

        /**
         * An error occurred while authenticating with Link.
         *
         * @param error The error that occurred.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Poko
        class Failed internal constructor(val error: Throwable) : AuthenticationResult
    }

    /**
     * [CRYPTO ONRAMP ONLY] Result of registering a new Link consumer account.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface RegisterConsumerResult {

        /**
         * The user successfully registered a new Link consumer account.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data object Success : RegisterConsumerResult

        /**
         * An error occurred while registering a new Link consumer account.
         *
         * @param error The error that occurred.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Poko
        class Failed internal constructor(val error: Throwable) : RegisterConsumerResult
    }

    /**
     * Callback for receiving results from [Presenter.presentPaymentMethods].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun interface PresentPaymentMethodsCallback {
        fun onPresentPaymentMethodsResult(result: PresentPaymentMethodsResult)
    }

    /**
     * [CRYPTO ONRAMP ONLY] Callback for receiving results from [Presenter.authenticate] and
     * [Presenter.authenticateExistingConsumer].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun interface AuthenticationCallback {
        fun onAuthenticationResult(result: AuthenticationResult)
    }

    /**
     * Information about a Link consumer account.
     *
     * @param email The email address associated with the Link account.
     * @param redactedPhoneNumber The phone number associated with the account, with sensitive digits redacted.
     * @param sessionState The current session state of the Link account.
     * @param consumerSessionClientSecret The client secret for the consumer session, if available.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    @Poko
    class LinkAccount(
        val email: String,
        val redactedPhoneNumber: String,
        val sessionState: SessionState,
        val consumerSessionClientSecret: String?,
    ) : Parcelable

    /**
     * Represents the current session state of a Link consumer account.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class SessionState {
        /** The user is not logged in to their Link account. */
        LoggedOut,

        /** The user is logged in but needs to verify their account (e.g., via SMS). */
        NeedsVerification,

        /** The user is fully logged in and verified. */
        LoggedIn,
    }

    /**
     * Preview information for a Link payment method.
     *
     * @param label The main label text (e.g., "Link").
     * @param sublabel Additional descriptive text (e.g., "Visa •••• 4242").
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Poko
    class PaymentMethodPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(
        private val imageLoader: suspend () -> Drawable,
        val label: String,
        val sublabel: String,
    ) {
        /**
         * An image representing a payment method; e.g. the VISA logo.
         */
        @IgnoredOnParcel
        val icon: Drawable by lazy {
            DelegateDrawable(imageLoader)
        }

        /**
         * An image representing a payment method; e.g. the VISA logo.
         */
        val iconPainter: Painter
            @Composable
            get() = rememberDrawablePainter(icon)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        fun create(
            application: Application,
            savedStateHandle: SavedStateHandle
        ): LinkController {
            return create(
                application = application,
                savedStateHandle = savedStateHandle,
                // Temporarily "android_crypto_onramp" until backend is ready.
                // Should be "android_link_controller" instead.
                requestSurface = RequestSurface.CryptoOnramp,
            )
        }

        // Internal use only.
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        fun create(
            application: Application,
            savedStateHandle: SavedStateHandle,
            requestSurface: RequestSurface,
        ): LinkController {
            return DaggerLinkControllerComponent.factory()
                .build(
                    application = application,
                    savedStateHandle = savedStateHandle,
                    paymentElementCallbackIdentifier = "LinkController",
                    requestSurface = requestSurface,
                )
                .linkController
        }
    }
}
