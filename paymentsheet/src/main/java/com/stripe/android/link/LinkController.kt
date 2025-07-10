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

@LinkControllerScope
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LinkController @Inject internal constructor(
    activity: Activity,
    private val linkControllerCoordinator: LinkControllerCoordinator,
    private val viewModel: LinkControllerViewModel,
) {
    val state: StateFlow<State> = viewModel.state(activity)

    fun configure(configuration: Configuration) {
        viewModel.configure(configuration)
    }

    fun presentPaymentMethods(email: String?) {
        viewModel.onPresentPaymentMethods(
            launcher = linkControllerCoordinator.linkActivityResultLauncher,
            email = email
        )
    }

    fun createPaymentMethod() {
        viewModel.onCreatePaymentMethod()
    }

    fun lookupConsumer(email: String) {
        viewModel.onLookupConsumer(email)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun reloadSession() {
        viewModel.reloadSession()
    }

    @Parcelize
    @Poko
    class Configuration internal constructor(
        internal val merchantDisplayName: String,
        internal val cardBrandAcceptance: PaymentSheet.CardBrandAcceptance,
        internal val link: PaymentSheet.LinkConfiguration,
    ) : Parcelable {

        class Builder(private val merchantDisplayName: String) {
            private var cardBrandAcceptance: PaymentSheet.CardBrandAcceptance =
                ConfigurationDefaults.cardBrandAcceptance

            private var link: PaymentSheet.LinkConfiguration =
                ConfigurationDefaults.link

            fun cardBrandAcceptance(cardBrandAcceptance: PaymentSheet.CardBrandAcceptance) = apply {
                this.cardBrandAcceptance = cardBrandAcceptance
            }

            fun link(link: PaymentSheet.LinkConfiguration) = apply {
                this.link = link
            }

            fun build(): Configuration = Configuration(
                merchantDisplayName = merchantDisplayName,
                cardBrandAcceptance = cardBrandAcceptance,
                link = link,
            )
        }

        internal companion object {
            fun default(context: Context): Configuration {
                val appName = context.applicationInfo.loadLabel(context.packageManager).toString()
                return Builder(appName).build()
            }
        }
    }

    @Parcelize
    @Poko
    class State
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(
        val isConsumerVerified: Boolean? = null,
        val selectedPaymentMethodPreview: PaymentMethodPreview? = null,
        val createdPaymentMethod: PaymentMethod? = null,
    ) : Parcelable

    sealed interface PresentPaymentMethodsResult {
        data object Success : PresentPaymentMethodsResult
        data object Canceled : PresentPaymentMethodsResult
        class Failed internal constructor(val error: Throwable) : PresentPaymentMethodsResult
    }

    sealed interface LookupConsumerResult {
        class Success internal constructor(val email: String, val isConsumer: Boolean) : LookupConsumerResult
        class Failed internal constructor(val email: String, val error: Throwable) : LookupConsumerResult
    }

    sealed interface CreatePaymentMethodResult {
        data object Success : CreatePaymentMethodResult
        class Failed internal constructor(val error: Throwable) : CreatePaymentMethodResult
    }

    fun interface PresentPaymentMethodsCallback {
        fun onPresentPaymentMethodsResult(result: PresentPaymentMethodsResult)
    }

    fun interface LookupConsumerCallback {
        fun onLookupConsumerResult(result: LookupConsumerResult)
    }

    fun interface CreatePaymentMethodCallback {
        fun onCreatePaymentMethodResult(result: CreatePaymentMethodResult)
    }

    @Parcelize
    @Poko
    class PaymentMethodPreview(
        @DrawableRes val iconRes: Int,
        val label: String,
        val sublabel: String?
    ) : Parcelable

    companion object {
        @JvmStatic
        fun create(
            activity: ComponentActivity,
            presentPaymentMethodCallback: PresentPaymentMethodsCallback,
            lookupConsumerCallback: LookupConsumerCallback,
            createPaymentMethodCallback: CreatePaymentMethodCallback,
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
                    presentPaymentMethodCallback = presentPaymentMethodCallback,
                    lookupConsumerCallback = lookupConsumerCallback,
                    createPaymentMethodCallback = createPaymentMethodCallback,
                )
                .controller
        }
    }
}
