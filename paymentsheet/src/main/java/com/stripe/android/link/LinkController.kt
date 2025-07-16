package com.stripe.android.link

import android.app.Activity
import android.content.Context
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.common.configuration.ConfigurationDefaults
import com.stripe.android.link.injection.LinkControllerScope
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.flow.StateFlow
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * A controller to perform various Link operations.
 */
@LinkControllerScope
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LinkController @Inject internal constructor(
    activity: Activity,
    private val linkControllerCoordinator: LinkControllerCoordinator,
    private val viewModel: LinkControllerViewModel,
) {
    val state: StateFlow<State> = viewModel.state(activity)

    /**
     * Configure the controller with a [Configuration].
     *
     * The [state] will reset and the Link session will be reloaded to reflect the new configuration.
     *
     * @param configuration The [Configuration] to use for Link operations.
     * @return The result of the configuration.
     */
    suspend fun configure(configuration: Configuration): ConfigureResult {
        return viewModel.configure(configuration)
    }

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
        viewModel.onPresentPaymentMethods(
            launcher = linkControllerCoordinator.linkActivityResultLauncher,
            email = email
        )
    }

    /**
     * [CRYPTO ON-RAMP ONLY] Authenticate with Link.
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
        viewModel.onAuthenticate(
            launcher = linkControllerCoordinator.linkActivityResultLauncher,
            email = email
        )
    }

    /**
     * [CRYPTO ON-RAMP ONLY] Authenticate with Link for existing consumers only.
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
        viewModel.onAuthenticateExistingConsumer(
            launcher = linkControllerCoordinator.linkActivityResultLauncher,
            email = email
        )
    }

    /**
     * Create a payment method from the currently selected Link payment method.
     *
     * This converts the selected Link payment method into a Stripe [PaymentMethod] that can be
     * used for payment processing. The created payment method will be available in [State.createdPaymentMethod]
     * and the result will be communicated through the [CreatePaymentMethodCallback].
     *
     * **Note**: This requires a payment method to be selected via [presentPaymentMethods] first,
     * and a valid Link configuration and account. If these requirements are not met, the operation will fail.
     */
    fun createPaymentMethod() {
        viewModel.onCreatePaymentMethod()
    }

    /**
     * Look up whether the provided email address is associated with an existing Link consumer account.
     *
     * This is useful for determining whether to show Link-specific UI elements or messaging to the user
     * before they interact with Link payment methods.
     *
     * The result will be communicated through the [LookupConsumerCallback] provided during controller creation.
     *
     * @param email The email address to check for an existing Link consumer account.
     */
    fun lookupConsumer(email: String) {
        viewModel.onLookupConsumer(email)
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
            private val merchantDisplayName: String
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
     * @param isConsumerVerified Whether the Link consumer account is verified. Null if no account is loaded.
     * @param selectedPaymentMethodPreview A preview of the currently selected payment method from Link, if any.
     * @param createdPaymentMethod The [PaymentMethod] created from the selected Link payment method, if any.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    @Poko
    class State
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(
        val isConsumerVerified: Boolean? = null,
        val selectedPaymentMethodPreview: PaymentMethodPreview? = null,
        val createdPaymentMethod: PaymentMethod? = null,
    ) : Parcelable

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
     * Result of authenticating with Link.
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
        class Failed internal constructor(val error: Throwable) : AuthenticationResult
    }

    /**
     * Callback for receiving results from [presentPaymentMethods].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun interface PresentPaymentMethodsCallback {
        fun onPresentPaymentMethodsResult(result: PresentPaymentMethodsResult)
    }

    /**
     * Callback for receiving results from [lookupConsumer].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun interface LookupConsumerCallback {
        fun onLookupConsumerResult(result: LookupConsumerResult)
    }

    /**
     * Callback for receiving results from [createPaymentMethod].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun interface CreatePaymentMethodCallback {
        fun onCreatePaymentMethodResult(result: CreatePaymentMethodResult)
    }

    /**
     * Callback for receiving results from [authenticate].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun interface AuthenticationCallback {
        fun onAuthenticationResult(result: AuthenticationResult)
    }

    /**
     * Preview information for a Link payment method.
     *
     * @param iconRes The drawable resource ID for the payment method icon.
     * @param label The main label text (e.g., "Link").
     * @param sublabel Additional descriptive text (e.g., "Card •••• 1234").
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    @Poko
    class PaymentMethodPreview(
        @DrawableRes val iconRes: Int,
        val label: String,
        val sublabel: String?
    ) : Parcelable

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        /**
         * Create a [LinkController] instance.
         *
         * @param activity The Activity that will present Link-related UI.
         * @param presentPaymentMethodsCallback Called with the result when [presentPaymentMethods] completes.
         * @param lookupConsumerCallback Called with the result when [lookupConsumer] completes.
         * @param createPaymentMethodCallback Called with the result when [createPaymentMethod] completes.
         * @param authenticationCallback Called with the result when [authenticate] completes.
         *
         * @return A configured [LinkController] instance.
         */
        @JvmStatic
        fun create(
            activity: ComponentActivity,
            presentPaymentMethodsCallback: PresentPaymentMethodsCallback,
            lookupConsumerCallback: LookupConsumerCallback,
            createPaymentMethodCallback: CreatePaymentMethodCallback,
            authenticationCallback: AuthenticationCallback,
        ): LinkController {
            val viewModelProvider = ViewModelProvider(
                owner = activity,
                factory = LinkControllerViewModel.Factory()
            )
            val viewModel = viewModelProvider[LinkControllerViewModel::class.java]
            return viewModel
                .controllerComponentFactory.build(
                    activity = activity,
                    lifecycleOwner = activity,
                    activityResultRegistryOwner = activity,
                    presentPaymentMethodsCallback = presentPaymentMethodsCallback,
                    lookupConsumerCallback = lookupConsumerCallback,
                    createPaymentMethodCallback = createPaymentMethodCallback,
                    authenticationCallback = authenticationCallback,
                )
                .controller
        }
    }
}
