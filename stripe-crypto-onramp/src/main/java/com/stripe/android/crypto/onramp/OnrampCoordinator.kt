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
import com.stripe.android.crypto.onramp.model.OnrampCallbacks
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
import com.stripe.android.crypto.onramp.model.OnrampCreateCryptoPaymentTokenResult
import com.stripe.android.crypto.onramp.model.OnrampKYCResult
import com.stripe.android.crypto.onramp.model.OnrampLinkLookupResult
import com.stripe.android.crypto.onramp.model.OnrampRegisterUserResult
import com.stripe.android.crypto.onramp.model.OnrampSetWalletAddressResult
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
    ) {
        interactor.configure(configuration)
    }

    /**
     * Looks up whether the provided email is associated with an existing Link user.
     *
     * @param email The email address to look up.
     * @return OnrampLinkLookupResult indicating whether the user exists.
     */
    suspend fun lookupLinkUser(email: String): OnrampLinkLookupResult {
        return interactor.lookupLinkUser(email)
    }

    /**
     * Given the required information, registers a new Link user.
     *
     * @param info The LinkInfo for the new user.
     * @return OnrampRegisterUserResult indicating the result of registration.
     */
    suspend fun registerLinkUser(info: LinkUserInfo): OnrampRegisterUserResult {
        return interactor.registerNewLinkUser(info)
    }

    /**
     * Registers a wallet address for the user.
     *
     * @param walletAddress The wallet address to register.
     * @param network The crypto network for the wallet address.
     * @return OnrampSetWalletAddressResult indicating the result of setting the wallet address.
     */
    suspend fun registerWalletAddress(
        walletAddress: String,
        network: CryptoNetwork
    ): OnrampSetWalletAddressResult {
        return interactor.registerWalletAddress(walletAddress, network)
    }

    /**
     * Given the required information, collects KYC information.
     *
     * @param info The KycInfo for the user.
     * @return OnrampKYCResult indicating the result of data collection.
     */
    suspend fun collectKycInfo(info: KycInfo): OnrampKYCResult {
        return interactor.collectKycInfo(info)
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
         * Presents the Link verification flow for an existing user.
         *
         * Requires successful lookup or registration of the user first.
         */
        fun presentForVerification() {
            coordinator.presentForVerification()
        }

        /**
         * Creates an identity verification session and launches the verification flow.
         *
         * Requires successful lookup or registration of the user first.
         */
        fun promptForIdentityVerification() {
            coordinator.promptForIdentityVerification()
        }

        /**
         * Presents a payment selection UI to the user.
         *
         * @param type The payment method type to collect.
         */
        fun collectPaymentMethod(type: PaymentMethodType) {
            coordinator.collectPaymentMethod(type)
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
