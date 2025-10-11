package com.stripe.android.crypto.onramp

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.annotation.RestrictTo
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.crypto.onramp.di.DaggerOnrampComponent
import com.stripe.android.crypto.onramp.di.OnrampComponent
import com.stripe.android.crypto.onramp.di.OnrampPresenterComponent
import com.stripe.android.crypto.onramp.model.CryptoNetwork
import com.stripe.android.crypto.onramp.model.KycInfo
import com.stripe.android.crypto.onramp.model.LinkUserInfo
import com.stripe.android.crypto.onramp.model.OnrampAttachKycInfoResult
import com.stripe.android.crypto.onramp.model.OnrampCallbacks
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
import com.stripe.android.crypto.onramp.model.OnrampConfigurationResult
import com.stripe.android.crypto.onramp.model.OnrampCreateCryptoPaymentTokenResult
import com.stripe.android.crypto.onramp.model.OnrampHasLinkAccountResult
import com.stripe.android.crypto.onramp.model.OnrampLogOutResult
import com.stripe.android.crypto.onramp.model.OnrampRegisterLinkUserResult
import com.stripe.android.crypto.onramp.model.OnrampRegisterWalletAddressResult
import com.stripe.android.crypto.onramp.model.OnrampTokenAuthenticationResult
import com.stripe.android.crypto.onramp.model.OnrampUpdatePhoneNumberResult
import com.stripe.android.crypto.onramp.model.PaymentMethodType
import javax.inject.Inject

/**
 * Coordinator interface for managing the Onramp lifecycle, Link user checks,
 * and authentication flows.
 *
 * @param interactor The interactor that persists configuration state across process restarts.
 * @param presenterComponentFactory Factory for creating presenter components.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class OnrampCoordinator @Inject internal constructor(
    private val interactor: OnrampInteractor,
    private val presenterComponentFactory: OnrampPresenterComponent.Factory,
) {

    /**
     * Initialize the coordinator with the provided configuration.
     *
     * @param configuration The OnrampConfiguration to apply.
     */
    suspend fun configure(
        configuration: OnrampConfiguration,
    ): OnrampConfigurationResult {
        return interactor.configure(configuration)
    }

    /**
     * Authenticates a user given the provided token.
     *
     * @param linkAuthTokenClientSecret The auth token to authenticate a user with.
     */
    suspend fun authenticateUserWithToken(linkAuthTokenClientSecret: String): OnrampTokenAuthenticationResult {
        return interactor.authenticateUserWithToken(linkAuthTokenClientSecret)
    }

    /**
     * Whether or not the provided email is associated with an existing Link consumer.
     *
     * @param email The email address to look up.
     * @return OnrampLinkLookupResult indicating whether the user exists.
     */
    suspend fun hasLinkAccount(email: String): OnrampHasLinkAccountResult {
        return interactor.hasLinkAccount(email)
    }

    /**
     * Registers a new Link user with the provided details.
     *
     * @param info The [LinkUserInfo] for the new user.
     * @return OnrampRegisterUserResult indicating the result of registration.
     */
    suspend fun registerLinkUser(info: LinkUserInfo): OnrampRegisterLinkUserResult {
        return interactor.registerLinkUser(info)
    }

    /**
     * Updates the phone number for the current Link user.
     *
     * @property phone The phone number of the user. Phone number must be in E.164 format (e.g., +12125551234).
     * @return OnrampUpdatePhoneNumberResult indicating the result of the update.
     */
    suspend fun updatePhoneNumber(phone: String): OnrampUpdatePhoneNumberResult {
        return interactor.updatePhoneNumber(phone)
    }

    /**
     * Registers the given crypto wallet address to the current Link account.
     * Requires an authenticated Link user.
     *
     * @param walletAddress The crypto wallet address to register.
     * @param network The crypto network for the wallet address.
     * @return OnrampSetWalletAddressResult indicating the result of setting the wallet address.
     */
    suspend fun registerWalletAddress(
        walletAddress: String,
        network: CryptoNetwork
    ): OnrampRegisterWalletAddressResult {
        return interactor.registerWalletAddress(walletAddress, network)
    }

    /**
     * Attaches the specific KYC info to the current Link user. Requires an authenticated Link user.
     *
     * @param info The KYC info to attach to the Link user.
     * @return [OnrampAttachKycInfoResult] indicating the result.
     */
    suspend fun attachKycInfo(info: KycInfo): OnrampAttachKycInfoResult {
        return interactor.attachKycInfo(info)
    }

    /**
     * Creates a crypto payment token for the payment method currently selected on the coordinator.
     * Call after a successful [Presenter.collectPaymentMethod].
     *
     * @return A [OnrampCreateCryptoPaymentTokenResult] containing the crypto payment token ID.
     */
    suspend fun createCryptoPaymentToken(): OnrampCreateCryptoPaymentTokenResult {
        return interactor.createCryptoPaymentToken()
    }

    /**
     * Logs out the current Link user, if any.
     *
     * @return [OnrampLogOutResult] indicating the result of the logout operation.
     */
    suspend fun logOut(): OnrampLogOutResult {
        return interactor.logOut()
    }

    /**
     * Create a presenter for handling Link UI interactions.
     *
     * @param activity The activity that will host the Link flows.
     * @param onrampCallbacks Callbacks for handling asynchronous responses from UI operations.
     * @return A presenter instance for handling Link UI operations.
     */
    fun createPresenter(
        activity: ComponentActivity,
        onrampCallbacks: OnrampCallbacks
    ): Presenter {
        return presenterComponentFactory
            .build(
                activity = activity,
                lifecycleOwner = activity,
                activityResultRegistryOwner = activity,
                onrampCallbacks = onrampCallbacks,
            )
            .presenter
    }

    /**
     * Presenter for handling Link UI interactions without requiring direct activity references.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Presenter @Inject internal constructor(
        private val coordinator: OnrampPresenterCoordinator,
    ) {
        /**
         * Presents Link UI to authenticate an existing Link user.
         * Requires successful lookup or registration of the user first.
         */
        fun authenticateUser() {
            coordinator.authenticateUser()
        }

        /**
         * Creates an identity verification session and launches the document verification flow.
         * Requires an authenticated Link user.
         */
        fun verifyIdentity() {
            coordinator.verifyIdentity()
        }

        /**
         * Presents UI to collect/select a payment method of the given type.
         *
         * @param type The payment method type to collect.
         */
        fun collectPaymentMethod(type: PaymentMethodType) {
            coordinator.collectPaymentMethod(type)
        }

        /**
         * Authorizes a Link auth intent and authenticates the user if necessary.
         *
         * @param linkAuthIntentId The LinkAuthIntent ID to authorize.
         */
        fun authorize(linkAuthIntentId: String) {
            coordinator.authorize(linkAuthIntentId)
        }

        /**
         * Performs the checkout flow for a crypto onramp session, handling any required authentication steps.
         * The result will be delivered through the checkoutCallback provided in OnrampCallbacks.
         *
         * @param onrampSessionId The onramp session identifier.
         * @param checkoutHandler An async closure that calls your backend to perform a checkout.
         *     Your backend should call Stripe's `/v1/crypto/onramp_sessions/:id/checkout`
         *     endpoint with the onramp session ID. The closure should return the onramp session client secret
         *     on success, or throw an Error on failure. This closure may be called twice: once initially,
         *     and once more after handling any required authentication.
         */
        fun performCheckout(
            onrampSessionId: String,
            checkoutHandler: suspend () -> String
        ) {
            coordinator.performCheckout(
                onrampSessionId = onrampSessionId,
                checkoutHandler = checkoutHandler
            )
        }
    }

    /**
     * A Builder utility type to create an [OnrampCoordinator] with appropriate parameters.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Builder {
        /**
         * Constructs an [OnrampCoordinator] for the given parameters.
         *
         * @param application The Application context.
         * @param savedStateHandle The SavedStateHandle for state persistence.
         */
        fun build(
            application: Application,
            savedStateHandle: SavedStateHandle
        ): OnrampCoordinator {
            val onrampComponent: OnrampComponent =
                DaggerOnrampComponent.factory()
                    .build(
                        application = application,
                        savedStateHandle = savedStateHandle
                    )
            return onrampComponent.onrampCoordinator
        }
    }
}
