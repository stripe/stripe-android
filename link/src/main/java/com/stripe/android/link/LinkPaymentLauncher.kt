package com.stripe.android.link

import android.content.Context
import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.annotation.RestrictTo
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.NonFallbackInjectable
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.link.injection.DaggerLinkPaymentLauncherComponent
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.link.injection.LinkPaymentLauncherComponent
import com.stripe.android.link.ui.cardedit.CardEditViewModel
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.link.ui.paymentmethod.PaymentMethodViewModel
import com.stripe.android.link.ui.paymentmethod.SupportedPaymentMethod
import com.stripe.android.link.ui.signup.SignUpViewModel
import com.stripe.android.link.ui.verification.VerificationViewModel
import com.stripe.android.link.ui.wallet.WalletViewModel
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.uicore.address.AddressRepository
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Launcher for an Activity that will confirm a payment using Link.
 */
@Singleton
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LinkPaymentLauncher @Inject internal constructor(
    context: Context,
    @Named(PRODUCT_USAGE) private val productUsage: Set<String>,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(STRIPE_ACCOUNT_ID) private val stripeAccountIdProvider: () -> String?,
    @Named(ENABLE_LOGGING) private val enableLogging: Boolean,
    @IOContext ioContext: CoroutineContext,
    @UIContext uiContext: CoroutineContext,
    paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
    analyticsRequestExecutor: AnalyticsRequestExecutor,
    stripeRepository: StripeRepository,
    addressRepository: AddressRepository,
) : NonFallbackInjectable {
    private val launcherComponentBuilder = DaggerLinkPaymentLauncherComponent.builder()
        .context(context)
        .ioContext(ioContext)
        .uiContext(uiContext)
        .analyticsRequestFactory(paymentAnalyticsRequestFactory)
        .analyticsRequestExecutor(analyticsRequestExecutor)
        .stripeRepository(stripeRepository)
        .addressRepository(addressRepository)
        .enableLogging(enableLogging)
        .publishableKeyProvider(publishableKeyProvider)
        .stripeAccountIdProvider(stripeAccountIdProvider)
        .productUsage(productUsage)

    @InjectorKey
    private val injectorKey: String = WeakMapInjectorRegistry.nextKey(
        requireNotNull(LinkPaymentLauncher::class.simpleName)
    )

    private val componentFlow = MutableStateFlow<LinkPaymentLauncherComponent?>(null)

    @OptIn(FlowPreview::class)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val emailFlow: Flow<String?> = componentFlow
        .filterNotNull()
        .flatMapMerge { it.linkAccountManager.linkAccount }
        .map { it?.email }

    /**
     * The dependency injector Component for all injectable classes in Link while in an embedded
     * environment.
     */
    internal val component: LinkPaymentLauncherComponent?
        get() = componentFlow.value

    /**
     * Fetch the customer's account status, initializing the dependencies if they haven't been
     * initialized yet.
     */
    fun getAccountStatusFlow(configuration: Configuration) =
        getLinkPaymentLauncherComponent(configuration).linkAccountManager.accountStatus

    private var linkActivityResultLauncher:
        ActivityResultLauncher<LinkActivityContract.Args>? = null

    fun register(
        activityResultRegistry: ActivityResultRegistry,
        callback: (LinkActivityResult) -> Unit,
    ) {
        linkActivityResultLauncher = activityResultRegistry.register(
            "LinkHandler_${UUID.randomUUID()}",
            LinkActivityContract(),
            callback,
        )
    }

    fun register(
        activityResultCaller: ActivityResultCaller,
        callback: (LinkActivityResult) -> Unit,
    ) {
        linkActivityResultLauncher = activityResultCaller.registerForActivityResult(
            LinkActivityContract(),
            callback,
        )
    }

    fun unregister() {
        linkActivityResultLauncher?.unregister()
        linkActivityResultLauncher = null
    }

    /**
     * Launch the Link UI to process a payment.
     *
     * @param configuration The payment and customer settings
     * @param prefilledNewCardParams The card information prefilled by the user. If non null, Link
     *  will launch into adding a new card, with the card information pre-filled.
     */
    fun present(
        configuration: Configuration,
        prefilledNewCardParams: PaymentMethodCreateParams? = null,
    ) {
        val args = LinkActivityContract.Args(
            configuration,
            prefilledNewCardParams,
            LinkActivityContract.Args.InjectionParams(
                injectorKey,
                productUsage,
                enableLogging,
                publishableKeyProvider(),
                stripeAccountIdProvider()
            )
        )
        buildLinkComponent(getLinkPaymentLauncherComponent(configuration), args)
        linkActivityResultLauncher?.launch(args)
    }

    /**
     * Trigger Link sign in with the input collected from the user inline in PaymentSheet, whether
     * it's a new or existing account.
     */
    suspend fun signInWithUserInput(
        configuration: Configuration,
        userInput: UserInput
    ) = getLinkPaymentLauncherComponent(configuration)
        .linkAccountManager
        .signInWithUserInput(userInput)
        .map { true }

    /**
     * Attach a new Card to the currently signed in Link account.
     *
     * @return The parameters needed to confirm the current Stripe Intent using the newly created
     *          PaymentDetails.
     */
    suspend fun attachNewCardToAccount(
        configuration: Configuration,
        paymentMethodCreateParams: PaymentMethodCreateParams
    ): Result<LinkPaymentDetails.New> =
        getLinkPaymentLauncherComponent(configuration)
            .linkAccountManager
            .createCardPaymentDetails(paymentMethodCreateParams)

    /**
     * Create or get the existing [LinkPaymentLauncherComponent], responsible for injecting all
     * injectable classes in Link while in an embedded environment.
     */
    private fun getLinkPaymentLauncherComponent(configuration: Configuration) =
        component?.takeIf { it.configuration == configuration }
            ?: launcherComponentBuilder
                .configuration(configuration)
                .build()
                .also {
                    componentFlow.value = it
                }

    /**
     * Set up [LinkComponent], responsible for injecting all dependencies into the Link app.
     */
    private fun buildLinkComponent(
        component: LinkPaymentLauncherComponent,
        args: LinkActivityContract.Args
    ) {
        val linkComponent = component.linkComponentBuilder.starterArgs(args).build()
        val injector = object : NonFallbackInjector {
            override fun inject(injectable: Injectable<*>) {
                when (injectable) {
                    is LinkActivityViewModel.Factory -> linkComponent.inject(injectable)
                    is SignUpViewModel.Factory -> linkComponent.inject(injectable)
                    is VerificationViewModel.Factory -> linkComponent.inject(injectable)
                    is WalletViewModel.Factory -> linkComponent.inject(injectable)
                    is PaymentMethodViewModel.Factory -> linkComponent.inject(injectable)
                    is CardEditViewModel.Factory -> linkComponent.inject(injectable)
                    else -> {
                        throw IllegalArgumentException("invalid Injectable $injectable requested in $this")
                    }
                }
            }
        }
        WeakMapInjectorRegistry.register(injector, injectorKey)
    }

    /**
     * Arguments for launching [LinkActivity] to confirm a payment with Link.
     *
     * @param stripeIntent The Stripe Intent that is being processed
     * @param merchantName The customer-facing business name.
     * @param customerName Name of the customer, used to pre-fill the form.
     * @param customerEmail Email of the customer, used to pre-fill the form.
     * @param customerPhone Phone number of the customer, used to pre-fill the form.
     * @param shippingValues The initial shipping values for [FormController].
     */
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Configuration(
        val stripeIntent: StripeIntent,
        val merchantName: String,
        val customerName: String?,
        val customerEmail: String?,
        val customerPhone: String?,
        val customerBillingCountryCode: String?,
        val shippingValues: Map<IdentifierSpec, String?>?
    ) : Parcelable

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        const val LINK_ENABLED = true
        val supportedFundingSources = SupportedPaymentMethod.allTypes
    }
}
